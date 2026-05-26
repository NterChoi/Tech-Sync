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

@Document(collection = "DRAFT_VERSION")
@CompoundIndexes({
        @CompoundIndex(name = "IDX_WORKSPACE_VERSION",
                def = "{'WORKSPACE_ID': 1, 'VERSION_NO': -1}")
})
@Getter
@NoArgsConstructor
public class DraftVersion {

    @Id
    private String id;

    @Field("WORKSPACE_ID")
    private Long workspaceId;

    @Field("VERSION_NO")
    private Long versionNo;

    @Field("VERSION_TYPE")
    private VersionType versionType;

    @Field("CONTENT")
    private List<Map<String, Object>> content;

    @Field("CREATED_BY")
    private Long createdBy;

    @Field("CREATED_AT")
    private LocalDateTime createdAt;

    public enum VersionType {
        MAJOR, MINOR
    }

    @Builder
    public DraftVersion(Long workspaceId, Long versionNo, VersionType versionType,
                        List<Map<String, Object>> content, Long createdBy,
                        LocalDateTime createdAt) {
        this.workspaceId = workspaceId;
        this.versionNo = versionNo;
        this.versionType = versionType;
        this.content = content;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
