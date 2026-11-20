import asyncio
import edge_tts
import subprocess
import os

RESUMO = """
Resumo da Aula 1: O Primeiro Passo Para Ser Visto Como Executivo.

Curso: Virada do Líder, De Executor a Executivo 30K mais.
Instrutora: Patrícia Gopian, Cofundadora e CEO da Bússola Executiva.

Problema Central.
Muitos profissionais trabalham duro, entregam resultados, resolvem problemas e fazem mais do que é pedido, mas continuam invisíveis aos olhos da alta gestão. Enquanto isso, pessoas que entregam menos avançam mais rápido.
O que falta não é esforço. É estratégia.
Carreira é estratégia. Não é sobre sorte, empenho ou esforço.

Conceito Chave: Percepção.
O primeiro ensinamento fundamental é que não é sobre o quanto você trabalha, é sobre percepção.
Percepção é a imagem que você transmite, o que as pessoas dizem sobre você ao te observarem.
Uma coisa é o que você sabe sobre si mesmo. Outra é o que as pessoas percebem em você.
Percepção não se impõe, se transmite.
No mundo corporativo, a percepção é mais forte que a descrição de cargo.
Ou você domina os seus recados ou você vai ser dominado pelo meio.

Os 3 Cês do Executivo de Sucesso.
O primeiro é Consciência. Ter clareza sobre como você é percebido e agir de forma deliberada.
Os outros dois serão revelados nas aulas 2 e 3.

Visibilidade Executiva.
Visibilidade é uma construção consciente. Ela não acontece por acaso.
Não é apenas ser visto. É se posicionar estrategicamente para que sua presença seja notada e reconhecida.
Não depende da área, do cargo, do tamanho da empresa, do segmento, nem se é presencial, híbrido ou remoto.
Você não é o cargo onde está. Você é o que permite que as pessoas percebam em você.

Posicionamento Executivo Padrão Bússola.
É o equilíbrio entre duas forças: Direção, saber onde você quer chegar. E Estratégia, como você vai chegar lá.

As 3 Frentes são:
Primeira: Liderança. A liderança que você exerce.
Segunda: Comunicação. A comunicação que você utiliza.
Terceira: Networking. As conexões que você acessa.

Líder Executor versus Líder Executivo.
O executor está preso no operacional, apagando incêndio, ou seja, está invisível.
O executivo conduz a carreira com direção e estratégia, ou seja, está visível.
O executor é visto como limitado e bom pro cargo atual.
O executivo é visto como potencial para níveis acima.
O executor vive sobrecarregado.
O executivo tem tempo para ser estratégico.
O executor é visto como bom entregador de resultado.
O executivo é visto como alguém que gera valor.
O executor não tem força na voz.
O executivo tem suas ideias consideradas e respeitadas.
O executor torce para dar certo, faz carreira de esperança.
O executivo faz dar certo, conduz sua jornada.

Armadilha do Executor.
Quando você se posiciona como executor, o time te usa sem você perceber.
Trazem problemas para você resolver em vez de buscar soluções próprias.
A solução é desenvolver visão holística de negócio.

Autossabotagem.
Desculpas comuns que travam sua carreira:
Para mim é mais difícil.
O meu caso é diferente.
Na empresa que eu tô não funciona.
O meu líder não me dá espaço.
Todo mundo tem desafios. A pergunta é se você está preparado para lidar com eles.

Principais Lições.
Primeira: Carreira é estratégia, não sorte ou esforço bruto.
Segunda: Percepção é mais forte que descrição de cargo.
Terceira: Consciência é o primeiro passo.
Quarta: Você não é o seu cargo.
Quinta: Pare de ser executor, comece a ser executivo.
Sexta: As 3 frentes são Liderança, Comunicação e Networking.
Sétima: Seja o CEO da sua carreira.
"""

OUTPUT_FILE = os.path.join(os.path.dirname(__file__), "aula1-resumo-audio.mp3")
VOICE = "pt-BR-AntonioNeural"  # Brazilian Portuguese male voice

async def main():
    print(f"Gerando audio com voz: {VOICE}...")
    communicate = edge_tts.Communicate(RESUMO.strip(), VOICE, rate="-5%")
    await communicate.save(OUTPUT_FILE)
    print(f"Audio salvo em: {OUTPUT_FILE}")

    # Try to play it
    print("Reproduzindo...")
    try:
        subprocess.run(["powershell.exe", "-c", f"Start-Process '{OUTPUT_FILE}'"], check=True)
    except Exception:
        try:
            subprocess.run(["mpv", OUTPUT_FILE], check=True)
        except Exception:
            try:
                subprocess.run(["ffplay", "-nodisp", "-autoexit", OUTPUT_FILE], check=True)
            except Exception:
                print(f"Nao consegui reproduzir automaticamente.")
                print(f"Abra o arquivo manualmente: {OUTPUT_FILE}")

asyncio.run(main())
