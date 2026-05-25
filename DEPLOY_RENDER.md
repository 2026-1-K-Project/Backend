# Render CI/CD Setup

## 1) Create Web Service
- Runtime: `Docker`가 아니라 `Native`(Java)
- Branch: `main`
- Build Command:
  - `./gradlew bootJar`
- Start Command:
  - `java -jar build/libs/*.jar`

## 2) Environment Variables (Render)
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OPENAI_API_KEY`
- (optional) `OPENAI_MODEL`

## 3) Generate Deploy Hook URL
- Render Web Service -> `Settings` -> `Deploy Hook`
- 새 Hook 생성 후 URL 복사

## 4) GitHub Secrets
레포지토리 `Settings` -> `Secrets and variables` -> `Actions`에 아래 추가:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OPENAI_API_KEY`
- `RENDER_DEPLOY_HOOK_URL`

## 5) CI/CD Flow
- PR to `main`: 테스트만 실행
- Push to `main`: 테스트 통과 후 Render Deploy Hook 호출
