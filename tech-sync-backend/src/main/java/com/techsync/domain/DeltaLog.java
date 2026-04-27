package com.techsync.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "DELTA_LOG")
@CompoundIndexes({
        @CompoundIndex(name = "IDX_WORKSPACE_SEQ",
                def = "{'WORKSPACE_ID': 1, 'SEQ_NO': 1}",
                unique = true)
})
@Getter
@NoArgsConstructor
public class DeltaLog {

    @Id
    private String id;

    @Field("WORKSPACE_ID")
    private Long workspaceId;

    @Field("SEQ_NO")
    private Long seqNo;

    @Field("USER_ID")
    private Long userId;

    @Field("OPS")
    private List<Map<String, Object>> ops;

    @Field("CREATED_AT")
    private LocalDateTime createdAt;

    @Builder
    public DeltaLog(Long workspaceId, Long seqNo, Long userId,
                    List<Map<String, Object>> ops, LocalDateTime createdAt) {
        this.workspaceId = workspaceId;
        this.seqNo = seqNo;
        this.userId = userId;
        this.ops = ops;
        this.createdAt = createdAt;
    }
}
