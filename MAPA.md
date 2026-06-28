# Onde colocar cada arquivo

Todos os caminhos são relativos à **raiz do repositório** do projeto novo.

Extraia o `skills-agents.zip` na raiz do repo: ele já contém a árvore
`.claude/...` pronta. Tudo aqui é **novo** (nada a sobrescrever), exceto a fiação
no `spec-driven-build`, que fica como passo seguinte (ver final).

## Skills (pastas inteiras, novas)

| Caminho |
|---|
| `.claude/skills/vertical-slice/SKILL.md` |
| `.claude/skills/vertical-slice/references/estrutura.md` |
| `.claude/skills/vertical-slice/references/regras-arquiteturais.md` |
| `.claude/skills/kotlin-boas-praticas/SKILL.md` |
| `.claude/skills/kotlin-boas-praticas/references/idiomas.md` |
| `.claude/skills/kotlin-boas-praticas/references/modelagem.md` |
| `.claude/skills/testes-unitarios/SKILL.md` |
| `.claude/skills/testes-unitarios/references/principios.md` |
| `.claude/skills/testes-unitarios/references/kotlin-spring.md` |
| `.claude/skills/virtual-threads-pinning/SKILL.md` |
| `.claude/skills/virtual-threads-pinning/references/deteccao-correcao.md` |

## Agents (arquivos novos)

| Caminho | Modelo |
|---|---|
| `.claude/agents/implementador.md` | sonnet |
| `.claude/agents/revisor-spec.md` | opus |
| `.claude/agents/revisor-qualidade.md` | sonnet |
| `.claude/agents/explorador.md` | haiku |

## Próximo passo — fiação no spec-driven-build (T9)

Não incluí aqui porque depende dos **seus** 4 arquivos atuais do
`spec-driven-build` (`workflow.md`, `quality-checklists.md`, `spec-template.md`,
`project-context-template.md`) para editar em cima sem perder o conteúdo que já
existe. Quando puder colar esses 4, eu aplico:

- `workflow.md`: referência às skills nas fases de spec e de criação de tasks.
- `quality-checklists.md`: checklist de conformidade como gate.
- `spec-template.md`: seção exigindo declaração de conformidade com cada skill.
- `project-context-template.md`: ponteiro leve listando as skills de padrão.

## Notas

- **Reconstruído a partir dos padrões** das nossas conversas, não dos arquivos
  originais (que não estavam disponíveis). Revise os exemplos genéricos e ajuste
  ao domínio real do projeto novo.
- Subagente **não herda** skills da sessão pai — por isso o `implementador` lista
  as skills no frontmatter explicitamente.
- O orquestrador (loop do `spec-driven-build`) chama, por task:
  `implementador` → `revisor-spec` → `revisor-qualidade`. O `explorador` roda
  antes, na fase de levantamento.
