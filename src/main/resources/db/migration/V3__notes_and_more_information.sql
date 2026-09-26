ALTER TABLE questions ADD COLUMN more_information TEXT;

CREATE TABLE notes (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES questions(id),
    note_description TEXT NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, question_id)
);
CREATE INDEX idx_notes_question ON notes(question_id);
CREATE INDEX idx_notes_user ON notes(user_id);
