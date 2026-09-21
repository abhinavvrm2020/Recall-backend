CREATE TABLE chapters (
    id              BIGSERIAL PRIMARY KEY,
    subject_id      BIGINT NOT NULL REFERENCES subjects(id),
    title           VARCHAR(200) NOT NULL,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
);

CREATE INDEX idx_chapters_subject ON chapters(subject_id);

CREATE TABLE chapter_questions (
    chapter_id      BIGINT NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    question_id     BIGINT NOT NULL REFERENCES questions(id),
    position        INTEGER NOT NULL,
    PRIMARY KEY (chapter_id, question_id)
);

CREATE INDEX idx_chapter_questions_order ON chapter_questions(chapter_id, position);

CREATE TABLE chapter_progress (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    chapter_id          BIGINT NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    status              VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
    current_index       INTEGER NOT NULL DEFAULT 0,
    correct_count       INTEGER NOT NULL DEFAULT 0,
    wrong_count         INTEGER NOT NULL DEFAULT 0,
    answers_json        JSONB NOT NULL DEFAULT '[]'::jsonb,
    revision_id         BIGINT REFERENCES revisions(id),
    last_active_at      TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    UNIQUE (user_id, chapter_id)
);

CREATE INDEX idx_chapter_progress_user ON chapter_progress(user_id);
