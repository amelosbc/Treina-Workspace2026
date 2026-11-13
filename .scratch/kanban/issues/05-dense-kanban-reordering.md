# 05: Movimentação e Reordenação Densa no Kanban (PATCH /move)

**What to build:**
Criar o endpoint `PATCH /api/boards/{boardId}/tasks/{taskId}/move` que permite mover uma tarefa para outra coluna e/ou reordenar sua posição, recalculando em lote os índices (`position`) das demais tarefas da coluna de forma atômica e consistente (ADR-0002).

**Blocked by:** 04: Cartões do Kanban — Criação e Visualização Agrupada

**Status:** ready-for-agent

- [ ] Criar DTO `MoveTaskDTO` com campos `newStatus` (opcional) e `newPosition` (obrigatório, >= 0).
- [ ] Implementar serviço de reordenação com shift transacional:
  - Na coluna origem: decrementar posições de quem estava abaixo da tarefa movida.
  - Na coluna destino: incrementar posições de quem está na posição alvo ou posterior.
  - Atualizar status e posição do item movido.
- [ ] Implementar endpoint `PATCH /api/boards/{boardId}/tasks/{taskId}/move` no Controller.
- [ ] Testes unitários exaustivos cobrindo: mover dentro da mesma coluna (para cima/para baixo) e mover entre colunas diferentes.
