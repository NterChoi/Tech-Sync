package com.techsync.repository;

import com.techsync.domain.Alarm;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AlarmRepository extends MongoRepository<Alarm, String> {

    List<Alarm> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);
}
