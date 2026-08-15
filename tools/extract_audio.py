#!/usr/bin/env python3
import os
import sys
import zlib

def extract_all_audio():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    rom_path = os.path.join(base_dir, 'Perfect Dark (USA).z64')

    if not os.path.exists(rom_path):
        # Check alternative names
        for alt in ['pd.ntsc-final.z64', 'pd.z64', 'PerfectDark.z64']:
            p = os.path.join(base_dir, alt)
            if os.path.exists(p):
                rom_path = p
                break

    if not os.path.exists(rom_path):
        print(f"Erro: ROM nao encontrada em '{rom_path}'")
        return False

    print(f"Lendo ROM: {rom_path}...")
    with open(rom_path, 'rb') as f:
        rom = f.read()

    # Decompress 1173 data segment
    def decompress_1173(data):
        w0 = int.from_bytes(data[0:2], 'big')
        if w0 != 0x1173:
            return data
        uncomp_len = int.from_bytes(data[2:5], 'big')
        d = zlib.decompressobj(-15)
        return d.decompress(data[5:], uncomp_len)

    data_offset = 0x39850
    data_seg = decompress_1173(rom[data_offset:])

    files_offset = 0x28080
    offsets = []
    i = files_offset
    while True:
        offset = int.from_bytes(data_seg[i:i+4], 'big')
        if offset == 0 and len(offsets):
            break
        offsets.append(offset)
        i += 4

    tableaddr = offsets[-1]
    names = []
    i = tableaddr
    while True:
        offset = int.from_bytes(rom[i:i+4], 'big')
        if offset == 0 and len(names):
            break
        nullpos = rom[tableaddr + offset:].index(0)
        name = str(rom[tableaddr + offset:tableaddr + offset + nullpos], 'utf-8')
        names.append(name)
        i += 4

    audio_out_dir = os.path.join(base_dir, 'audio')
    os.makedirs(audio_out_dir, exist_ok=True)

    extracted_count = 0
    list_entries = []

    print(f"Extraindo arquivos de audio para '{audio_out_dir}'...")

    for idx, offset in enumerate(offsets):
        if idx == 0 or idx >= len(offsets) - 1:
            continue
        endoffset = offsets[idx + 1]
        name = names[idx]
        if name.startswith('A'):
            content = rom[offset:endoffset]
            
            # Save primary MP3 named after the game internal name
            out_filename = f"{name}.mp3"
            out_path = os.path.join(audio_out_dir, out_filename)
            with open(out_path, 'wb') as out_f:
                out_f.write(content)
            
            extracted_count += 1
            entry = f"ID_DEC: {idx:04d} | ID_HEX: 0x{idx:04x} | Nome: {name:<12} | Arquivo: {out_filename:<16} | Tamanho: {len(content)} bytes"
            list_entries.append(entry)

    # Save audio list
    list_file_path = os.path.join(audio_out_dir, 'audio_list.txt')
    with open(list_file_path, 'w', encoding='utf-8') as lf:
        lf.write(f"=== Lista de Audios do Perfect Dark (Total: {extracted_count}) ===\n")
        lf.write("Formato para substituir/dublar:\n")
        lf.write("  - Nome do arquivo: audio/<NomeDoArquivo>.mp3 (ex: Arecep01M.mp3)\n")
        lf.write("  - Ou por ID decimal: audio/<ID_DEC>.mp3 (ex: 0625.mp3)\n")
        lf.write("  - Ou por ID hexadecimal: audio/<ID_HEX>.mp3 (ex: 0271.mp3)\n\n")
        lf.write("\n".join(list_entries))
        lf.write("\n")

    print(f"Sucesso! {extracted_count} audios extraidos em '{audio_out_dir}'.")
    print(f"Arquivo de lista gerado em: '{list_file_path}'")
    return True

if __name__ == '__main__':
    extract_all_audio()
