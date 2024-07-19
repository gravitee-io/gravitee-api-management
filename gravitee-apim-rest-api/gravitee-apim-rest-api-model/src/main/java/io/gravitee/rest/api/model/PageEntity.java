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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.model.search.Indexable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.With;

/**
 * @author Titouan COMPIEGNE
 * @author Guillaume GILLON
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@With
public class PageEntity implements Indexable {

    private String id;

    /**
     * The page crossId uniquely identifies a page across environments.
     * Pages promoted between environments will share the same crossId.
     */
    private String crossId;

    private String name;
    private String type;
    private String content;
    private int order;
    private String lastContributor;
    private boolean published;
    private Visibility visibility;
    private Date lastModificationDate;
    private String contentType;
    private PageSourceEntity source;
    private Map<String, String> configuration;
    private boolean homepage;
    private String parentId;
    private String parentPath;

    @JsonProperty("excluded_groups")
    private List<String> excludedGroups;

    private boolean excludedAccessControls;
    private Set<AccessControlEntity> accessControls;
    private List<String> messages;

    @JsonProperty("attached_media")
    private List<PageMediaEntity> attachedMedia;

    private Map<String, String> metadata;
    private List<PageEntity> translations;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean generalConditions;

    /**
     * revision Id of the page used to fill the content attributes
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private PageRevisionId contentRevisionId;

    @JsonIgnore
    private String referenceType;

    @JsonIgnore
    private String referenceId;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getReferenceType() {
        return this.referenceType;
    }

    @Override
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public String getReferenceId() {
        return referenceId;
    }

    @Override
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    @JsonIgnore
    public boolean isRoot() {
        return PageType.ROOT.name().equals(type);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getLastContributor() {
        return lastContributor;
    }

    public void setLastContributor(String lastContributor) {
        this.lastContributor = lastContributor;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Date getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(Date lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public PageSourceEntity getSource() {
        return source;
    }

    public void setSource(PageSourceEntity source) {
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

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public List<PageEntity> getTranslations() {
        return translations;
    }

    public void setTranslations(List<PageEntity> translations) {
        this.translations = translations;
    }

    public Boolean isGeneralConditions() {
        return generalConditions;
    }

    public void setGeneralConditions(boolean generalConditions) {
        this.generalConditions = generalConditions;
    }

    public PageRevisionId getContentRevisionId() {
        return contentRevisionId;
    }

    public void setContentRevisionId(PageRevisionId contentRevisionId) {
        this.contentRevisionId = contentRevisionId;
    }

    public List<PageMediaEntity> getAttachedMedia() {
        return attachedMedia;
    }

    public void setAttachedMedia(List<PageMediaEntity> attachedMedia) {
        this.attachedMedia = attachedMedia;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public List<String> getExcludedGroups() {
        return excludedGroups;
    }

    public void setExcludedGroups(List<String> excludedGroups) {
        this.excludedGroups = excludedGroups;
    }

    public void setExcludedAccessControls(boolean excludedAccessControls) {
        this.excludedAccessControls = excludedAccessControls;
    }

    public boolean isExcludedAccessControls() {
        return excludedAccessControls;
    }

    public Set<AccessControlEntity> getAccessControls() {
        return accessControls;
    }

    public void setAccessControls(Set<AccessControlEntity> accessControls) {
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
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageEntity)) {
            return false;
        }
        PageEntity that = (PageEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "PageEntity{" +
            "id='" +
            id +
            '\'' +
            ", crossId='" +
            crossId +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", type='" +
            type +
            '\'' +
            ", content='" +
            content +
            '\'' +
            ", order=" +
            order +
            ", lastContributor='" +
            lastContributor +
            '\'' +
            ", published=" +
            published +
            ", visibility=" +
            visibility +
            ", lastModificationDate=" +
            lastModificationDate +
            ", contentType='" +
            contentType +
            '\'' +
            ", source=" +
            source +
            ", configuration=" +
            configuration +
            ", homepage=" +
            homepage +
            ", parentId='" +
            parentId +
            '\'' +
            ", parentPath='" +
            parentPath +
            '\'' +
            ", excludedGroups=" +
            excludedGroups +
            ", excludedAccessControls='" +
            excludedAccessControls +
            '\'' +
            ", accessControls='" +
            accessControls +
            '\'' +
            ", attachedMedia=" +
            attachedMedia +
            ", metadata='" +
            metadata +
            '\'' +
            ", translations='" +
            translations +
            '\'' +
            ", generalConditions='" +
            generalConditions +
            '\'' +
            ", contentRevisionId='" +
            contentRevisionId +
            '\'' +
            '}'
        );
    }

    public static class PageRevisionId {

        private final String pageId;
        private final int revision;

        public PageRevisionId(String pageId, int revision) {
            this.pageId = pageId;
            this.revision = revision;
        }

        public String getPageId() {
            return pageId;
        }

        public int getRevision() {
            return revision;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PageRevisionId that = (PageRevisionId) o;
            return revision == that.revision && Objects.equals(pageId, that.pageId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pageId, revision);
        }

        @Override
        public String toString() {
            return "PageRevisionId{" + "pageId='" + pageId + '\'' + ", revision=" + revision + '}';
        }
    }
}
