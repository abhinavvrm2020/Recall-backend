# quiz-backend

Spring Boot 4 / Java 21 quiz API with **virtual threads**, `CompletableFuture` for parallel reads/scoring, Postgres, JWT, and hybrid revision.

## Package layout

```
com.quizapp
├── user/          AuthController → AuthService → UserRepository (+ dto/)
├── subject/       SubjectController → SubjectService → SubjectRepository (+ dto/)
├── quiz/          QuizController → QuizService → Quiz*Repository (+ dto/)
├── attempt/       AttemptController → AttemptService → Attempt*Repository (+ dto/)
├── revision/      RevisionController → RevisionService → Revision*Repository (+ dto/)
├── question/      Question entity + repository
├── common/        ApiException (+ util: AuthContext, Futures, QuestionPayloadMapper)
├── config/        Security, Jackson, VirtualThread
├── security/      JWT
└── seed/          MCQ bootstrap
```

Controllers are thin; business logic and concurrency live in services; persistence in repositories.

## Run

```bash
# Postgres DB `quizapp` must exist
createdb quizapp   # once

# Edit local DB credentials in:
#   src/main/resources/application-local.properties

./mvnw spring-boot:run
```

Server (local): `http://localhost:8081`

## Docker / Railway

Multi-stage `Dockerfile` builds a JRE 21 image. Railway uses it automatically when this repo is connected.

```bash
# Local image smoke-test (needs a reachable Postgres)
docker build -t quiz-backend .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e PORT=8080 \
  -e DATABASE_URL='postgresql://user:pass@host:5432/quizapp' \
  -e JWT_SECRET='your-long-random-secret-at-least-32-chars' \
  quiz-backend
```

### Railway variables

| Variable | Required | Notes |
|----------|----------|--------|
| `DATABASE_URL` | yes | Auto-set when you add Railway Postgres |
| `JWT_SECRET` | yes | Long random string (≥32 chars) |
| `PORT` | auto | Set by Railway |
| `SPRING_PROFILES_ACTIVE` | yes | Set to `prod` (also default in Dockerfile) |
| `SEED_ENABLED` | optional | default `true` on first boot |

Health check: `GET /health` (also `/actuator/health`).

## Auth

```bash
curl -X POST localhost:8081/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada","email":"ada@test.com","password":"secret12"}'
```

Use `Authorization: Bearer <token>` on all other routes.

## Main routes

| Method | Path | Notes |
|--------|------|--------|
| POST | `/api/auth/signup` | |
| POST | `/api/auth/login` | |
| GET | `/api/me` | profile |
| GET | `/api/subjects` | |
| GET | `/api/subjects/{id}/quizzes` | predefined sizes |
| GET | `/api/quizzes/{id}` | questions (no answers) |
| POST | `/api/quizzes/{id}/attempts` | start attempt |
| POST | `/api/attempts/{id}/submit` | score + create revision |
| GET | `/api/me/attempts` | history |
| GET | `/api/revisions` | pending / in progress |
| GET | `/api/revisions/{id}` | due questions |
| POST | `/api/revisions/{id}/submit` | update remaining / next review |

## Config

- **Local DB is always taken from** `src/main/resources/application-local.properties`
- Profile `local` is activated by default (`spring.profiles.active=local`)
- Edit host, database name, username, password, schema, and logging **only** in that properties file

### Schema & tables (Flyway `V1__schema.sql`)

Schema: **`public`**

| Table | Purpose |
|-------|---------|
| `users` | accounts |
| `subjects` | e.g. Indian History |
| `questions` | MCQ bank |
| `quizzes` | PRACTICE sets (10/20/50/100) |
| `quiz_questions` | quiz ↔ question |
| `user_quiz_attempts` | attempt header |
| `user_quiz_attempt_questions` | scored answers |
| `revisions` | revision pack |
| `revision_questions` | due items |
| `flyway_schema_history` | Flyway |

Server (local): **`http://localhost:8081`**

## Postman

All routes are under **`/api`**. Missing `/api` returns **403**.

### 1. Sign up

- **Method:** `POST`
- **URL:** `http://localhost:8081/api/auth/signup`
- **Headers:** `Content-Type: application/json`
- **Body → raw → JSON:**

```json
{
  "name": "Abhinav",
  "email": "abhinavvrm543@gmail.com",
  "password": "123456"
}
```

Copy `token` from the response.

### 2. Log in

- **URL:** `http://localhost:8081/api/auth/login`
- Same JSON shape with `email` + `password` only.

### 3. Authenticated calls

Add header on every other request:

```text
Authorization: Bearer <paste-token-here>
```

Examples:

| Method | URL |
|--------|-----|
| GET | `http://localhost:8081/api/me` |
| GET | `http://localhost:8081/api/subjects` |
| GET | `http://localhost:8081/api/subjects/1/quizzes` |
| GET | `http://localhost:8081/api/quizzes/1` |
| POST | `http://localhost:8081/api/quizzes/1/attempts` |
| POST | `http://localhost:8081/api/attempts/{attemptId}/check` |
| POST | `http://localhost:8081/api/attempts/{attemptId}/submit` |
| GET | `http://localhost:8081/api/revisions` |

**Check answer body:**

```json
{
  "questionId": 1,
  "selectedOption": "a"
}
```
# Company1
