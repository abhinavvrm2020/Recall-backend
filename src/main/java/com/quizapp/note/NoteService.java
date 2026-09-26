package com.quizapp.note;

import com.quizapp.common.ApiException;
import com.quizapp.note.dto.NoteDto;
import com.quizapp.question.QuestionRepository;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final QuestionRepository questionRepository;

    public NoteService(NoteRepository noteRepository, QuestionRepository questionRepository) {
        this.noteRepository = noteRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public NoteDto get(Long userId, Long questionId) {
        requireQuestion(questionId);
        return noteRepository
                .findByUserIdAndQuestionId(userId, questionId)
                .map(this::toDto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Note not found"));
    }

    @Transactional
    public NoteDto upsert(Long userId, Long questionId, String noteDescription) {
        requireQuestion(questionId);
        Instant now = Instant.now();
        Note note = noteRepository
                .findByUserIdAndQuestionId(userId, questionId)
                .orElseGet(() -> {
                    Note created = new Note();
                    created.setUserId(userId);
                    created.setQuestionId(questionId);
                    created.setCreatedAt(now);
                    return created;
                });
        note.setNoteDescription(noteDescription);
        note.setUpdatedAt(now);
        return toDto(noteRepository.save(note));
    }

    @Transactional
    public void delete(Long userId, Long questionId) {
        requireQuestion(questionId);
        noteRepository.deleteByUserIdAndQuestionId(userId, questionId);
    }

    private void requireQuestion(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Question not found");
        }
    }

    private NoteDto toDto(Note note) {
        return new NoteDto(
                note.getId(),
                note.getQuestionId(),
                note.getNoteDescription(),
                note.getUserId(),
                note.getCreatedAt(),
                note.getUpdatedAt());
    }
}
