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
package io.gravitee.repository.management.api;

public class ApiFieldFilter {

    public static ApiFieldFilter emptyFilter() {
        return new ApiFieldFilter();
    }

    private final boolean categoriesIncluded;

    private final boolean definitionExcluded;
    private final boolean pictureExcluded;

    public ApiFieldFilter() {
        this.categoriesIncluded = false;
        this.definitionExcluded = false;
        this.pictureExcluded = false;
    }

    private ApiFieldFilter(Builder builder) {
        categoriesIncluded = builder.categoriesIncluded;
        definitionExcluded = builder.definitionExcluded;
        pictureExcluded = builder.pictureExcluded;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean areCategoriesIncluded() {
        return categoriesIncluded;
    }

    public boolean isDefinitionExcluded() {
        return definitionExcluded;
    }

    public boolean isPictureExcluded() {
        return pictureExcluded;
    }

    public static class Builder {

        private boolean categoriesIncluded;

        private boolean definitionExcluded;
        private boolean pictureExcluded;

        public Builder excludeDefinition() {
            this.definitionExcluded = true;
            return this;
        }

        public Builder excludePicture() {
            this.pictureExcluded = true;
            return this;
        }

        public Builder includeCategories() {
            categoriesIncluded = true;
            return this;
        }

        public ApiFieldFilter build() {
            return new ApiFieldFilter(this);
        }
    }
}
