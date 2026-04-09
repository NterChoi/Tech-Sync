package com.techsync.repository;

import com.techsync.domain.KeywordMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KeywordMasterRepository extends JpaRepository<KeywordMaster, Long> {

    List<KeywordMaster> findByCategory(String category);
}
