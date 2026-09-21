package com.quizapp.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quizapp.chapter.Chapter;
import com.quizapp.chapter.ChapterQuestion;
import com.quizapp.chapter.ChapterQuestionRepository;
import com.quizapp.chapter.ChapterRepository;
import com.quizapp.common.util.Futures;
import com.quizapp.config.VirtualThreadConfig;
import com.quizapp.question.Question;
import com.quizapp.question.QuestionRepository;
import com.quizapp.subject.Subject;
import com.quizapp.subject.SubjectRepository;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class McqSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(McqSeedRunner.class);

    private final boolean enabled;
    private final int allottedTimeMs;
    private final ObjectMapper mapper;
    private final SubjectRepository subjects;
    private final QuestionRepository questions;
    private final ChapterRepository chapters;
    private final ChapterQuestionRepository chapterQuestions;
    private final ExecutorService virtualExecutor;

    public McqSeedRunner(
            @Value("${app.seed.enabled:true}") boolean enabled,
            @Value("${app.seed.allotted-time-ms:60000}") int allottedTimeMs,
            ObjectMapper mapper,
            SubjectRepository subjects,
            QuestionRepository questions,
            ChapterRepository chapters,
            ChapterQuestionRepository chapterQuestions,
            @Qualifier(VirtualThreadConfig.VIRTUAL_EXECUTOR) ExecutorService virtualExecutor) {
        this.enabled = enabled;
        this.allottedTimeMs = allottedTimeMs;
        this.mapper = mapper;
        this.subjects = subjects;
        this.questions = questions;
        this.chapters = chapters;
        this.chapterQuestions = chapterQuestions;
        this.virtualExecutor = virtualExecutor;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) return;

        Subject subject = subjects.findByName("Indian History").orElseGet(() -> {
            Subject s = new Subject();
            s.setName("Indian History");
            return subjects.save(s);
        });

        if (questions.countBySubjectId(subject.getId()) == 0) {
            seedQuestions(subject.getId());
        }

        List<Question> bank = questions.findBySubjectId(subject.getId());
        if (bank.isEmpty()) {
            log.warn("No questions to build chapter");
            return;
        }

        if (!chapters.existsBySubjectIdAndTitle(subject.getId(), "Full practice set")) {
            Chapter chapter = new Chapter();
            chapter.setSubjectId(subject.getId());
            chapter.setTitle("Full practice set");
            chapter.setSortOrder(0);
            chapter.setStatus("ACTIVE");
            chapters.save(chapter);

            List<ChapterQuestion> links = java.util.stream.IntStream.range(0, bank.size())
                    .mapToObj(position -> {
                        ChapterQuestion link = new ChapterQuestion();
                        link.setChapterId(chapter.getId());
                        link.setQuestionId(bank.get(position).getId());
                        link.setPosition(position);
                        return link;
                    })
                    .toList();
            chapterQuestions.saveAll(links);
            log.info("Seeded chapter id={} with {} questions", chapter.getId(), links.size());
        }
    }

    private void seedQuestions(Long subjectId) throws Exception {
        ClassPathResource resource = new ClassPathResource("data/mcqs.json");
        try (InputStream in = resource.getInputStream()) {
            JsonNode root = mapper.readTree(in);
            List<JsonNode> items = StreamSupport.stream(root.path("mcqs").spliterator(), false).toList();

            // Map JSON → entities concurrently on virtual threads
            List<CompletableFuture<Question>> jobs = items.stream()
                    .map(item -> Futures.supply(() -> toQuestion(subjectId, item), virtualExecutor))
                    .toList();
            List<Question> all = Futures.joinAll(jobs);

            final int batchSize = 100;
            for (int i = 0; i < all.size(); i += batchSize) {
                questions.saveAll(all.subList(i, Math.min(i + batchSize, all.size())));
            }
            log.info("Seeded {} questions for subject {}", questions.countBySubjectId(subjectId), subjectId);
        }
    }

    private Question toQuestion(Long subjectId, JsonNode item) {
        try {
            ObjectNode payload = mapper.createObjectNode();
            payload.put("question", item.path("question").asText());
            payload.set("options", item.path("options"));
            if (item.has("explanation")) payload.put("explanation", item.path("explanation").asText());
            if (item.has("source")) payload.put("source", item.path("source").asText());
            if (item.has("book_number")) payload.put("bookNumber", item.path("book_number").asInt());

            Question q = new Question();
            q.setSubjectId(subjectId);
            q.setCorrectOption(item.path("answer").asText("").toLowerCase());
            q.setAllottedTimeMs(allottedTimeMs);
            q.setQuestionJson(mapper.writeValueAsString(payload));
            return q;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to map MCQ", e);
        }
    }
}
