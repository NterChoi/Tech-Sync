package com.techsync.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "DRAFT_SNAPSHOT")
@Getter
@NoArgsConstructor
public class DraftSnapshot {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("WORKSPACE_ID")
    private Long workspaceId;

    @Field("CONTENT")
    private List<Map<String, Object>> content;

    @Field("LAST_EDITOR_ID")
    private Long lastEditorId;

    @Field("UPDATED_AT")
    private LocalDateTime updatedAt;

    @Builder
    public DraftSnapshot(Long workspaceId, List<Map<String, Object>> content,
                         Long lastEditorId, LocalDateTime updatedAt) {
        this.workspaceId = workspaceId;
        this.content = content;
        this.lastEditorId = lastEditorId;
        this.updatedAt = updatedAt;
    }

    public void updateContent(List<Map<String, Object>> content, Long editorId) {
        this.content = content;
        this.lastEditorId = editorId;
        this.updatedAt = LocalDateTime.now();
    }
}
