package com.techsync.repository;

import com.techsync.domain.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    List<Keyword> findByUserId(Long userId);

    Optional<Keyword> findByUserIdAndKeywordName(Long userId, String keywordName);

    boolean existsByUserIdAndKeywordName(Long userId, String keywordName);
}
