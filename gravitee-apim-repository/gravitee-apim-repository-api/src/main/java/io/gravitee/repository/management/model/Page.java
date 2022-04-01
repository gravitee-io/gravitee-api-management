/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.management.model;

import java.util.*;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public PageReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(PageReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLastContributor() {
        return lastContributor;
    }

    public void setLastContributor(String lastContributor) {
        this.lastContributor = lastContributor;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public PageSource getSource() {
        return source;
    }

    public void setSource(PageSource source) {
        this.source = source;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    public boolean isHomepage() {
        return homepage;
    }

    public void setHomepage(boolean homepage) {
        this.homepage = homepage;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Boolean getUseAutoFetch() {
        return useAutoFetch;
    }

    public void setUseAutoFetch(Boolean useAutoFetch) {
        this.useAutoFetch = useAutoFetch;
    }

    public List<PageMedia> getAttachedMedia() {
        return attachedMedia;
    }

    public void setAttachedMedia(List<PageMedia> attachedMedia) {
        this.attachedMedia = attachedMedia;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public void setExcludedAccessControls(boolean excludedAccessControls) {
        this.excludedAccessControls = excludedAccessControls;
    }

    public boolean isExcludedAccessControls() {
        return excludedAccessControls;
    }

    public Set<AccessControl> getAccessControls() {
        return accessControls;
    }

    public void setAccessControls(Set<AccessControl> accessControls) {
        this.accessControls = accessControls;
    }

    public String getCrossId() {
        return crossId;
    }

    public void setCrossId(String crossId) {
        this.crossId = crossId;
    }

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
