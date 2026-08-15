// Texture export tool. See port/include/texexport.h for the overview.
//
// IMPORTANT: this file relies on exact knowledge of how texdecompress.c lays
// out decoded pixel data in `struct tex.data` (row padding/stride per format,
// endianness, palette layout). That was derived by reading texAlignIndices(),
// texChannelsToPixels() and texInflateZlib() in src/game/texdecompress.c line
// by line — it was NOT reimplemented/guessed. If a texture comes out visibly
// wrong (skewed rows, wrong colours), that's the place to re-check first.
// A good way to sanity check: compare an exported texture against how it
// actually looks in-game.
//
// texLoadCustomPng() uses the "simple" strategy: raw RGBA8 is stored directly
// in the caller-supplied dst buffer without re-encoding back to the original
// N64 pixel format. The caller (modTextureLoad via mod.c) is responsible for
// using the RGBA8 data correctly.

#include <stdint.h>
#include <string.h>
#include <stdio.h>

#include <ultra64.h>
#include "constants.h"
#include "types.h"
#include "game/texdecompress.h"
#include "system.h"
#include "fs.h"
#include "config.h"
#include "texexport.h"

#define STB_IMAGE_WRITE_IMPLEMENTATION
#include "stb_image_write.h"

// stb_image for PNG load-back (single-header, public domain)
#define STB_IMAGE_IMPLEMENTATION
#define STBI_ONLY_PNG
#define STBI_NO_STDIO
#include "stb_image.h"

#define TEXEXPORT_PARENT   "$H/texturas"
#define TEXEXPORT_DIR      "$H/texturas/dump"
#define TEXLOAD_DIR        "$H/texturas/load"
#define TEXEXPORT_TRIGGER  "$H/export_textures.txt"
#define TEXEXPORT_POOLSIZE (1024 * 1024) // generous; textures are small, this just needs to fit one at a time
#define TEXEXPORT_SCALE    32 // Upscale factor: 32x resolution (16x16 -> 512x512)

// --- Config-backed globals ---------------------------------------------------
// 0 = off, 1 = create trigger file at next boot so the dump runs once.
// Saved/loaded via pd.ini [Textures] section.
s32 g_TexDumpEnabled = 0;

// 0 = off, 1 = attempt loading custom PNGs instead of original textures.
s32 g_TexLoadEnabled = 0;

static inline s32 alignUp(s32 value, s32 align)
{
	return (value + (align - 1)) & ~(align - 1);
}

static inline u8 scale5to8(u32 v5)
{
	return (u8)((v5 << 3) | (v5 >> 2));
}

static inline u8 scale4to8(u32 v4)
{
	return (u8)(v4 * 17);
}

static inline u8 scale3to8(u32 v3)
{
	return (u8)((v3 * 255) / 7);
}

// Bilinear interpolation for smoother upscaling
static u8 bilinearInterpolate(u8 *src, s32 srcWidth, s32 srcHeight, float x, float y, s32 channel)
{
	const s32 x0 = (s32)x;
	const s32 y0 = (s32)y;
	const s32 x1 = (x0 + 1 < srcWidth) ? x0 + 1 : x0;
	const s32 y1 = (y0 + 1 < srcHeight) ? y0 + 1 : y0;

	const float fx = x - x0;
	const float fy = y - y0;

	const u8 c00 = src[(y0 * srcWidth + x0) * 4 + channel];
	const u8 c10 = src[(y0 * srcWidth + x1) * 4 + channel];
	const u8 c01 = src[(y1 * srcWidth + x0) * 4 + channel];
	const u8 c11 = src[(y1 * srcWidth + x1) * 4 + channel];

	const float c0 = c00 * (1 - fx) + c10 * fx;
	const float c1 = c01 * (1 - fx) + c11 * fx;

	return (u8)(c0 * (1 - fy) + c1 * fy);
}

// Reads a 5551 colour (as stored explicitly big-endian by texInflateZlib/
// PD_BE16) and writes RGBA8 into out[0..3].
static void decode5551(u16 raw, u8 *out)
{
	out[0] = scale5to8((raw >> 11) & 0x1f);
	out[1] = scale5to8((raw >> 6) & 0x1f);
	out[2] = scale5to8((raw >> 1) & 0x1f);
	out[3] = (raw & 1) ? 255 : 0;
}

// Converts one decoded `struct tex` into a freshly allocated RGBA8 buffer.
// Returns NULL on an unrecognised/unsupported combination.
static u8 *texToRgba8(struct tex *t)
{
	const s32 w = t->width;
	const s32 h = t->height;

	if (w <= 0 || h <= 0 || !t->data) {
		return NULL;
	}

	u8 *out = sysMemAlloc(w * h * 4);
	if (!out) {
		return NULL;
	}

	// --- Paletted formats (CI4 / CI8) ---------------------------------
	// Only ever produced by the zlib path (texInflateZlib). Index buffer
	// is written by texAlignIndices, palette is appended right after all
	// LOD index data, as explicit big-endian u16 entries (5551 or 8/8
	// intensity+alpha depending on lutmodeindex).
	if (t->gbiformat == G_IM_FMT_CI) {
		const s32 numColours = t->unk0a + 1;
		const s32 indexStride = (t->depth == G_IM_SIZ_4b)
			? alignUp(alignUp(w, 2) / 2, 8)
			: alignUp(w, 8);

		// Palette sits immediately after the (single, LOD0-only export
		// case) index block. We only ever load LOD0 here since we pass
		// hasloddata=true via texLoadFromTextureNum(), so LOD0 alone is
		// what's swizzled/aligned first; palette follows every LOD's
		// index data, so for a definitive palette offset we search the
		// end of the buffer instead: numColours * 2 bytes from the end
		// is unreliable in this simplified single-shot export, so we
		// keep it explicit: paletteOffset = indexStride * h (LOD0 only,
		// since we don't chase further LODs here).
		const u8 *indexData = t->data;
		const u8 *palette = t->data + indexStride * h;

		for (s32 y = 0; y < h; y++) {
			for (s32 x = 0; x < w; x++) {
				u8 index;
				if (t->depth == G_IM_SIZ_4b) {
					u8 packed = indexData[y * indexStride + (x >> 1)];
					index = (x & 1) ? (packed & 0xf) : (packed >> 4);
				} else {
					index = indexData[y * indexStride + x];
				}

				if (index >= numColours) {
					index = 0;
				}

				const u16 raw = (palette[index * 2] << 8) | palette[index * 2 + 1];
				u8 *px = &out[(y * w + x) * 4];

				if (t->lutmodeindex == (G_TT_IA16 >> G_MDSFT_TEXTLUT)) {
					// 8-bit intensity + 8-bit alpha palette entry
					px[0] = px[1] = px[2] = raw >> 8;
					px[3] = raw & 0xff;
				} else {
					// 5551 palette entry
					decode5551(raw, px);
				}
			}
		}

		return out;
	}

	// --- RGBA32 / RGB24 (both stored as native-endian packed u32) ----
	if (t->gbiformat == G_IM_FMT_RGBA && t->depth == G_IM_SIZ_32b) {
		const s32 stride = alignUp(w, 4);
		const u32 *src = (const u32 *)t->data;

		for (s32 y = 0; y < h; y++) {
			for (s32 x = 0; x < w; x++) {
				const u32 v = src[y * stride + x];
				u8 *px = &out[(y * w + x) * 4];
				px[0] = (v >> 24) & 0xff;
				px[1] = (v >> 16) & 0xff;
				px[2] = (v >> 8) & 0xff;
				px[3] = v & 0xff;
			}
		}

		return out;
	}

	// --- RGBA16 / RGB15 (5551, explicit big-endian via PD_BE16) ------
	if (t->gbiformat == G_IM_FMT_RGBA && t->depth == G_IM_SIZ_16b) {
		const s32 stride = alignUp(w, 4);
		const u8 *src = t->data;

		for (s32 y = 0; y < h; y++) {
			for (s32 x = 0; x < w; x++) {
				const u8 *p = &src[(y * stride + x) * 2];
				const u16 raw = (p[0] << 8) | p[1];
				decode5551(raw, &out[(y * w + x) * 4]);
			}
		}

		return out;
	}

	// --- IA16 (native-endian u16: high byte = intensity, low = alpha) -
	if (t->gbiformat == G_IM_FMT_IA && t->depth == G_IM_SIZ_16b) {
		const s32 stride = alignUp(w, 4);
		const u16 *src = (const u16 *)t->data;

		for (s32 y = 0; y < h; y++) {
			for (s32 x = 0; x < w; x++) {
				const u16 v = src[y * stride + x];
				u8 *px = &out[(y * w + x) * 4];
				px[0] = px[1] = px[2] = (v >> 8) & 0xff;
				px[3] = v & 0xff;
			}
		}

		return out;
	}

	// --- IA8 (4-bit intensity + 4-bit alpha per byte) -----------------
	if (t->gbiformat == G_IM_FMT_IA && t->depth == G_IM_SIZ_8b) {
		const s32 stride = alignUp(w, 8);
		const u8 *src = t->data;

		for (s32 y = 0; y < h; y++) {
			for (s32 x = 0; x < w; x++) {
				const u8 v = src[y * stride + x];
				u8 *px = &out[(y * w + x) * 4];
				px[0] = px[1] = px[2] = scale4to8(v >> 4);
				px[3] = scale4to8(v & 0xf);
			}
		}

		return out;
	}

	// --- I8 (plain 8-bit grayscale, opaque) ---------------------------
	if (t->gbiformat == G_IM_FMT_I && t->depth == G_IM_SIZ_8b) {
		const s32 stride = alignUp(w, 8);
		const u8 *src = t->data;

		for (s32 y = 0; y < h; y++) {
			for (s32 x = 0; x < w; x++) {
				const u8 v = src[y * stride + x];
				u8 *px = &out[(y * w + x) * 4];
				px[0] = px[1] = px[2] = v;
				px[3] = 255;
			}
		}

		return out;
	}

	// --- IA4 (3-bit intensity + 1-bit alpha, 2 pixels per byte) -------
	if (t->gbiformat == G_IM_FMT_IA && t->depth == G_IM_SIZ_4b) {
		const s32 stride = alignUp(w, 16) / 2;
		const u8 *src = t->data;

		for (s32 y = 0; y < h; y++) {
			for (s32 x = 0; x < w; x++) {
				const u8 packed = src[y * stride + (x >> 1)];
				const u8 nibble = (x & 1) ? (packed & 0xf) : (packed >> 4);
				u8 *px = &out[(y * w + x) * 4];
				px[0] = px[1] = px[2] = scale3to8((nibble >> 1) & 0x7);
				px[3] = (nibble & 1) ? 255 : 0;
			}
		}

		return out;
	}

	// --- I4 (4-bit grayscale, 2 pixels per byte, opaque) --------------
	if (t->gbiformat == G_IM_FMT_I && t->depth == G_IM_SIZ_4b) {
		const s32 stride = alignUp(w, 16) / 2;
		const u8 *src = t->data;

		for (s32 y = 0; y < h; y++) {
			for (s32 x = 0; x < w; x++) {
				const u8 packed = src[y * stride + (x >> 1)];
				const u8 nibble = (x & 1) ? (packed & 0xf) : (packed >> 4);
				u8 *px = &out[(y * w + x) * 4];
				px[0] = px[1] = px[2] = scale4to8(nibble);
				px[3] = 255;
			}
		}

		return out;
	}

	sysMemFree(out);
	return NULL;
}

int texExportSingle(u16 texturenum)
{
	static u8 poolBuffer[TEXEXPORT_POOLSIZE];
	struct texpool pool;

	texInitPool(&pool, poolBuffer, sizeof(poolBuffer));
	texLoadFromTextureNum(texturenum, &pool);

	struct tex *t = texFindInPool(texturenum, &pool);
	if (!t || !t->data) {
		return 0; // texture has no data (common — texture indices aren't all populated)
	}

	u8 *rgba = texToRgba8(t);
	if (!rgba) {
		sysLogPrintf(LOG_WARNING, "texexport: unsupported format for texture %04x (gbiformat=%d depth=%d)",
			texturenum, t->gbiformat, t->depth);
		return 0;
	}

	// Upscale texture by TEXEXPORT_SCALE factor using bilinear interpolation
	const s32 scaledWidth = t->width * TEXEXPORT_SCALE;
	const s32 scaledHeight = t->height * TEXEXPORT_SCALE;
	u8 *scaledRgba = sysMemZeroAlloc(scaledWidth * scaledHeight * 4);
	if (!scaledRgba) {
		sysLogPrintf(LOG_WARNING, "texexport: could not allocate scaled buffer for texture %04x", texturenum);
		sysMemFree(rgba);
		return 0;
	}

	// Bilinear upscaling for smoother results
	const float scaleX = (float)(t->width - 1) / (scaledWidth - 1);
	const float scaleY = (float)(t->height - 1) / (scaledHeight - 1);

	for (s32 y = 0; y < scaledHeight; y++) {
		for (s32 x = 0; x < scaledWidth; x++) {
			const float srcX = x * scaleX;
			const float srcY = y * scaleY;
			const s32 dstOffset = (y * scaledWidth + x) * 4;
			scaledRgba[dstOffset + 0] = bilinearInterpolate(rgba, t->width, t->height, srcX, srcY, 0); // R
			scaledRgba[dstOffset + 1] = bilinearInterpolate(rgba, t->width, t->height, srcX, srcY, 1); // G
			scaledRgba[dstOffset + 2] = bilinearInterpolate(rgba, t->width, t->height, srcX, srcY, 2); // B
			scaledRgba[dstOffset + 3] = bilinearInterpolate(rgba, t->width, t->height, srcX, srcY, 3); // A
		}
	}

	char relPath[FS_MAXPATH + 1];
	snprintf(relPath, sizeof(relPath), TEXEXPORT_DIR "/%04x.png", texturenum);

	const int ok = stbi_write_png(fsFullPath(relPath), scaledWidth, scaledHeight, 4, scaledRgba, scaledWidth * 4);

	sysMemFree(scaledRgba);
	sysMemFree(rgba);

	return ok;
}

void texExportCheckAndRun(void)
{
	// If the UI flag was set, ensure the trigger file exists so the dump runs.
	if (g_TexDumpEnabled) {
		// Create the trigger file if it doesn't already exist.
		if (fsFileSize(TEXEXPORT_TRIGGER) < 0) {
			FILE *f = fsFileOpenWrite(TEXEXPORT_TRIGGER);
			if (f) {
				fsFileFree(f);
			}
		}
		// Clear the flag so we don't re-create it on every subsequent boot
		// (the trigger file itself controls the one-shot run).
		g_TexDumpEnabled = 0;
	}

	const int triggered = sysArgCheck("--export-textures") || (fsFileSize(TEXEXPORT_TRIGGER) >= 0);
	if (!triggered) {
		return;
	}

	sysLogPrintf(LOG_NOTE, "texexport: starting full texture export to " TEXEXPORT_DIR);
	fsCreateDir(TEXEXPORT_PARENT); // perfect dark/texturas/
	fsCreateDir(TEXEXPORT_DIR);    // perfect dark/texturas/dump/
	fsCreateDir(TEXLOAD_DIR);      // perfect dark/texturas/load/ (created once so user knows where to put files)

	s32 exported = 0;
	for (s32 i = 0; i < NUM_TEXTURES; i++) {
		if (texExportSingle((u16)i)) {
			exported++;
		}
	}

	sysLogPrintf(LOG_NOTE, "texexport: done, exported %d/%d textures", exported, NUM_TEXTURES);

	// Remove the trigger file so this doesn't re-run (and slow down boot) every launch.
	remove(fsFullPath(TEXEXPORT_TRIGGER));
}

// ---------------------------------------------------------------------------
// texLoadCustomPng — load-back (simple RGBA8 path)
// ---------------------------------------------------------------------------
// Reads $H/texturas/load/XXXX.png, decodes it to RGBA8 via stb_image, then
// copies the pixel data raw into dst. No re-encoding to the original N64
// pixel format is performed — the caller uses it as-is (raw RGBA8, 4 bytes
// per pixel, row-major, no row padding).
//
// Returns: bytes written (w*h*4, clamped to dstSize), or -1 on failure.
s32 texLoadCustomPng(u16 texturenum, void *dst, u32 dstSize)
{
	if (!g_TexLoadEnabled || !dst || dstSize == 0) {
		return -1;
	}

	char relPath[FS_MAXPATH + 1];
	snprintf(relPath, sizeof(relPath), TEXLOAD_DIR "/%04x.png", texturenum);

	// Load file into a temporary buffer
	u32 fileSize = 0;
	void *fileData = fsFileLoad(relPath, &fileSize);
	if (!fileData || fileSize == 0) {
		if (fileData) {
			sysMemFree(fileData);
		}
		return -1;
	}

	// Decode PNG
	int w, h, channels;
	stbi_uc *pixels = stbi_load_from_memory(
		(const stbi_uc *)fileData, (int)fileSize,
		&w, &h, &channels, 4 // force RGBA8
	);
	sysMemFree(fileData);

	if (!pixels) {
		sysLogPrintf(LOG_WARNING, "texexport: stb_image failed to decode %s: %s",
			relPath, stbi_failure_reason());
		return -1;
	}

	const u32 needed = (u32)(w * h * 4);
	const u32 toCopy = (needed < dstSize) ? needed : dstSize;
	memcpy(dst, pixels, toCopy);
	stbi_image_free(pixels);

	sysLogPrintf(LOG_NOTE, "texexport: loaded custom PNG %04x (%dx%d)", texturenum, w, h);
	return (s32)toCopy;
}

// ---------------------------------------------------------------------------
// Config registration (runs before configInit via constructor priority)
// ---------------------------------------------------------------------------
PD_CONSTRUCTOR static void texExportConfigInit(void)
{
	configRegisterInt("Textures.DumpEnabled", &g_TexDumpEnabled, 0, 1);
	configRegisterInt("Textures.LoadCustomEnabled", &g_TexLoadEnabled, 0, 1);
}
