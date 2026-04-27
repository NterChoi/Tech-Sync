package com.techsync.repository;

import com.techsync.domain.DeltaLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DeltaLogRepository extends MongoRepository<DeltaLog, String> {

    List<DeltaLog> findByWorkspaceIdAndSeqNoGreaterThanOrderBySeqNoAsc(
            Long workspaceId, Long seqNo);
}
