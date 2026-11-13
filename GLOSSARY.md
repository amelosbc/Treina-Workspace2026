# GLOSSARY: TaskManager Domain Terms

Este glossário define a linguagem ubíqua (*Ubiquitous Language*) do projeto TaskManager.

---

### User (Usuário)
* **Definição:** Entidade que representa a identidade do operador no sistema.
* **Origem da Identidade:** Gerenciada externamente pelo **Clerk** (armazenado via `clerkUserId`).
* **Sincronização:** Sincronizado automaticamente (*Just-in-Time*) na primeira requisição autenticada daquele usuário.

### Board (Quadro)
* **Definição:** Contêiner de alto nível que agrupa e contextualiza um fluxo de trabalho (ex: "Estudos DIO", "Trabalho").
* **Propriedade:** Pertence a exatamente um `User`.
* **Onboarding:** Todo novo usuário recebe automaticamente um quadro inicial denominado `"Meu Primeiro Quadro"`.

### Task (Tarefa / Cartão)
* **Definição:** A menor unidade de trabalho gerenciável no sistema.
* **Pertencimento:** Pertence a exatamente um `Board`.
* **Atributos:** Possui Título, Descrição em Markdown, Prioridade, Status, Data Limite (`dueDate`) e Posição.

### TaskStatus (Status do Kanban)
* **Definição:** O estágio atual da tarefa dentro do fluxo de trabalho.
* **Valores Permitidos:**
  - `TO_DO`: A fazer / backlog imediato.
  - `IN_PROGRESS`: Em execução ativa.
  - `IN_REVIEW`: Concluída aguardando revisão ou validação.
  - `DONE`: Finalizada com sucesso.

### TaskPriority (Prioridade da Tarefa)
* **Definição:** Nível de urgência e importância para execução.
* **Valores:** `LOW` (Baixa), `MEDIUM` (Média - padrão), `HIGH` (Alta), `URGENT` (Urgente).

### Column Position (Posição na Coluna)
* **Definição:** Índice inteiro sequencial (`0, 1, 2...`) que determina a ordem visual de exibição das tarefas dentro de uma mesma coluna de status.
* **Shift (Deslocamento):** Ao mover uma tarefa para uma posição existente, as tarefas subsequentes são deslocadas para acomodar o novo item.

### Soft Delete (Exclusão Lógica)
* **Definição:** Remoção lógica do registro através do preenchimento do campo `deletedAt`. O registro permanece no banco para integridade e histórico, mas é ignorado em todas as consultas da API.
