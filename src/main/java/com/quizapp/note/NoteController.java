package com.quizapp.note;

import com.quizapp.common.util.AuthContext;
import com.quizapp.note.dto.NoteDto;
import com.quizapp.note.dto.UpsertNoteRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions/{questionId}/note")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public NoteDto get(@PathVariable Long questionId) {
        return noteService.get(AuthContext.currentUserId(), questionId);
    }

    @PutMapping
    public NoteDto upsert(
            @PathVariable Long questionId, @Valid @RequestBody UpsertNoteRequest request) {
        return noteService.upsert(
                AuthContext.currentUserId(), questionId, request.noteDescription());
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Long questionId) {
        noteService.delete(AuthContext.currentUserId(), questionId);
        return ResponseEntity.noContent().build();
    }
}
