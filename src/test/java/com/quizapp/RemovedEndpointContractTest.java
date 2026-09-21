package com.quizapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.quizapp.attempt.AttemptController;
import com.quizapp.quiz.QuizController;
import com.quizapp.revision.RevisionController;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

class RemovedEndpointContractTest {

    @Test
    void legacyQuizAndCheckEndpointsAreNotMapped() {
        assertThat(paths(QuizController.class))
                .doesNotContain(
                        "/subjects/{id}/quizzes",
                        "/quizzes/{id}",
                        "/quizzes/{id}/start",
                        "/quizzes/{id}/attempts");
        assertThat(paths(AttemptController.class)).doesNotContain("/attempts/{id}/check");
        assertThat(paths(RevisionController.class)).doesNotContain("/{id}/check");
    }

    private static String[] paths(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .flatMap(RemovedEndpointContractTest::paths)
                .toArray(String[]::new);
    }

    private static Stream<String> paths(Method method) {
        GetMapping get = method.getAnnotation(GetMapping.class);
        PostMapping post = method.getAnnotation(PostMapping.class);
        if (get != null) return Arrays.stream(get.value());
        if (post != null) return Arrays.stream(post.value());
        return Stream.empty();
    }
}
