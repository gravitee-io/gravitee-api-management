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

import java.util.Objects;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiFieldFilter {

    /**
     * All fields are included even  consequent ones like Base64 images and apiDefinition
     * /!\ This can cause performance issues.
     */
    public static ApiFieldFilter allFields() {
        return new ApiFieldFilter();
    }

    /**
     * Only include fields that would not cause performance issues.
     */
    public static ApiFieldFilter defaultFields() {
        return new Builder().excludeDefinition().excludePicture().build();
    }

    private final boolean definitionExcluded;
    private final boolean pictureExcluded;

    private ApiFieldFilter() {
        this.definitionExcluded = false;
        this.pictureExcluded = false;
    }

    private ApiFieldFilter(ApiFieldFilter.Builder builder) {
        this.definitionExcluded = builder.excludeDefinition;
        this.pictureExcluded = builder.excludePicture;
    }

    public boolean isDefinitionExcluded() {
        return definitionExcluded;
    }

    public boolean isPictureExcluded() {
        return pictureExcluded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApiFieldFilter)) return false;
        ApiFieldFilter that = (ApiFieldFilter) o;
        return definitionExcluded == that.definitionExcluded && pictureExcluded == that.pictureExcluded;
    }

    @Override
    public int hashCode() {
        return Objects.hash(definitionExcluded, pictureExcluded);
    }

    public static class Builder {

        private boolean excludeDefinition;
        private boolean excludePicture;

        public ApiFieldFilter.Builder excludeDefinition() {
            this.excludeDefinition = true;
            return this;
        }

        public ApiFieldFilter.Builder excludePicture() {
            this.excludePicture = true;
            return this;
        }

        public ApiFieldFilter build() {
            return new ApiFieldFilter(this);
        }
    }
}
