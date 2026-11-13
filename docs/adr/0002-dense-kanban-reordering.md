# ADR-0002: Estratégia de Reordenação Densa no Kanban

* **Status:** Aceito
* **Data:** 2026-11-12

## Contexto
No Kanban, tarefas são organizadas visualmente em colunas de status. Ao mover uma tarefa para uma coluna ou reordená-la dentro da mesma coluna, a ordem visual deve ser preservada. 

## Decisão
Adotamos a **Reordenação Densa com Deslocamento (Shift)**:
- As tarefas em cada coluna possuem posições inteiras contíguas indexadas em zero (`0, 1, 2, ... N-1`).
- Ao mover uma tarefa para a posição `k` da coluna destino:
  1. As tarefas da coluna destino com posição `>= k` têm suas posições incrementadas em 1 (`pos = pos + 1`).
  2. A tarefa movida assume a posição `k` e o novo status.
  3. Se a movimentação for entre colunas diferentes, a coluna de origem fecha a lacuna decrementando as posições posteriores (`pos = pos - 1`).

## Consequências
* **Positivas:** Posições sempre limpas, previsíveis e consistentes no banco de dados. Sem necessidade de cálculo de números flutuantes ou gaps de Lexorank.
* **Negativas:** Requer um pequeno update em lote nas tarefas da coluna afetada dentro de uma mesma transação (`@Transactional`). Para escala de quadros de uso pessoal (dezenas ou centenas de tarefas por coluna), o impacto de performance é desprezível (<5ms).
