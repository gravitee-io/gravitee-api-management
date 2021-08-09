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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @author Titouan COMPIEGNE
 * @author Guillaume GILLON
 */
public class UpdatePageEntity extends FetchablePageEntity {

    @NotNull
    @Size(min = 1)
    private String name;

    private String lastContributor;

    private Integer order;

    private Boolean published;

    private Visibility visibility;

    private PageSourceEntity source;

    private Map<String, String> configuration;

    private Boolean homepage;

    @JsonProperty("excluded_groups")
    private List<String> excludedGroups;

    @JsonProperty("attached_media")
    private List<PageMediaEntity> attachedMedia;

    private String parentId;

    private Boolean excludedAccessControls;

    private Set<AccessControlEntity> accessControls;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastContributor() {
        return lastContributor;
    }

    public void setLastContributor(String lastContributor) {
        this.lastContributor = lastContributor;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Boolean isPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
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

    public Boolean isHomepage() {
        return homepage;
    }

    public void setHomepage(Boolean homepage) {
        this.homepage = homepage;
    }

    public List<String> getExcludedGroups() {
        return excludedGroups;
    }

    public void setExcludedGroups(List<String> excludedGroups) {
        this.excludedGroups = excludedGroups;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
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

    public void setExcludedAccessControls(Boolean excludedAccessControls) {
        this.excludedAccessControls = excludedAccessControls;
    }

    public Boolean isExcludedAccessControls() {
        return excludedAccessControls;
    }

    public Set<AccessControlEntity> getAccessControls() {
        return accessControls;
    }

    public void setAccessControls(Set<AccessControlEntity> accessControls) {
        this.accessControls = accessControls;
    }

    public static UpdatePageEntity from(PageEntity pageEntity) {
        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setConfiguration(pageEntity.getConfiguration());
        updatePageEntity.setContent(pageEntity.getContent());
        updatePageEntity.setExcludedAccessControls(pageEntity.isExcludedAccessControls());
        updatePageEntity.setAccessControls(pageEntity.getAccessControls());
        updatePageEntity.setHomepage(pageEntity.isHomepage());
        updatePageEntity.setLastContributor(pageEntity.getLastContributor());
        updatePageEntity.setName(pageEntity.getName());
        updatePageEntity.setOrder(pageEntity.getOrder());
        updatePageEntity.setParentId(pageEntity.getParentId());
        updatePageEntity.setPublished(pageEntity.isPublished());
        updatePageEntity.setSource(pageEntity.getSource());
        updatePageEntity.setAttachedMedia(pageEntity.getAttachedMedia());

        return updatePageEntity;
    }
}
