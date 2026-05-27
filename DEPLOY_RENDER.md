# Render CI/CD Setup

## 1) Service Runtime
- Use `Docker` runtime (Render does not provide native Java runtime in current options).
- Keep `render.yaml` at repo root.

## 2) Deploy with Blueprint
1. Render Dashboard -> `New` -> `Blueprint`
2. Select repository: `2026-1-K-Project/Backend`
3. Blueprint path: `render.yaml`
4. Create/Sync

## 3) Environment Variables (Render)
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OPENAI_API_KEY`
- (optional) `OPENAI_MODEL`

## 4) GitHub Secrets (for CI/CD workflow)
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OPENAI_API_KEY`
- `RENDER_DEPLOY_HOOK_URL`

## 5) CI/CD Flow
- PR to `main`: run tests
- Push to `main`: run tests, then trigger Render deploy hook
