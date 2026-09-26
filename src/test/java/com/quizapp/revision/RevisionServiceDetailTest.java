package com.quizapp.revision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.quizapp.chapter.ChapterQuestionRepository;
import com.quizapp.common.util.QuestionPayloadMapper;
import com.quizapp.user.AuthService;
import com.quizapp.question.Question;
import com.quizapp.question.QuestionRepository;
import com.quizapp.revision.dto.RevisionDetailDto;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RevisionServiceDetailTest {

    private final RevisionRepository revisions = mock(RevisionRepository.class);
    private final RevisionQuestionRepository revisionQuestions = mock(RevisionQuestionRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final RevisionAlgorithm algorithm = mock(RevisionAlgorithm.class);
    private final QuestionPayloadMapper payloadMapper = mock(QuestionPayloadMapper.class);
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ChapterQuestionRepository chapterQuestions = mock(ChapterQuestionRepository.class);
    private final AuthService authService = mock(AuthService.class);
    private final RevisionService service = new RevisionService(
            revisions,
            revisionQuestions,
            questions,
            chapterQuestions,
            algorithm,
            payloadMapper,
            authService,
            executor);

    @AfterEach
    void closeExecutor() {
        executor.close();
    }

    @Test
    void detailIncludesAnswerAndExplanation() {
        Revision revision = new Revision();
        setId(revision, 5L);
        revision.setUserId(42L);
        revision.setSubjectId(7L);
        revision.setStatus("IN_PROGRESS");
        RevisionQuestion link = new RevisionQuestion();
        setId(link, 8L);
        link.setRevisionId(5L);
        link.setQuestionId(11L);
        link.setReason("WRONG");
        link.setRemainingReviews(3);
        Question question = new Question();
        setId(question, 11L);
        question.setCorrectOption("b");
        question.setAllottedTimeMs(60_000);

        when(revisions.findById(5L)).thenReturn(java.util.Optional.of(revision));
        when(revisionQuestions.findDue(
                        org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(List.of(link));
        when(questions.findAllById(List.of(11L))).thenReturn(List.of(question));
        when(payloadMapper.text(question)).thenReturn("Question?");
        when(payloadMapper.options(question)).thenReturn(Map.of("a", "No", "b", "Yes"));
        when(payloadMapper.explanation(question)).thenReturn("Because.");

        RevisionDetailDto detail = service.getDueQuestions(5L, 42L, null);

        assertThat(detail.questions()).hasSize(1);
        assertThat(detail.questions().getFirst().correctOption()).isEqualTo("b");
        assertThat(detail.questions().getFirst().explanation()).isEqualTo("Because.");
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
