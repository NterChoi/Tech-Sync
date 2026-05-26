package com.techsync.repository;

import com.techsync.domain.DraftVersion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DraftVersionRepository extends MongoRepository<DraftVersion, String> {

    List<DraftVersion> findByWorkspaceIdOrderByVersionNoDesc(Long workspaceId);

    Optional<DraftVersion> findFirstByWorkspaceIdOrderByVersionNoDesc(Long workspaceId);

    Optional<DraftVersion> findByWorkspaceIdAndVersionNo(Long workspaceId, Long versionNo);
}
