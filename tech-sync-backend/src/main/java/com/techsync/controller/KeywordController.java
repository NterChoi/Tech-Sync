package com.techsync.controller;

import com.techsync.domain.KeywordMaster;
import com.techsync.dto.ApiResponse;
import com.techsync.repository.KeywordMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
public class KeywordController {

    private final KeywordMasterRepository keywordMasterRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<KeywordMaster>>> getKeywords() {
        List<KeywordMaster> keywords = keywordMasterRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(keywords));
    }
}
