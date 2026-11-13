# 01: Persistência H2 em Arquivo e Tipos de Domínio

**What to build:**
Configurar a persistência de dados no H2 em arquivo (`./data/taskdb`) para reter as alterações entre reinicializações da aplicação e criar os tipos de domínio `TaskStatus` e `TaskPriority` que sustentam o Kanban.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Atualizar `application.properties` para `jdbc:h2:file:./data/taskdb` com diretório ignorado no Git.
- [ ] Criar enum `TaskStatus` (`TO_DO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`) no pacote domain.
- [ ] Criar enum `TaskPriority` (`LOW`, `MEDIUM`, `HIGH`, `URGENT`) no pacote domain.
- [ ] Teste unitário e de contexto do Spring garantindo que o banco sobe e cria o arquivo de dados sem erros.
