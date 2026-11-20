1. Desafio:

Desafio
Você foi contratado como consultor júnior em uma empresa de Tecnologia da Informação que está modernizando seus sistemas. Seu primeiro desafio é ajudar a equipe a padronizar a identificação de projetos internos. Cada projeto recebe um nome, mas para facilitar buscas e integrações, todos os nomes devem ser convertidos para letras maiúsculas. Além disso, a equipe deseja saber quantos caracteres o nome do projeto possui, para garantir que não ultrapasse o limite permitido pelo sistema legado. Sua tarefa é criar um programa que, dado o nome de um projeto, retorne o nome em letras maiúsculas seguido do número de caracteres, separados por um espaço.

Implemente uma solução que leia uma linha contendo o nome do projeto e produza uma linha de saída com o nome em maiúsculas e o comprimento do nome, separados por um espaço. Não utilize bibliotecas externas, apenas recursos padrão da linguagem. Considere que o nome do projeto pode conter letras, números e espaços, e nunca estará vazio.

Entrada
Uma única linha contendo o nome do projeto, composto por letras, números e espaços.

Saída
Uma única linha contendo o nome do projeto em letras maiúsculas, seguido de um espaço e do número de caracteres do nome.

Exemplos
A tabela abaixo apresenta exemplos de entrada e saída:

Entrada	| Saída
intranet portal |	INTRANET PORTAL 15
cloud9	| CLOUD9 6
api v2	| API V2 6
datahub2024	| DATAHUB2024 11

2. Desafio:
Você foi contratado como consultor júnior em uma empresa de Tecnologia da Informação que está modernizando seus sistemas. Seu primeiro desafio é ajudar a equipe a padronizar nomes de projetos, que chegam em diferentes formatos e estilos. Para garantir a organização dos arquivos, todos os nomes de projetos devem ser convertidos para letras maiúsculas. Além disso, se o nome do projeto estiver vazio ou contiver apenas espaços, você deve alertar a equipe informando que o nome é inválido. Sua tarefa é criar um programa que receba o nome de um projeto e retorne o nome em letras maiúsculas, ou a mensagem "INVALIDO" caso o nome seja vazio ou apenas espaços.

Implemente uma solução simples, sem o uso de bibliotecas externas, que leia uma linha contendo o nome do projeto e produza a saída conforme as regras acima. Certifique-se de tratar corretamente casos em que o nome está em branco ou contém apenas espaços.

Entrada
Uma única linha contendo o nome do projeto, que pode conter letras, números, espaços e caracteres especiais. O nome pode estar vazio ou conter apenas espaços.

Saída
Se o nome do projeto for válido, imprima o nome em letras maiúsculas. Caso contrário, imprima a palavra "INVALIDO".

Exemplos
A tabela abaixo apresenta exemplos de entrada e saída:

Entrada	| Saída
intranet2024 |	INTRANET2024
sistema financeiro |	SISTEMA FINANCEIRO
devOps pipeline | DEVOPS PIPELINE
Cloud-API |	CLOUD-API