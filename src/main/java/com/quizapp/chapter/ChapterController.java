package com.quizapp.chapter;

import com.quizapp.chapter.dto.ChapterDetailDto;
import com.quizapp.chapter.dto.ChapterListItemDto;
import com.quizapp.chapter.dto.ChapterSubmitResponse;
import com.quizapp.chapter.dto.ProgressRequest;
import com.quizapp.chapter.dto.SubmitChapterRequest;
import com.quizapp.common.util.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping("/subjects/{id}/chapters")
    public List<ChapterListItemDto> listBySubject(@PathVariable Long id) {
        return chapterService.listBySubject(id, AuthContext.currentUserId());
    }

    @GetMapping("/chapters/{id}")
    public ChapterDetailDto get(@PathVariable Long id) {
        return chapterService.get(id, AuthContext.currentUserId());
    }

    @PostMapping("/chapters/{id}/start")
    public void start(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean restart) {
        chapterService.start(id, AuthContext.currentUserId(), restart);
    }

    @PutMapping("/chapters/{id}/progress")
    public void saveProgress(@PathVariable Long id, @Valid @RequestBody ProgressRequest request) {
        chapterService.saveProgress(id, AuthContext.currentUserId(), request);
    }

    @PostMapping("/chapters/{id}/submit")
    public ChapterSubmitResponse submit(
            @PathVariable Long id, @Valid @RequestBody SubmitChapterRequest request) {
        return chapterService.submit(id, AuthContext.currentUserId(), request);
    }
}
