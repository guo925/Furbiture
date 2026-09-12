---
description: 提交代码 — 先执行单元测试与质量审查门禁，全部通过后生成 commit message 并推送
argument-hint: [--force]
---

# /git-save — 带门禁的代码提交

按顺序执行以下步骤。**任一步失败即停止，不要自行绕过门禁。**

## 步骤 1：解析参数

`$ARGUMENTS` 的取值：

- 为空 → **正常模式**
- 含 `--force` → **强制模式**（跳过质检）。强制模式下必须先向用户展示将被跳过的检查项，
  并**等待用户明确回复确认**后才可继续；未确认则终止。

## 步骤 2：判断有无 Java 变更

```bash
git diff HEAD --name-only -- '*.java' ':!*Test*.java'
git status --short -- '*.java'
```

- **无 Java 变更** → 跳过质检，直接进入步骤 5
- **有 Java 变更** → 进入步骤 3

## 步骤 3：执行质检门禁

用 Agent 工具启动 `gitcommit-agent`，把当前变更交给它完成流水线检查。
它会并行运行 `tester`（单元测试）与 `quality-engineer`（四维度质量审查），
并把结果写入 `.claude/checks/tester-result.json` 与 `.claude/checks/quality-result.json`。

> ⚠️ **职责边界**：`gitcommit-agent` **只负责质检，不负责提交**。
> 提交动作由本命令在它返回后执行，避免两者互相回调造成死循环。

## 步骤 4：判定质检结果

```bash
cat .claude/checks/tester-result.json 2>/dev/null || echo '{"pass":false,"error":"标记文件缺失"}'
cat .claude/checks/quality-result.json 2>/dev/null || echo '{"pass":false,"error":"标记文件缺失"}'
```

| 情况 | 处理 |
|---|---|
| 两个 `pass` 均为 `true` | 进入步骤 5 |
| 任一为 `false`，或标记文件缺失 | **停止提交**，列出未通过的检查项与具体问题，提示修复后重跑 |

通过条件（与 `quality-engineer` 的约定一致）：**零严重问题 + 零高危问题 + 综合评分 ≥ 60**，
且单元测试全部通过。

## 步骤 5：生成 commit message 并提交

**commit message 格式**（遵循 Conventional Commits）：

```
<type>: <简短描述>

<可选的正文，说明改了什么、为什么改>

Co-Authored-By: Claude Code <noreply@anthropic.com>
```

`<type>` 取值：`feat` / `fix` / `refactor` / `docs` / `test` / `chore` / `perf` / `ci`

**执行提交**：

```bash
git add -A
git commit -m "<生成的 message>"
git push
```

若当前分支没有上游分支，用 `git push -u origin HEAD`。

## 步骤 6：输出结果

向用户简要汇报：提交是否成功、commit hash、推送目标分支、质检得分与测试通过数。

---

## 前置说明

本命令受 `.claude/hooks/pre-commit` 保护。该 hook 会校验：

1. 两个标记文件存在且 `pass` 为 `true`
2. 标记文件中的 `commit_hash` 与**当前工作区变更指纹一致**

第 2 条是关键：如果质检之后又改了代码，指纹会变化，标记失效，提交被拦截——
必须重新跑一次门禁。这是为了防止"先拿通过标记、再偷偷改代码"绕过质检。

紧急情况下可用 `git commit --no-verify` 跳过 hook，但**这会让门禁完全失效**，仅在明确知情时使用。
