package com.quizapp.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizapp.chapter.Chapter;
import com.quizapp.chapter.ChapterQuestionRepository;
import com.quizapp.chapter.ChapterRepository;
import com.quizapp.common.util.QuestionPayloadMapper;
import com.quizapp.question.Question;
import com.quizapp.question.QuestionRepository;
import com.quizapp.subject.Subject;
import com.quizapp.subject.SubjectRepository;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.ClassPathResource;

class McqSeedRunnerTest {

    private final SubjectRepository subjects = mock(SubjectRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final ChapterRepository chapters = mock(ChapterRepository.class);
    private final ChapterQuestionRepository chapterQuestions = mock(ChapterQuestionRepository.class);
    private final QuestionPayloadMapper questionPayloadMapper = mock(QuestionPayloadMapper.class);
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ObjectMapper mapper = new ObjectMapper();

    @AfterEach
    void closeExecutor() {
        executor.close();
    }

    @Test
    void rebuildsTopicChaptersWhenLegacyFullPracticeSetExists() throws Exception {
        Subject subject = new Subject();
        setId(subject, 7L);

        JsonNode root = mapper.readTree(new ClassPathResource("data/mcqs.json").getInputStream());
        List<Question> bank = new ArrayList<>();
        long id = 1;
        for (JsonNode item : root.path("mcqs")) {
            Question q = new Question();
            setId(q, id++);
            String text = item.path("question").asText();
            q.setQuestionJson(mapper.createObjectNode().put("question", text).toString());
            bank.add(q);
            when(questionPayloadMapper.text(q)).thenReturn(text);
        }

        Chapter legacy = new Chapter();
        setId(legacy, 99L);
        legacy.setTitle("Full practice set");
        legacy.setSubjectId(7L);

        when(subjects.findByName("Indian History")).thenReturn(Optional.of(subject));
        when(questions.countBySubjectId(7L)).thenReturn((long) bank.size());
        when(questions.findBySubjectId(7L)).thenReturn(bank);
        when(chapters.existsBySubjectIdAndTitle(7L, "Full practice set")).thenReturn(true);
        when(chapters.countBySubjectIdAndTitleNot(7L, "Full practice set")).thenReturn(0L);
        when(chapters.findBySubjectId(7L)).thenReturn(List.of(legacy));
        when(chapters.save(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            setId(chapter, Math.abs(chapter.getTitle().hashCode() * 1L));
            return chapter;
        });

        McqSeedRunner runner = new McqSeedRunner(
                true,
                60_000,
                mapper,
                subjects,
                questions,
                chapters,
                chapterQuestions,
                questionPayloadMapper,
                executor);
        runner.run(mock(ApplicationArguments.class));

        verify(chapterQuestions).deleteByChapterId(99L);
        verify(chapters).deleteAll(List.of(legacy));

        ArgumentCaptor<Chapter> chapterCaptor = ArgumentCaptor.forClass(Chapter.class);
        verify(chapters, atLeastOnce()).save(chapterCaptor.capture());
        assertThat(chapterCaptor.getAllValues())
                .extracting(Chapter::getTitle)
                .doesNotContain("Full practice set")
                .contains("The Advent of European Companies", "Subhash Chandra Bose and Azad Hind Fauj");
        assertThat(chapterCaptor.getAllValues().size()).isGreaterThan(20);
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
