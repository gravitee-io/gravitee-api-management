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
package io.gravitee.repository.mongodb.management.internal.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "pages")
public class PageMongo extends Auditable {

    @Id
    private String id;

    /**
     * The page crossId uniquely identifies a page across environments.
     * Pages promoted between environments will share the same crossId.
     */
    private String crossId;

    private String referenceId;
    private String referenceType;
    private String name;
    private String type;
    private String title;
    private String content;
    private String lastContributor;
    private int order;
    private boolean published;
    private String visibility;
    private PageSourceMongo source;
    private Map<String, String> configuration;
    private boolean homepage;
    private boolean excludedAccessControls;
    private Set<AccessControlMongo> accessControls;
    private List<PageMediaMongo> attachedMedia;
    private String parentId;
    private Map<String, String> metadata;
    private Boolean useAutoFetch; // use Boolean to avoid default value of primitive type

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

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public PageSourceMongo getSource() {
        return source;
    }

    public void setSource(PageSourceMongo source) {
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

    public boolean isExcludedAccessControls() {
        return excludedAccessControls;
    }

    public void setExcludedAccessControls(boolean excludedAccessControls) {
        this.excludedAccessControls = excludedAccessControls;
    }

    public Set<AccessControlMongo> getAccessControls() {
        return accessControls;
    }

    public void setAccessControls(Set<AccessControlMongo> accessControls) {
        this.accessControls = accessControls;
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

    public List<PageMediaMongo> getAttachedMedia() {
        return attachedMedia;
    }

    public void setAttachedMedia(List<PageMediaMongo> attachedMedia) {
        this.attachedMedia = attachedMedia;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
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
        if (!(o instanceof PageMongo)) return false;
        PageMongo pageMongo = (PageMongo) o;
        return Objects.equals(id, pageMongo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "PageMongo{" +
            "id='" +
            id +
            '\'' +
            ", crossId='" +
            crossId +
            '\'' +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", referenceType='" +
            referenceType +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", type='" +
            type +
            '\'' +
            ", title='" +
            title +
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
            ", visibility=" +
            visibility +
            ", source=" +
            source +
            ", configuration=" +
            configuration +
            ", homepage=" +
            homepage +
            ", excludedAccessControls=" +
            excludedAccessControls +
            ", accessControls=" +
            accessControls +
            ", attachedMedia=" +
            attachedMedia +
            ", parentId='" +
            parentId +
            '\'' +
            ", metadata=" +
            metadata +
            ", useAutoFetch=" +
            useAutoFetch +
            "} " +
            super.toString()
        );
    }
}
