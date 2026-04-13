package com.techsync.repository;

import com.techsync.domain.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    boolean existsByUserIdAndArticleId(Long userId, String articleId);

    Optional<Scrap> findByUserIdAndArticleId(Long userId, String articleId);

    @Query("SELECT s.articleId FROM Scrap s WHERE s.userId = :userId")
    List<String> findArticleIdByUserId(Long userId);
}
