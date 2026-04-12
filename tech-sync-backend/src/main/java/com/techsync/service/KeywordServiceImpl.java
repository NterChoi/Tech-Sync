package com.techsync.service;

import com.techsync.domain.Keyword;
import com.techsync.domain.KeywordMaster;
import com.techsync.dto.KeywordResponse;
import com.techsync.exception.BusinessException;
import com.techsync.repository.KeywordMasterRepository;
import com.techsync.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordServiceImpl implements KeywordService {

    private final KeywordRepository keywordRepository;
    private final KeywordMasterRepository keywordMasterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<KeywordResponse> getRecommended() {
        return keywordMasterRepository.findAll().stream()
                .map(KeywordResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KeywordResponse> getMyKeywords(Long userId) {
        return keywordRepository.findByUserId(userId).stream()
                .map(KeywordResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void subscribe(Long userId, Long keywordMasterId) {
        KeywordMaster master = keywordMasterRepository.findById(keywordMasterId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 키워드입니다.", HttpStatus.NOT_FOUND));

        if (keywordRepository.existsByUserIdAndKeywordName(userId, master.getKeywordName())) {
            throw new BusinessException("이미 구독 중인 키워드입니다.", HttpStatus.CONFLICT);
        }

        keywordRepository.save(Keyword.builder()
                .userId(userId)
                .keywordName(master.getKeywordName())
                .build());
    }

    @Override
    @Transactional
    public void unsubscribe(Long userId, Long keywordMasterId) {
        KeywordMaster master = keywordMasterRepository.findById(keywordMasterId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 키워드입니다.", HttpStatus.NOT_FOUND));

        Keyword keyword = keywordRepository.findByUserIdAndKeywordName(userId, master.getKeywordName())
                .orElseThrow(() -> new BusinessException("구독하지 않은 키워드입니다.", HttpStatus.NOT_FOUND));

        keywordRepository.delete(keyword);
    }
}
