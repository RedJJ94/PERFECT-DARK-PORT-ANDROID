#ifndef _IN_TEXEXPORT_H
#define _IN_TEXEXPORT_H

#include <PR/ultratypes.h>

/*
 * Texture export / dump tool.
 *
 * Reuses the game's own runtime texture decoder (texdecompress.c) to force-load
 * every texture in NUM_TEXTURES and write each one out as a PNG, using the
 * already-decoded pixel buffer that the game itself uses for rendering.
 *
 * This does NOT reimplement any of the PD texture compression (zlib / huffman /
 * rle / lookup / blur) — it calls the real texLoadFromTextureNum()/texInitPool()
 * API from src/game/texdecompress.c, so decoding correctness is inherited from
 * the game's own code. This file is only responsible for turning the resulting
 * `struct tex` (raw N64-native pixel buffer) into RGBA8 PNG bytes.
 *
 * Texture load-back:
 * texLoadCustomPng() reads $H/texture_export/XXXX.png (if present) and stores
 * the decoded RGBA8 data directly into dst. Because no re-encoding to the
 * original N64 pixel format is performed, the caller is responsible for
 * treating the buffer as raw RGBA8 (4 bytes per pixel, row-major, no padding).
 * dst must be large enough: width * height * 4 bytes.
 */

// --- Config flags (registered in the config system by texexport.c) ----------
// Set to 1 to create the dump trigger file at next boot.
// Cleared automatically after the dump completes.
extern s32 g_TexDumpEnabled;

// Set to 1 to attempt loading custom PNGs instead of original textures.
extern s32 g_TexLoadEnabled;

// --- Functions ---------------------------------------------------------------

// Checks for a trigger (see texexport.c for how) and, if present, exports every
// texture to $H/texture_export/XXXX.png, then clears the trigger. Safe to call
// unconditionally at boot: it's a no-op when nothing requested an export.
// Call this once, after texInit() has populated g_Textures (see integration
// note in texexport.c).
void texExportCheckAndRun(void);

// Exports a single texture by number. Returns true on success.
// Exposed separately in case you want to hook this into a debug menu / cheat
// instead of (or as well as) the batch trigger above.
int texExportSingle(unsigned short texturenum);

// Loads a custom PNG from $H/texture_export/XXXX.png and writes raw RGBA8
// (4 bytes/pixel, no row padding) into dst. Returns the number of bytes
// written (width*height*4), or -1 if the file doesn't exist or load fails.
// g_TexLoadEnabled must be 1 for this to attempt anything.
// dst must be able to hold at least dstSize bytes; if width*height*4 >
// dstSize the copy is truncated and the return value is the truncated size.
s32 texLoadCustomPng(unsigned short texturenum, void *dst, unsigned int dstSize);

#endif
