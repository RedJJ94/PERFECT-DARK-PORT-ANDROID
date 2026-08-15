#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script para extrair e organizar todos os áudios do Perfect Dark por cenas do jogo.
Organiza em pastas estruturadas: Cutscenes, Briefings, Diálogos de Fases e Treinamentos.
Gera arquivos de informação e transcrição para cada cena.
"""

import os
import sys
import re
import shutil
import json

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
AUDIO_SRC_DIR = os.path.join(BASE_DIR, 'audio')
OUTPUT_BASE_DIR = os.path.join(BASE_DIR, 'audios_por_cenas')

# Mapeamento de descrições e nomes das Cutscenes
CUTSCENE_INFO = {
    1: {
        "nome": "Cutscene_01_Intro_dataDyne_Defection",
        "titulo": "Cutscene 01: dataDyne Central - Defection (Introdução / Chegada de Joanna)",
        "descricao": "Joanna Dark chega de helicóptero no topo da torre da dataDyne Central à noite em Chicago."
    },
    2: {
        "nome": "Cutscene_02_dataDyne_Defection_Outro",
        "titulo": "Cutscene 02: dataDyne Central - Defection (Conclusão / Contato com Carrington)",
        "descricao": "Joanna desce pelo elevador e entra em contato com Daniel Carrington para a extração do Dr. Caroll."
    },
    3: {
        "nome": "Cutscene_03_dataDyne_Extraction_Outro",
        "titulo": "Cutscene 03: dataDyne Central - Extraction (Fuga no Heliponto)",
        "descricao": "Joanna escapa do prédio da dataDyne Central pelo heliponto sob fogo de guardas."
    },
    4: {
        "nome": "Cutscene_04_dataDyne_Investigation_Intro",
        "titulo": "Cutscene 04: dataDyne Central - Investigation (Reunião Secreta: Cassandra e Mr. Blonde)",
        "descricao": "Cassandra De Vries se reúne em segredo com o misterioso Mr. Blonde na sala de reuniões da dataDyne."
    },
    5: {
        "nome": "Cutscene_05_dataDyne_Investigation_Outro",
        "titulo": "Cutscene 05: dataDyne Central - Investigation (Gravação do CamSpy e Fuga)",
        "descricao": "Joanna grava a reunião conspiratória com o CamSpy e envia os dados para o Instituto Carrington."
    },
    6: {
        "nome": "Cutscene_06_Carrington_Villa_Intro",
        "titulo": "Cutscene 06: Carrington Villa - Hostage One (Introdução / Invasão da Vila)",
        "descricao": "Daniel Carrington é encurralado por forças terroristas da dataDyne na sua vila particular."
    },
    7: {
        "nome": "Cutscene_07_Carrington_Villa_Outro",
        "titulo": "Cutscene 07: Carrington Villa - Hostage One (Fuga de Lancha com Carrington)",
        "descricao": "Joanna resgata Carrington e os dois fogem de lancha pelo lago da vila."
    },
    8: {
        "nome": "Cutscene_08_Chicago_Stealth_Intro",
        "titulo": "Cutscene 08: Chicago - Stealth (Encontro Noturno: Trent, Cassandra e Mr. Blonde)",
        "descricao": "Na chuva das ruas de Chicago, Trent Easton, Cassandra De Vries e Mr. Blonde conspiram sobre o Pelagic II."
    },
    9: {
        "nome": "Cutscene_09_Chicago_Stealth_Outro",
        "titulo": "Cutscene 09: Chicago - Stealth (Entrada no Edifício G5)",
        "descricao": "Joanna cria uma distração com o táxi voador e consegue entrar no edifício da corporação G5."
    },
    10: {
        "nome": "Cutscene_10_G5_Building_Outro",
        "titulo": "Cutscene 10: G5 Building - Reconnaissance (Resgate do Dr. Caroll no Cofre)",
        "descricao": "Joanna e a IA Dr. Caroll invadem o cofre da G5, baixam os dados da conspiração e planejam a fuga."
    },
    11: {
        "nome": "Cutscene_11_Area_51_Infiltration_Intro",
        "titulo": "Cutscene 11: Area 51 - Infiltration (Comunicação com o Agente Jonathan)",
        "descricao": "Joanna entra em contato com o agente infiltrado Jonathan perto do perímetro da Área 51."
    },
    12: {
        "nome": "Cutscene_12_Area_51_Infiltration_Outro",
        "titulo": "Cutscene 12: Area 51 - Infiltration (Jonathan explode a entrada do Hangar)",
        "descricao": "Jonathan pilota o hovercraft, quebra os portões da base militar e explode a entrada do complexo."
    },
    13: {
        "nome": "Cutscene_13_Area_51_Rescue_Intro",
        "titulo": "Cutscene 13: Area 51 - Rescue (Laboratório de Autópsia Alienígena / Contato com Elvis)",
        "descricao": "Joanna encontra a sala de autópsia secreta onde o alienígena Maian 'Elvis' está preso."
    },
    14: {
        "nome": "Cutscene_14_Area_51_Rescue_Outro",
        "titulo": "Cutscene 14: Area 51 - Rescue (Resgate de Elvis na Maca Antigravidade)",
        "descricao": "Joanna e Elvis escapam do laboratório subterrâneo utilizando a maca antigravitacional."
    },
    15: {
        "nome": "Cutscene_15_Area_51_Escape_Outro",
        "titulo": "Cutscene 15: Area 51 - Escape (Fuga no Disco Voador Maian)",
        "descricao": "Joanna, Elvis e Jonathan acionam o disco voador alienígena no hangar e decolam escapando da Área 51."
    },
    16: {
        "nome": "Cutscene_16_Air_Base_Espionage_Intro",
        "titulo": "Cutscene 16: Air Base - Espionage (Briefing de Joanna e Carrington na Base Aérea)",
        "descricao": "Carrington instrui Joanna sobre a conspiração do governo e a necessidade de infiltrar o Air Force One."
    },
    17: {
        "nome": "Cutscene_17_Air_Base_Espionage_Outro",
        "titulo": "Cutscene 17: Air Base - Espionage (Trent e o Presidente a bordo do Air Force One)",
        "descricao": "Trent Easton confronta o Presidente dos EUA a bordo do avião presidencial prestes a decolar."
    },
    18: {
        "nome": "Cutscene_18_Air_Force_One_Intro",
        "titulo": "Cutscene 18: Air Force One - Antiterrorism (Elvis lança Joanna no Teto do Avião)",
        "descricao": "Elvis pilota o disco voador em alta altitude e posiciona Joanna sobre o teto do Air Force One em voo."
    },
    19: {
        "nome": "Cutscene_19_Air_Force_One_Outro",
        "titulo": "Cutscene 19: Air Force One - Antiterrorism (Fuga na Cápsula de Escape Presidencial)",
        "descricao": "Com o Air Force One caindo, Joanna coloca o Presidente na cápsula de escape e ambos ejetam na neve."
    },
    20: {
        "nome": "Cutscene_20_Crash_Site_Intro",
        "titulo": "Cutscene 20: Crash Site - Confrontation (Confronto nos Destroços do Avião)",
        "descricao": "Trent Easton e Mr. Blonde chegam aos destroços na neve e encontram o Presidente e Joanna protegidos."
    },
    21: {
        "nome": "Cutscene_21_Crash_Site_Outro",
        "titulo": "Cutscene 21: Crash Site - Confrontation (Resgate de Elvis e do Presidente)",
        "descricao": "Elvis chega com suporte aéreo e resgata Joanna e o Presidente da emboscada alienígena Skedar."
    },
    22: {
        "nome": "Cutscene_22_Pelagic_II_Intro",
        "titulo": "Cutscene 22: Pelagic II - Exploration (Partida no Submarino de Exploração)",
        "descricao": "Elvis e Joanna navegam no submarino em direção ao navio de pesquisa governamental Pelagic II."
    },
    23: {
        "nome": "Cutscene_23_Pelagic_II_Outro",
        "titulo": "Cutscene 23: Pelagic II - Exploration (Desativação e Sacrifício do Dr. Caroll)",
        "descricao": "Dr. Caroll acessa o computador central Skedar, desativa os sistemas e se despede de Joanna emocionado."
    },
    24: {
        "nome": "Cutscene_24_Deep_Sea_Intro",
        "titulo": "Cutscene 24: Deep Sea - Nullify Threat (Preparação para Nave Cetan)",
        "descricao": "Joanna, Elvis e Carrington preparam a infiltração final na colossal nave alienígena Cetan no fundo do oceano."
    },
    25: {
        "nome": "Cutscene_25_Deep_Sea_Outro",
        "titulo": "Cutscene 25: Deep Sea - Nullify Threat (Destruição da Mega-Arma Cetan)",
        "descricao": "A mega-arma Skedar é destruída sob as águas profundas, frustrando a invasão alienígena."
    },
    26: {
        "nome": "Cutscene_26_Carrington_Institute_Defense_Intro",
        "titulo": "Cutscene 26: Carrington Institute - Defense (Ataque e Invasão ao Instituto)",
        "descricao": "Tropas da dataDyne e forças Skedar invadem as instalações do Instituto Carrington fazendo reféns."
    },
    27: {
        "nome": "Cutscene_27_Carrington_Institute_Defense_Outro",
        "titulo": "Cutscene 27: Carrington Institute - Defense (Partida para a Nave Mãe Skedar)",
        "descricao": "Após libertar o Instituto, Joanna e Elvis armam-se para invadir o Cruzador Skedar no espaço."
    },
    28: {
        "nome": "Cutscene_28_Attack_Ship_Outro",
        "titulo": "Cutscene 28: Attack Ship - Covert Assault (Destruição do Cruzador Skedar)",
        "descricao": "Joanna sabota o reator principal do Cruzador de Guerra Skedar antes de fugir para o planeta dos Skedar."
    },
    29: {
        "nome": "Cutscene_29_Skedar_Ruins_Ending",
        "titulo": "Cutscene 29: Skedar Ruins - Battle Shrine (Derrota do Rei Skedar e Final do Jogo)",
        "descricao": "Joanna derrota o Grande Rei Skedar no templo alienígena. Elvis e a frota Maian chegam triunfantes."
    }
}

MISSION_INFO = {
    1: {
        "nome": "Missao_01_dataDyne_Central_Defection_Investigation_Extraction",
        "titulo": "Missão 01: dataDyne Central (Defection / Investigation / Extraction)",
        "descricao": "Instruções e briefings de Daniel Carrington para as 3 fases de infiltração na sede da dataDyne."
    },
    2: {
        "nome": "Missao_02_Carrington_Villa_Hostage_One",
        "titulo": "Missão 02: Carrington Villa (Hostage One)",
        "descricao": "Briefings e instruções para resgatar Daniel Carrington e proteger os servidores na vila."
    },
    3: {
        "nome": "Missao_03_Chicago_Stealth_G5_Building",
        "titulo": "Missão 03: Chicago (Stealth & G5 Building Reconnaissance)",
        "descricao": "Briefings para a operação nas ruas chuvosas de Chicago e infiltração no Edifício da corporação G5."
    },
    4: {
        "nome": "Missao_04_Area_51_Infiltration_Rescue_Escape",
        "titulo": "Missão 04: Area 51 (Infiltration / Rescue / Escape)",
        "descricao": "Briefings para as 3 fases de invasão e fuga da base ultra-secreta militar Área 51."
    },
    5: {
        "nome": "Missao_05_Air_Base_Air_Force_One_Crash_Site",
        "titulo": "Missão 05: Air Base / Air Force One / Crash Site",
        "descricao": "Briefings para as operações na base aérea, resgate a bordo do avião presidencial e busca nos destroços."
    },
    6: {
        "nome": "Missao_06_Pelagic_II_Deep_Sea",
        "titulo": "Missão 06: Pelagic II / Deep Sea (Nullify Threat)",
        "descricao": "Briefings para o ataque submarino ao navio de pesquisa Pelagic II e destruição da arma Skedar."
    },
    7: {
        "nome": "Missao_07_Carrington_Institute_Defense",
        "titulo": "Missão 07: Carrington Institute (Defense / Retaking)",
        "descricao": "Briefing de emergência para repelir os invasores e libertar o Instituto Carrington."
    },
    8: {
        "nome": "Missao_08_Attack_Ship_Covert_Assault",
        "titulo": "Missão 08: Attack Ship (Covert Assault)",
        "descricao": "Briefing para a missão espacial a bordo do Cruzador de Batalha alienígena Skedar."
    },
    9: {
        "nome": "Missao_09_Skedar_Ruins_Battle_Shrine",
        "titulo": "Missão 09: Skedar Ruins (Battle Shrine - Final)",
        "descricao": "Briefing para o confronto final no planeta santuário e ruínas sagradas dos Skedar."
    }
}

# Carregar transcrições dos arquivos de setup e lang se disponíveis
def load_transcriptions():
    transcriptions = {}
    setups_dir = os.path.join(BASE_DIR, 'src', 'setups')
    if os.path.exists(setups_dir):
        for fname in os.listdir(setups_dir):
            if fname.endswith('.c'):
                fpath = os.path.join(setups_dir, fname)
                try:
                    with open(fpath, 'r', encoding='utf-8', errors='ignore') as f:
                        for line in f:
                            # Match speak or play_sound with comment
                            m = re.search(r'(MP3_[0-9A-Fa-f]{4}|FILE_A[A-Za-z0-9_]+).*?//\s*"(.*?)"', line)
                            if m:
                                key, text = m.group(1), m.group(2)
                                transcriptions[key.upper()] = text
                except Exception:
                    pass
    return transcriptions

def organize_audio():
    print("Iniciando organização dos áudios por cenas...")
    transcriptions = load_transcriptions()
    
    if not os.path.exists(AUDIO_SRC_DIR):
        print(f"Erro: Diretório de áudios '{AUDIO_SRC_DIR}' não encontrado.")
        return False
        
    all_files = sorted([f for f in os.listdir(AUDIO_SRC_DIR) if f.endswith('.mp3')])
    if not all_files:
        print("Nenhum arquivo MP3 encontrado em 'audio/'.")
        return False

    os.makedirs(OUTPUT_BASE_DIR, exist_ok=True)
    
    # Dicionário de categorização: categoria_path -> list of (file, info_extra)
    categories = {}
    
    for f in all_files:
        name = f[:-4]
        cat_rel = None
        desc_item = ""
        
        # 1. Cutscenes
        if name.startswith('Ap'):
            m = re.match(r'Ap(\d+)_', name)
            if m:
                cnum = int(m.group(1))
                cinfo = CUTSCENE_INFO.get(cnum, {"nome": f"Cutscene_{cnum:02d}", "titulo": f"Cutscene {cnum:02d}", "descricao": ""})
                cat_rel = os.path.join("01_Cutscenes", f"{cnum:02d}_{cinfo['nome']}")
                # Identificar personagem pelo sufixo
                # jo = Joanna, ca = Cassandra/Carrington, el = Elvis, jn/jo = Jonathan, pr = President, tr = Trent, bl = Blonde, dr = Dr. Caroll, su = Saucer/Misc, dv = De Vries, gd = Guard
                char_map = {
                    'jo': 'Joanna Dark',
                    'ca': 'Daniel Carrington / Cassandra De Vries',
                    'el': 'Elvis (Maian)',
                    'jn': 'Jonathan (Agente CI)',
                    'pr': 'Presidente dos EUA',
                    'tr': 'Trent Easton',
                    'bl': 'Mr. Blonde',
                    'dr': 'Dr. Caroll (IA)',
                    'su': 'Piloto / Técnico Maian',
                    'dv': 'Cassandra De Vries',
                    'gd': 'Guarda de Segurança'
                }
                char_suffix = name.split('_')[-1].replace('M', '').lower()
                char_name = char_map.get(char_suffix, "Personagem da Cena")
                desc_item = f"Voz: {char_name}"
            elif name.startswith('Apelelv') or name.startswith('Apelgrd'):
                cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_13_Pelagic_II")
                desc_item = "Diálogos do submarino/elevador Pelagic II"
        
        # 2. Briefings de Missão
        elif name.startswith('Am'):
            m = re.match(r'Am(\d+)_', name)
            if m:
                mnum = int(m.group(1))
                minfo = MISSION_INFO.get(mnum, {"nome": f"Missao_{mnum:02d}", "titulo": f"Missão {mnum:02d}", "descricao": ""})
                cat_rel = os.path.join("02_Briefings_de_Missao", f"{mnum:02d}_{minfo['nome']}")
                desc_item = "Briefing de Daniel Carrington (Instruções da Missão)"
            elif name == 'Am3l2carrM':
                cat_rel = os.path.join("02_Briefings_de_Missao", "03_Missao_03_Chicago_Stealth_G5_Building")
                desc_item = "Briefing de Daniel Carrington (G5 Building)"

        # 3. Fases In-Game
        elif name.startswith('Aa51'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_08_Area_51_Rescue")
            desc_item = "Diálogos in-game na Área 51 (Elvis, Jonathan, Guardas e Cientistas)"
        elif name.startswith('Aaf1'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_11_Air_Force_One")
            desc_item = "Diálogos a bordo do Air Force One (Presidente, Trent, Joanna)"
        elif name.startswith('Aairb') or name.startswith('Aairstw') or name.startswith('Abse') or name.startswith('Ahelic'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_10_Air_Base")
            desc_item = "Diálogos da Base Aérea e Comissários de Voo"
        elif name.startswith('Aassa'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_15_Carrington_Institute_Defense")
            desc_item = "Diálogos da invasão e defesa do Instituto Carrington"
        elif name.startswith('Abn') or name.startswith('Avault2'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_06_G5_Building")
            desc_item = "Diálogos no Prédio da G5 e no Cofre"
        elif name.startswith('Acarrbye') or name.startswith('Avilgrim'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_04_Carrington_Villa")
            desc_item = "Diálogos na Vila de Carrington (Grimshaw e Carrington)"
        elif name.startswith('Aceta') or name.startswith('Aelvcet'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_14_Deep_Sea")
            desc_item = "Diálogos da missão Deep Sea e nave alienígena Cetan"
        elif name.startswith('Achdroid'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_05_Chicago_Stealth")
            desc_item = "Vozes do Robô de Segurança / Droid G5 nas ruas de Chicago"
        elif name.startswith('Adevr') or name.startswith('Aoffwrk'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_01_dataDyne_Defection")
            desc_item = "Diálogos de Cassandra De Vries e trabalhadores da dataDyne"
        elif name.startswith('Alabacc') or name.startswith('Alabtech'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_01_dataDyne_Defection_Laboratorio")
            desc_item = "Diálogos dos técnicos e cientistas nos laboratórios dataDyne"
        elif name.startswith('Ainv'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_02_dataDyne_Investigation")
            desc_item = "Diálogos da missão de investigação na dataDyne Central"
        elif name.startswith('Aexec') or name.startswith('Ajoexec'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_03_dataDyne_Extraction")
            desc_item = "Diálogos dos executivos e Joanna na extração da dataDyne"
        elif name.startswith('Arlguard') or name.startswith('Arltech'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_08_Area_51_Laboratorio")
            desc_item = "Diálogos dos laboratórios de pesquisa militar da Área 51"
        elif name.startswith('Asaucerexp'):
            cat_rel = os.path.join("03_Fases_Dialogos_InGame", "Fase_09_Area_51_Escape")
            desc_item = "Diálogos da fuga no disco voador na Área 51"

        # 4. Carrington Institute (Treinamento / NPCs / Hologramas)
        elif name.startswith('Arecep'):
            cat_rel = os.path.join("04_Carrington_Institute_Treinamento", "01_Recepcao_e_Boas_Vindas")
            desc_item = "Voz da Recepcionista do Instituto Carrington"
        elif name.startswith('Awepgd') or name.startswith('Awepsc'):
            cat_rel = os.path.join("04_Carrington_Institute_Treinamento", "02_Estande_de_Tiro_e_Armas")
            desc_item = "Instrutores e técnicos do estande de tiro"
        elif name.startswith('Atr'):
            cat_rel = os.path.join("04_Carrington_Institute_Treinamento", "03_Treinamento_com_Instrutores")
            desc_item = "Instruções de treino (Carrington, Foster, Grimshaw, Rogers)"
        elif name.startswith('Aci') or name.startswith('Acsec') or name.startswith('Acstan'):
            cat_rel = os.path.join("04_Carrington_Institute_Treinamento", "04_Personagens_e_Seguranca")
            desc_item = "Funcionários, cientistas e seguranças do Instituto Carrington"
        elif name.startswith('Aholo'):
            cat_rel = os.path.join("04_Carrington_Institute_Treinamento", "05_Hologramas_de_Treino")
            desc_item = "Vozes dos hologramas de combate virtual"
        elif name.startswith('Ajoinst') or name.startswith('Ajorep') or name.startswith('Ajorpld') or name.startswith('Ajosci'):
            cat_rel = os.path.join("04_Carrington_Institute_Treinamento", "06_Joanna_Respostas_Treinamento")
            desc_item = "Respostas e falas de Joanna Dark durante o treinamento"

        # 5. Vozes Genéricas de NPCs
        elif name.startswith('Ascie') or name.startswith('Ascien'):
            cat_rel = os.path.join("05_Vozes_Genericas_e_NPCs", "Cientistas")
            desc_item = "Vozes e reações de cientistas neutros/inimigos"
        else:
            cat_rel = "06_Outros_Audios"
            desc_item = "Áudio geral"

        if cat_rel not in categories:
            categories[cat_rel] = []
        categories[cat_rel].append((f, desc_item))

    total_copied = 0
    total_folders = len(categories)

    print(f"Copiando arquivos e gerando estrutura em '{OUTPUT_BASE_DIR}'...")

    for cat_rel, file_list in sorted(categories.items()):
        dest_folder = os.path.join(OUTPUT_BASE_DIR, cat_rel)
        os.makedirs(dest_folder, exist_ok=True)
        
        info_lines = []
        info_lines.append(f"=== {os.path.basename(dest_folder)} ===")
        info_lines.append(f"Total de áudios nesta cena: {len(file_list)}\n")
        info_lines.append("Arquivos de áudio:")

        for f, desc in file_list:
            src_f = os.path.join(AUDIO_SRC_DIR, f)
            dst_f = os.path.join(dest_folder, f)
            shutil.copy2(src_f, dst_f)
            total_copied += 1
            
            # Buscar transcrição se houver
            file_key = f[:-4].upper()
            transcript = transcriptions.get(f"FILE_{file_key}", transcriptions.get(file_key, ""))
            trans_str = f" | Legenda: \"{transcript}\"" if transcript else ""
            info_lines.append(f"  - {f:<20} | {desc}{trans_str}")

        info_lines.append("\nInstruções para dublagem/substituição:")
        info_lines.append("  Para substituir qualquer áudio, mantenha o mesmo nome do arquivo .mp3 correspondente.")
        
        # Gravar arquivo de info na pasta da cena
        info_path = os.path.join(dest_folder, "informacoes_da_cena.txt")
        with open(info_path, 'w', encoding='utf-8') as infof:
            infof.write("\n".join(info_lines) + "\n")

    # Criar um README geral no diretório raiz de saída
    readme_path = os.path.join(OUTPUT_BASE_DIR, "README_ESTRUTURA_CENAS.txt")
    with open(readme_path, 'w', encoding='utf-8') as rf:
        rf.write("=====================================================================\n")
        rf.write("     PERFECT DARK - ÁUDIOS EXTRAÍDOS E ORGANIZADOS POR CENAS         \n")
        rf.write("=====================================================================\n\n")
        rf.write(f"Total de Áudios Extraídos: {total_copied}\n")
        rf.write(f"Total de Pastas / Cenas: {total_folders}\n\n")
        rf.write("ESTRUTURA DAS PASTAS:\n\n")
        rf.write("1. 01_Cutscenes/\n")
        rf.write("   - Contém as 29 sequências cinematográficas completas do jogo,\n")
        rf.write("     cada uma em sua própria pasta numerada e com o título da cena.\n\n")
        rf.write("2. 02_Briefings_de_Missao/\n")
        rf.write("   - Contém todos os briefings narrados por Daniel Carrington\n")
        rf.write("     para cada uma das 9 missões principais.\n\n")
        rf.write("3. 03_Fases_Dialogos_InGame/\n")
        rf.write("   - Contém os diálogos e falas acionados durante o gameplay de cada fase\n")
        rf.write("     (Area 51, Air Force One, Chicago, dataDyne, Pelagic II, etc.).\n\n")
        rf.write("4. 04_Carrington_Institute_Treinamento/\n")
        rf.write("   - Contém todos os áudios do Centro de Treinamento do Instituto Carrington,\n")
        rf.write("     separados por Estande de Tiro, Aparelhos, Instrutores, Recepção e Respostas de Joanna.\n\n")
        rf.write("5. 05_Vozes_Genericas_e_NPCs/\n")
        rf.write("   - Vozes e reações de cientistas e outros NPCs.\n\n")
        rf.write("=====================================================================\n")
        rf.write("LISTAGEM COMPLETA DAS PASTAS E CENAS:\n")
        rf.write("=====================================================================\n\n")
        for cat_rel, file_list in sorted(categories.items()):
            rf.write(f"Pasta: {cat_rel} ({len(file_list)} áudios)\n")
            for f, desc in file_list:
                rf.write(f"   * {f:<20} - {desc}\n")
            rf.write("\n")

    print(f"\nExtração e organização concluídas com sucesso!")
    print(f"Total de arquivos organizados: {total_copied} em {total_folders} pastas.")
    print(f"Diretório de destino: '{OUTPUT_BASE_DIR}'")
    print(f"Guia geral gerado em: '{readme_path}'")
    return True

if __name__ == '__main__':
    organize_audio()
