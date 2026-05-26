package com.techsync.repository;

import com.techsync.domain.DraftSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DraftSnapshotRepository extends MongoRepository<DraftSnapshot, String> {

    Optional<DraftSnapshot> findByWorkspaceId(Long workspaceId);
}
