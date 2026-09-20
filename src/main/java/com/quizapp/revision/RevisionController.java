package com.quizapp.revision;

import com.quizapp.attempt.dto.CheckAnswerRequest;
import com.quizapp.attempt.dto.CheckAnswerResponse;
import com.quizapp.common.util.AuthContext;
import com.quizapp.revision.dto.RevisionDetailDto;
import com.quizapp.revision.dto.RevisionListItemDto;
import com.quizapp.revision.dto.SubmitRevisionRequest;
import com.quizapp.revision.dto.SubmitRevisionResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/revisions")
public class RevisionController {

    private final RevisionService revisionService;

    public RevisionController(RevisionService revisionService) {
        this.revisionService = revisionService;
    }

    @GetMapping
    public List<RevisionListItemDto> list() {
        return revisionService.listOpen(AuthContext.currentUserId());
    }

    @GetMapping("/{id}")
    public RevisionDetailDto get(@PathVariable Long id) {
        return revisionService.getDueQuestions(id, AuthContext.currentUserId());
    }

    @PostMapping("/{id}/check")
    public CheckAnswerResponse check(
            @PathVariable Long id, @Valid @RequestBody CheckAnswerRequest request) {
        return revisionService.check(id, AuthContext.currentUserId(), request);
    }

    @PostMapping("/{id}/submit")
    public SubmitRevisionResponse submit(
            @PathVariable Long id, @Valid @RequestBody SubmitRevisionRequest request) {
        return revisionService.submit(id, AuthContext.currentUserId(), request.answers());
    }
}
