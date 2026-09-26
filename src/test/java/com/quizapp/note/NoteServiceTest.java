package com.quizapp.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quizapp.common.ApiException;
import com.quizapp.note.dto.NoteDto;
import com.quizapp.question.QuestionRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock private NoteRepository noteRepository;
    @Mock private QuestionRepository questionRepository;

    private NoteService service;

    @BeforeEach
    void setUp() {
        service = new NoteService(noteRepository, questionRepository);
    }

    @Test
    void upsertCreatesNoteForUserWhenMissing() {
        when(questionRepository.existsById(5L)).thenReturn(true);
        when(noteRepository.findByUserIdAndQuestionId(10L, 5L)).thenReturn(Optional.empty());
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> {
            Note n = inv.getArgument(0);
            setId(n, 99L);
            return n;
        });

        NoteDto dto = service.upsert(10L, 5L, "My note");

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        Note saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getQuestionId()).isEqualTo(5L);
        assertThat(saved.getNoteDescription()).isEqualTo("My note");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(dto.id()).isEqualTo(99L);
        assertThat(dto.userId()).isEqualTo(10L);
    }

    @Test
    void upsertUpdatesExistingNoteWithoutChangingOwner() {
        when(questionRepository.existsById(5L)).thenReturn(true);
        Note existing = new Note();
        setId(existing, 1L);
        existing.setUserId(10L);
        existing.setQuestionId(5L);
        existing.setNoteDescription("Old");
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        existing.setCreatedAt(created);
        existing.setUpdatedAt(created);
        when(noteRepository.findByUserIdAndQuestionId(10L, 5L)).thenReturn(Optional.of(existing));
        when(noteRepository.save(existing)).thenReturn(existing);

        NoteDto dto = service.upsert(10L, 5L, "Updated");

        assertThat(existing.getUserId()).isEqualTo(10L);
        assertThat(existing.getCreatedAt()).isEqualTo(created);
        assertThat(existing.getNoteDescription()).isEqualTo("Updated");
        assertThat(existing.getUpdatedAt()).isAfter(created);
        assertThat(dto.noteDescription()).isEqualTo("Updated");
    }

    @Test
    void getReturns404WhenNoteMissing() {
        when(questionRepository.existsById(5L)).thenReturn(true);
        when(noteRepository.findByUserIdAndQuestionId(10L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(10L, 5L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void upsertFailsWhenQuestionMissing() {
        when(questionRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> service.upsert(10L, 5L, "x"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(noteRepository, never()).save(any());
    }

    private static void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
