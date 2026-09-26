package com.quizapp.chapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quizapp.chapter.dto.ChapterDetailDto;
import com.quizapp.chapter.dto.SubjectChaptersResponse;
import com.quizapp.chapter.dto.ChapterSubmitResponse;
import com.quizapp.chapter.dto.ProgressRequest;
import com.quizapp.chapter.dto.SubmitChapterRequest;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

class ChapterControllerTest {

    private final ChapterService service = mock(ChapterService.class);
    private final ChapterController controller = new ChapterController(service);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exposesChapterApiAndDelegatesWithCurrentUser() throws Exception {
        authenticate(42L);
        SubjectChaptersResponse chapters = new SubjectChaptersResponse(null, 0, List.of());
        ChapterDetailDto detail = mock(ChapterDetailDto.class);
        ProgressRequest progress = mock(ProgressRequest.class);
        SubmitChapterRequest submit = mock(SubmitChapterRequest.class);
        ChapterSubmitResponse response = mock(ChapterSubmitResponse.class);
        when(service.listBySubject(7L, 42L)).thenReturn(chapters);
        when(service.get(9L, 42L)).thenReturn(detail);
        when(service.submit(9L, 42L, submit)).thenReturn(response);

        assertThat(controller.listBySubject(7L)).isSameAs(chapters);
        assertThat(controller.get(9L)).isSameAs(detail);
        controller.start(9L, true);
        controller.saveProgress(9L, progress);
        assertThat(controller.submit(9L, submit)).isSameAs(response);

        verify(service).start(9L, 42L, true);
        verify(service).saveProgress(9L, 42L, progress);
        assertMapping("listBySubject", GetMapping.class, "/subjects/{id}/chapters");
        assertMapping("get", GetMapping.class, "/chapters/{id}");
        assertMapping("start", PostMapping.class, "/chapters/{id}/start");
        assertMapping("saveProgress", PutMapping.class, "/chapters/{id}/progress");
        assertMapping("submit", PostMapping.class, "/chapters/{id}/submit");
    }

    private static void authenticate(Long userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private static void assertMapping(
            String methodName, Class<? extends java.lang.annotation.Annotation> annotation, String path)
            throws Exception {
        Method method = List.of(ChapterController.class.getDeclaredMethods()).stream()
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        String[] values;
        if (annotation == GetMapping.class) {
            values = method.getAnnotation(GetMapping.class).value();
        } else if (annotation == PostMapping.class) {
            values = method.getAnnotation(PostMapping.class).value();
        } else {
            values = method.getAnnotation(PutMapping.class).value();
        }
        assertThat(values).containsExactly(path);
    }
}
