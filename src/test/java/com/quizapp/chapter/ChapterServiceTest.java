package com.quizapp.chapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizapp.chapter.dto.ChapterDetailDto;
import com.quizapp.chapter.dto.ChapterSubmitResponse;
import com.quizapp.chapter.dto.ProgressAnswerDto;
import com.quizapp.chapter.dto.ProgressRequest;
import com.quizapp.chapter.dto.SubmitChapterRequest;
import com.quizapp.common.ApiException;
import com.quizapp.common.util.QuestionPayloadMapper;
import com.quizapp.question.Question;
import com.quizapp.question.QuestionRepository;
import com.quizapp.revision.Revision;
import com.quizapp.revision.RevisionAlgorithm;
import com.quizapp.revision.RevisionQuestion;
import com.quizapp.revision.RevisionQuestionRepository;
import com.quizapp.revision.RevisionRepository;
import com.quizapp.subject.SubjectService;
import com.quizapp.user.AuthService;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

    @Mock private ChapterRepository chapterRepository;
    @Mock private ChapterQuestionRepository chapterQuestionRepository;
    @Mock private ChapterProgressRepository progressRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionPayloadMapper questionPayloadMapper;
    @Mock private RevisionAlgorithm revisionAlgorithm;
    @Mock private RevisionRepository revisionRepository;
    @Mock private RevisionQuestionRepository revisionQuestionRepository;
    @Mock private SubjectService subjectService;
    @Mock private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ChapterService service;

    @BeforeEach
    void setUp() {
        service = new ChapterService(
                chapterRepository,
                chapterQuestionRepository,
                progressRepository,
                questionRepository,
                questionPayloadMapper,
                revisionAlgorithm,
                revisionRepository,
                revisionQuestionRepository,
                subjectService,
                authService,
                objectMapper);
    }

    @Test
    void startCreatesInProgressAtIndexZero() {
        Chapter chapter = chapter(10L, 7L);
        when(chapterRepository.findById(10L)).thenReturn(Optional.of(chapter));
        when(progressRepository.findByUserIdAndChapterId(20L, 10L)).thenReturn(Optional.empty());

        service.start(10L, 20L, false);

        verify(progressRepository).save(org.mockito.ArgumentMatchers.any(ChapterProgress.class));
    }

    @Test
    void saveProgressUpdatesIndexCountsAndAnswersJson() throws Exception {
        ChapterProgress progress = progress(20L, 10L, "IN_PROGRESS");
        when(chapterRepository.findById(10L)).thenReturn(Optional.of(chapter(10L, 7L)));
        when(progressRepository.findByUserIdAndChapterId(20L, 10L)).thenReturn(Optional.of(progress));
        List<ProgressAnswerDto> answers =
                List.of(new ProgressAnswerDto(101L, "B", true, 1_200, "SURE"));

        service.saveProgress(10L, 20L, new ProgressRequest(1, 1, 0, answers));

        assertThat(progress.getCurrentIndex()).isEqualTo(1);
        List<ProgressAnswerDto> stored =
                objectMapper.readValue(progress.getAnswersJson(), new TypeReference<>() {});
        assertThat(stored).isEqualTo(answers);
        verify(authService).recordActivity(20L);
    }

    @Test
    void getReturnsStoredProgressAnswersForResume() throws Exception {
        Chapter chapter = chapter(10L, 7L);
        ChapterProgress progress = progress(20L, 10L, "IN_PROGRESS");
        List<ProgressAnswerDto> answers =
                List.of(new ProgressAnswerDto(101L, "B", false, 1_200, "UNSURE"));
        progress.setCurrentIndex(1);
        progress.setAnswersJson(objectMapper.writeValueAsString(answers));
        ChapterQuestion chapterQuestion = new ChapterQuestion();
        chapterQuestion.setChapterId(10L);
        chapterQuestion.setQuestionId(101L);
        Question question = question(101L, "A");

        when(chapterRepository.findById(10L)).thenReturn(Optional.of(chapter));
        when(chapterQuestionRepository.findByChapterIdOrderByPositionAsc(10L))
                .thenReturn(List.of(chapterQuestion));
        when(questionRepository.findAllById(List.of(101L))).thenReturn(List.of(question));
        when(questionPayloadMapper.text(question)).thenReturn("Question?");
        when(questionPayloadMapper.options(question)).thenReturn(Map.of("A", "First", "B", "Second"));
        when(questionPayloadMapper.explanation(question)).thenReturn(null);
        when(progressRepository.findByUserIdAndChapterId(20L, 10L)).thenReturn(Optional.of(progress));

        ChapterDetailDto detail = service.get(10L, 20L);

        assertThat(detail.progress().answers()).isEqualTo(answers);
    }

    @Test
    void submitRejectsEmptyAnswers() {
        stubInProgressChapter(List.of(101L, 102L));

        assertThatThrownBy(() -> service.submit(10L, 20L, new SubmitChapterRequest(List.of())))
                .isInstanceOf(ApiException.class)
                .hasMessage("Answers must contain every chapter question exactly once");
    }

    @Test
    void submitCompletesScoresAnswersAndUpsertsRevision() throws Exception {
        Chapter chapter = chapter(10L, 7L);
        ChapterProgress progress = progress(20L, 10L, "IN_PROGRESS");
        Question first = question(101L, "A");
        Question second = question(102L, "B");
        List<ProgressAnswerDto> clientAnswers = List.of(
                new ProgressAnswerDto(101L, "A", false, 1_000, "SURE"),
                new ProgressAnswerDto(102L, "A", true, 2_000, "GUESS"));
        RevisionAlgorithm.Candidate candidate =
                new RevisionAlgorithm.Candidate(102L, "SURE_WRONG", 3, Instant.parse("2026-09-22T00:00:00Z"));
        Revision revision = revision(30L);

        when(chapterRepository.findById(10L)).thenReturn(Optional.of(chapter));
        when(progressRepository.findByUserIdAndChapterId(20L, 10L)).thenReturn(Optional.of(progress));
        when(chapterQuestionRepository.findByChapterIdOrderByPositionAsc(10L))
                .thenReturn(List.of(chapterQuestion(10L, 101L), chapterQuestion(10L, 102L)));
        when(questionRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(first, second));
        when(questionPayloadMapper.isCorrect(first, "A")).thenReturn(true);
        when(questionPayloadMapper.isCorrect(second, "A")).thenReturn(false);
        when(revisionAlgorithm.candidates(any(), any())).thenReturn(List.of(candidate));
        when(revisionRepository.findFirstByUserIdAndSubjectIdAndStatus(20L, 7L, "PENDING"))
                .thenReturn(Optional.empty());
        when(revisionRepository.save(any(Revision.class))).thenReturn(revision);
        when(revisionQuestionRepository.findByRevisionIdAndQuestionId(30L, 102L))
                .thenReturn(Optional.empty());
        when(revisionQuestionRepository.findByRevisionId(30L))
                .thenReturn(List.of(mock(RevisionQuestion.class)));

        ChapterSubmitResponse response =
                service.submit(10L, 20L, new SubmitChapterRequest(clientAnswers));

        assertThat(response.revision().revisionId()).isEqualTo(30L);
        assertThat(progress.getRevisionId()).isEqualTo(30L);
        verify(revisionRepository).findFirstByUserIdAndSubjectIdAndStatus(20L, 7L, "PENDING");
        verify(authService).recordActivity(20L);
    }

    private void stubInProgressChapter(List<Long> questionIds) {
        when(chapterRepository.findById(10L)).thenReturn(Optional.of(chapter(10L, 7L)));
        when(progressRepository.findByUserIdAndChapterId(20L, 10L))
                .thenReturn(Optional.of(progress(20L, 10L, "IN_PROGRESS")));
        when(chapterQuestionRepository.findByChapterIdOrderByPositionAsc(10L))
                .thenReturn(questionIds.stream()
                        .map(questionId -> chapterQuestion(10L, questionId))
                        .toList());
    }

    private static ChapterQuestion chapterQuestion(Long chapterId, Long questionId) {
        ChapterQuestion chapterQuestion = new ChapterQuestion();
        chapterQuestion.setChapterId(chapterId);
        chapterQuestion.setQuestionId(questionId);
        return chapterQuestion;
    }

    private static Chapter chapter(Long id, Long subjectId) {
        Chapter chapter = new Chapter();
        setId(chapter, id);
        chapter.setSubjectId(subjectId);
        chapter.setTitle("Chapter");
        return chapter;
    }

    private static ChapterProgress progress(Long userId, Long chapterId, String status) {
        ChapterProgress progress = new ChapterProgress();
        progress.setUserId(userId);
        progress.setChapterId(chapterId);
        progress.setStatus(status);
        return progress;
    }

    private static Question question(Long id, String correctOption) {
        Question question = new Question();
        setId(question, id);
        question.setCorrectOption(correctOption);
        question.setAllottedTimeMs(60_000);
        return question;
    }

    private static Revision revision(Long id) {
        Revision revision = new Revision();
        setId(revision, id);
        return revision;
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
