package com.quizapp.subject;

import com.quizapp.common.ApiException;
import com.quizapp.subject.dto.SubjectDto;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Transactional(readOnly = true)
    public List<SubjectDto> listAll() {
        return subjectRepository.findAll().stream()
                .map(s -> new SubjectDto(s.getId(), s.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Subject requireById(Long id) {
        return subjectRepository
                .findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));
    }
}
