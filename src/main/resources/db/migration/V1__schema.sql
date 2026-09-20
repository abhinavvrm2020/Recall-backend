CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE subjects (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE questions (
    id              BIGSERIAL PRIMARY KEY,
    question_json   JSONB NOT NULL,
    correct_option  VARCHAR(8) NOT NULL,
    subject_id      BIGINT NOT NULL REFERENCES subjects(id),
    allotted_time_ms INTEGER NOT NULL DEFAULT 60000
);

CREATE INDEX idx_questions_subject ON questions(subject_id);

CREATE TABLE quizzes (
    id              BIGSERIAL PRIMARY KEY,
    subject_id      BIGINT NOT NULL REFERENCES subjects(id),
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    total_questions INTEGER NOT NULL,
    type            VARCHAR(32) NOT NULL DEFAULT 'PRACTICE'
);

CREATE INDEX idx_quizzes_subject ON quizzes(subject_id);

CREATE TABLE quiz_questions (
    quiz_id         BIGINT NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    question_id     BIGINT NOT NULL REFERENCES questions(id),
    position        INTEGER NOT NULL,
    PRIMARY KEY (quiz_id, question_id)
);

CREATE TABLE user_quiz_attempts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    quiz_id         BIGINT NOT NULL REFERENCES quizzes(id),
    total_correct   INTEGER,
    total_questions INTEGER,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

CREATE INDEX idx_attempts_user ON user_quiz_attempts(user_id, completed_at DESC);

CREATE TABLE user_quiz_attempt_questions (
    attempt_id      BIGINT NOT NULL REFERENCES user_quiz_attempts(id) ON DELETE CASCADE,
    question_id     BIGINT NOT NULL REFERENCES questions(id),
    correct         BOOLEAN NOT NULL,
    time_taken_ms   INTEGER NOT NULL,
    PRIMARY KEY (attempt_id, question_id)
);

CREATE TABLE revisions (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    subject_id          BIGINT NOT NULL REFERENCES subjects(id),
    source_attempt_id   BIGINT REFERENCES user_quiz_attempts(id),
    status              VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_revisions_user ON revisions(user_id, status);

CREATE TABLE revision_questions (
    id                  BIGSERIAL PRIMARY KEY,
    revision_id         BIGINT NOT NULL REFERENCES revisions(id) ON DELETE CASCADE,
    question_id         BIGINT NOT NULL REFERENCES questions(id),
    reason              VARCHAR(16) NOT NULL,
    remaining_reviews   INTEGER NOT NULL,
    next_review_at      TIMESTAMPTZ NOT NULL,
    UNIQUE (revision_id, question_id)
);

CREATE INDEX idx_revision_questions_due
    ON revision_questions(revision_id, next_review_at)
    WHERE remaining_reviews > 0;
