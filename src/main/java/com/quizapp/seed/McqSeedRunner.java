package com.quizapp.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quizapp.chapter.Chapter;
import com.quizapp.chapter.ChapterQuestion;
import com.quizapp.chapter.ChapterQuestionRepository;
import com.quizapp.chapter.ChapterRepository;
import com.quizapp.common.util.Futures;
import com.quizapp.common.util.QuestionPayloadMapper;
import com.quizapp.config.VirtualThreadConfig;
import com.quizapp.question.Question;
import com.quizapp.question.QuestionRepository;
import com.quizapp.subject.Subject;
import com.quizapp.subject.SubjectRepository;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final String LEGACY_FULL_SET = "Full practice set";

    private final boolean enabled;
    private final int allottedTimeMs;
    private final ObjectMapper mapper;
    private final SubjectRepository subjects;
    private final QuestionRepository questions;
    private final ChapterRepository chapters;
    private final ChapterQuestionRepository chapterQuestions;
    private final QuestionPayloadMapper questionPayloadMapper;
    private final ExecutorService virtualExecutor;

    public McqSeedRunner(
            @Value("${app.seed.enabled:true}") boolean enabled,
            @Value("${app.seed.allotted-time-ms:60000}") int allottedTimeMs,
            ObjectMapper mapper,
            SubjectRepository subjects,
            QuestionRepository questions,
            ChapterRepository chapters,
            ChapterQuestionRepository chapterQuestions,
            QuestionPayloadMapper questionPayloadMapper,
            @Qualifier(VirtualThreadConfig.VIRTUAL_EXECUTOR) ExecutorService virtualExecutor) {
        this.enabled = enabled;
        this.allottedTimeMs = allottedTimeMs;
        this.mapper = mapper;
        this.subjects = subjects;
        this.questions = questions;
        this.chapters = chapters;
        this.chapterQuestions = chapterQuestions;
        this.questionPayloadMapper = questionPayloadMapper;
        this.virtualExecutor = virtualExecutor;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            return;
        }

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
            log.warn("No questions to build chapters");
            return;
        }

        boolean hasLegacy = chapters.existsBySubjectIdAndTitle(subject.getId(), LEGACY_FULL_SET);
        long topicChapters = chapters.countBySubjectIdAndTitleNot(subject.getId(), LEGACY_FULL_SET);
        if (hasLegacy || topicChapters == 0) {
            rebuildTopicChapters(subject.getId(), bank);
        }
    }

    private void rebuildTopicChapters(Long subjectId, List<Question> bank) throws Exception {
        ClassPathResource resource = new ClassPathResource("data/mcqs.json");
        JsonNode root;
        try (InputStream in = resource.getInputStream()) {
            root = mapper.readTree(in);
        }

        List<String> chapterTitles = orderedChapterTitles(root);
        if (chapterTitles.isEmpty()) {
            log.warn("mcqs.json has no chapter metadata; skipping chapter rebuild");
            return;
        }

        List<Chapter> existing = chapters.findBySubjectId(subjectId);
        for (Chapter chapter : existing) {
            chapterQuestions.deleteByChapterId(chapter.getId());
        }
        if (!existing.isEmpty()) {
            chapters.deleteAll(existing);
        }

        Map<String, Question> byText = new LinkedHashMap<>();
        for (Question question : bank) {
            byText.putIfAbsent(normalize(questionPayloadMapper.text(question)), question);
        }

        Map<String, List<Long>> questionIdsByChapter = new LinkedHashMap<>();
        for (String title : chapterTitles) {
            questionIdsByChapter.put(title, new ArrayList<>());
        }

        for (JsonNode item : root.path("mcqs")) {
            String title = item.path("chapter").asText("").trim();
            if (title.isEmpty()) {
                continue;
            }
            questionIdsByChapter.computeIfAbsent(title, ignored -> new ArrayList<>());
            Question matched = byText.get(normalize(item.path("question").asText("")));
            if (matched == null) {
                log.warn("No DB question matched for chapter '{}'", title);
                continue;
            }
            questionIdsByChapter.get(title).add(matched.getId());
        }

        int sort = 0;
        int linked = 0;
        for (String title : chapterTitles) {
            List<Long> ids = questionIdsByChapter.getOrDefault(title, List.of());
            if (ids.isEmpty()) {
                log.warn("Skipping empty chapter '{}'", title);
                continue;
            }
            Chapter chapter = new Chapter();
            chapter.setSubjectId(subjectId);
            chapter.setTitle(title);
            chapter.setSortOrder(sort++);
            chapter.setStatus("ACTIVE");
            chapters.save(chapter);

            List<ChapterQuestion> links = new ArrayList<>(ids.size());
            for (int i = 0; i < ids.size(); i++) {
                ChapterQuestion link = new ChapterQuestion();
                link.setChapterId(chapter.getId());
                link.setQuestionId(ids.get(i));
                link.setPosition(i);
                links.add(link);
            }
            chapterQuestions.saveAll(links);
            linked += links.size();
            log.info("Seeded chapter '{}' with {} questions", title, links.size());
        }
        log.info("Rebuilt {} topic chapters ({} question links) for subject {}", sort, linked, subjectId);
    }

    private List<String> orderedChapterTitles(JsonNode root) {
        Set<String> titles = new LinkedHashSet<>();
        for (JsonNode chapter : root.path("chapters")) {
            String title = chapter.path("title").asText("").trim();
            if (!title.isEmpty()) {
                titles.add(title);
            }
        }
        if (titles.isEmpty()) {
            for (JsonNode item : root.path("mcqs")) {
                String title = item.path("chapter").asText("").trim();
                if (!title.isEmpty()) {
                    titles.add(title);
                }
            }
        }
        return List.copyOf(titles);
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\u00ad\\u200b]", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private void seedQuestions(Long subjectId) throws Exception {
        ClassPathResource resource = new ClassPathResource("data/mcqs.json");
        try (InputStream in = resource.getInputStream()) {
            JsonNode root = mapper.readTree(in);
            List<JsonNode> items = StreamSupport.stream(root.path("mcqs").spliterator(), false).toList();

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
            if (item.has("explanation")) {
                payload.put("explanation", item.path("explanation").asText());
            }
            if (item.has("source")) {
                payload.put("source", item.path("source").asText());
            }
            if (item.has("book_number")) {
                payload.put("bookNumber", item.path("book_number").asInt());
            }
            if (item.has("chapter")) {
                payload.put("chapter", item.path("chapter").asText());
            }

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
