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
package io.gravitee.repository.management.api.search;

import io.gravitee.repository.management.model.Visibility;

import java.util.Objects;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageCriteria {
    private String referenceId;
    private String referenceType;
    private String name;
    private String type;
    private Boolean homepage;
    private Boolean published;
    private String visibility;
    private String parent;
    private Boolean rootParent;
    private Boolean useAutoFetch;

    private PageCriteria() {}

    public String getReferenceId() {
        return referenceId;
    }
    public String getName() {
        return name;
    }
    public String getType() {
        return type;
    }
    public Boolean getHomepage() {
        return homepage;
    }
    public Boolean getPublished() {
        return published;
    }
    public String getParent() {
        return parent;
    }
    public Boolean getRootParent() {
        return rootParent;
    }
    public Boolean getUseAutoFetch() { return useAutoFetch; }

    public String getReferenceType() {
        return referenceType;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
    private void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
    private void setName(String name) {
        this.name = name;
    }
    private void setType(String type) {
        this.type = type;
    }
    private void setHomepage(Boolean homepage) {
        this.homepage = homepage;
    }
    private void setPublished(Boolean published) {
        this.published = published;
    }
    private void setParent(String parent) {
        this.parent = parent;
    }

    public void setRootParent(Boolean rootParent) {
        this.rootParent = rootParent;
    }

    public void setUseAutoFetch(Boolean useAutoFetch) { this.useAutoFetch = useAutoFetch; }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageCriteria that = (PageCriteria) o;
        return Objects.equals(referenceId, that.referenceId) &&
            Objects.equals(referenceType, that.referenceType) &&
            Objects.equals(name, that.name) &&
            Objects.equals(type, that.type) &&
            Objects.equals(homepage, that.homepage) &&
            Objects.equals(published, that.published) &&
            Objects.equals(visibility, that.visibility) &&
            Objects.equals(parent, that.parent) &&
            Objects.equals(rootParent, that.rootParent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceId, referenceType, name, type, homepage, published, visibility, parent, rootParent);
    }

    public static class Builder {

        private PageCriteria query;

        public Builder() {
            this.query = new PageCriteria();
        }

        public PageCriteria build() {
            return this.query;
        }

        public Builder referenceId(String referenceId) {
            this.query.setReferenceId(referenceId);
            return this;
        }

        public Builder referenceType(String referenceType) {
            this.query.setReferenceType(referenceType);
            return this;
        }

        public Builder name(String name) {
            this.query.setName(name);
            return this;
        }

        public Builder type(String type) {
            this.query.setType(type);
            return this;
        }

        public Builder homepage(Boolean homepage) {
            this.query.setHomepage(homepage);
            return this;
        }

        public Builder published(Boolean published) {
            this.query.setPublished(published);
            return this;
        }

        public Builder visibility(String visibility) {
            this.query.setVisibility(visibility);
            return this;
        }

        public Builder parent(String parent) {
            this.query.setParent(parent);
            return this;
        }

        public Builder rootParent(Boolean root) {
            this.query.setRootParent(root);
            return this;
        }

        public Builder withAutoFetch() {
            this.query.setUseAutoFetch(Boolean.TRUE);
            return this;
        }
    }
}
