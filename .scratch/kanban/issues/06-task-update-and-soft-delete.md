# 06: Edição e Soft Delete de Tarefas com Ajuste de Coluna

**What to build:**
Endpoints para atualização completa dos campos da tarefa (`PUT /api/boards/{boardId}/tasks/{taskId}`) e remoção lógica via Soft Delete (`DELETE /api/boards/{boardId}/tasks/{taskId}`), fechando a lacuna de posições deixada na coluna.

**Blocked by:** 04: Cartões do Kanban — Criação e Visualização Agrupada

**Status:** ready-for-agent

- [ ] Implementar endpoint `PUT /api/boards/{boardId}/tasks/{taskId}` para atualizar título, descrição, prioridade e data limite.
- [ ] Implementar endpoint `DELETE /api/boards/{boardId}/tasks/{taskId}` marcando `deleted_at = now()`.
- [ ] Ao deletar, decrementar as posições das tarefas que estavam abaixo da excluída na mesma coluna.
- [ ] Garantir que tarefas com `deleted_at != null` são ignoradas em buscas normais e contagens.
- [ ] Testes de integração validando a edição e a integridade de posições após exclusão lógica.
