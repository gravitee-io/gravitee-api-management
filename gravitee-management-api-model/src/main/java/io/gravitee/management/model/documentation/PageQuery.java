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
package io.gravitee.management.model.documentation;

import io.gravitee.management.model.PageType;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class PageQuery {

    private String api;
    private String name;
    private Boolean published;
    private PageType type;
    private Boolean homepage;
    private String parent;
    private Boolean rootParent;

    private PageQuery() {}

    public String getApi() {
        return api;
    }
    public String getName() {
        return name;
    }
    public PageType getType() {
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


    private void setApi(String api) {
        this.api = api;
    }
    private void setName(String name) {
        this.name = name;
    }
    private void setType(PageType type) {
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

    public static class Builder {

        private PageQuery query;

        public Builder() {
            this.query = new PageQuery();
        }

        public PageQuery build() {
            return this.query;
        }

        public Builder api(String api) {
            this.query.setApi(api);
            return this;
        }

        public Builder name(String name) {
            this.query.setName(name);
            return this;
        }

        public Builder type(PageType type) {
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

        public Builder parent(String parent) {
            this.query.setParent(parent);
            return this;
        }
        public Builder rootParent(Boolean root) {
            this.query.setRootParent(root);
            return this;
        }
    }
}
