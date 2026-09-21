package com.quizapp.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizapp.chapter.Chapter;
import com.quizapp.chapter.ChapterQuestion;
import com.quizapp.chapter.ChapterQuestionRepository;
import com.quizapp.chapter.ChapterRepository;
import com.quizapp.question.Question;
import com.quizapp.question.QuestionRepository;
import com.quizapp.subject.Subject;
import com.quizapp.subject.SubjectRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;

class McqSeedRunnerTest {

    private final SubjectRepository subjects = mock(SubjectRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final ChapterRepository chapters = mock(ChapterRepository.class);
    private final ChapterQuestionRepository chapterQuestions = mock(ChapterQuestionRepository.class);
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @AfterEach
    void closeExecutor() {
        executor.close();
    }

    @Test
    void seedsOneFullPracticeChapterWithEveryBankQuestion() throws Exception {
        Subject subject = new Subject();
        setId(subject, 7L);
        Question first = question(11L);
        Question second = question(12L);
        when(subjects.findByName("Indian History")).thenReturn(Optional.of(subject));
        when(questions.countBySubjectId(7L)).thenReturn(2L);
        when(questions.findBySubjectId(7L)).thenReturn(List.of(first, second));
        when(chapters.existsBySubjectIdAndTitle(7L, "Full practice set")).thenReturn(false);
        when(chapters.save(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            setId(chapter, 20L);
            return chapter;
        });

        McqSeedRunner runner = new McqSeedRunner(
                true,
                60_000,
                new ObjectMapper(),
                subjects,
                questions,
                chapters,
                chapterQuestions,
                executor);
        runner.run(mock(ApplicationArguments.class));

        ArgumentCaptor<Chapter> chapterCaptor = ArgumentCaptor.forClass(Chapter.class);
        verify(chapters).save(chapterCaptor.capture());
        assertThat(chapterCaptor.getValue().getTitle()).isEqualTo("Full practice set");
        assertThat(chapterCaptor.getValue().getSortOrder()).isZero();
        assertThat(chapterCaptor.getValue().getStatus()).isEqualTo("ACTIVE");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChapterQuestion>> linksCaptor = ArgumentCaptor.forClass(List.class);
        verify(chapterQuestions).saveAll(linksCaptor.capture());
        assertThat(linksCaptor.getValue())
                .extracting(ChapterQuestion::getChapterId, ChapterQuestion::getQuestionId, ChapterQuestion::getPosition)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(20L, 11L, 0),
                        org.assertj.core.groups.Tuple.tuple(20L, 12L, 1));
    }

    private static Question question(Long id) {
        Question question = new Question();
        setId(question, id);
        return question;
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
