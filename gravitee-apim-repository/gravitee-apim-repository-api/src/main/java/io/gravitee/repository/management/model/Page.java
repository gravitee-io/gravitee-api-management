/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.management.model;

import java.util.*;
import lombok.*;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Page {

    public enum AuditEvent implements Audit.ApiAuditEvent {
        PAGE_CREATED,
        PAGE_UPDATED,
        PAGE_DELETED,
        PAGE_PUBLISHED,
    }

    private String id;

    /**
     * The page crossId uniquely identifies a page across environments.
     * Pages promoted between environments will share the same crossId.
     */
    private String crossId;

    private String referenceId;
    private PageReferenceType referenceType;
    private String name;
    private String type;
    private String content;
    private String lastContributor;
    private int order;
    private boolean published;
    private String visibility;
    private PageSource source;
    private Map<String, String> configuration;
    private boolean homepage;
    private Date createdAt;
    private Date updatedAt;
    private String parentId;
    private boolean excludedAccessControls;
    private Set<AccessControl> accessControls;
    private Map<String, String> metadata;
    private Boolean useAutoFetch; // use Boolean to avoid default value of primitive type
    private List<PageMedia> attachedMedia;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return Objects.equals(id, page.id) && Objects.equals(referenceId, page.referenceId) && referenceType == page.referenceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, referenceId, referenceType);
    }

    @Override
    public String toString() {
        return (
            "Page{" +
            "id='" +
            id +
            '\'' +
            ", crossId='" +
            crossId +
            '\'' +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", referenceType=" +
            referenceType +
            ", name='" +
            name +
            '\'' +
            ", type='" +
            type +
            '\'' +
            ", content='" +
            content +
            '\'' +
            ", lastContributor='" +
            lastContributor +
            '\'' +
            ", order=" +
            order +
            ", published=" +
            published +
            ", visibility='" +
            visibility +
            '\'' +
            ", source=" +
            source +
            ", configuration=" +
            configuration +
            ", homepage=" +
            homepage +
            ", createdAt=" +
            createdAt +
            ", updatedAt=" +
            updatedAt +
            ", parentId='" +
            parentId +
            '\'' +
            ", excludedAccessControls=" +
            excludedAccessControls +
            ", accessControls=" +
            accessControls +
            ", metadata=" +
            metadata +
            ", useAutoFetch=" +
            useAutoFetch +
            ", attachedMedia=" +
            attachedMedia +
            '}'
        );
    }
}
