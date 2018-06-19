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
public class ApiFieldExclusionFilter {

    private boolean definition;
    private boolean picture;

    private ApiFieldExclusionFilter(ApiFieldExclusionFilter.Builder builder) {
        this.definition = builder.definition;
        this.picture = builder.picture;
    }

    public boolean isDefinition() {
        return definition;
    }

    public boolean isPicture() {
        return picture;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApiFieldExclusionFilter)) return false;
        ApiFieldExclusionFilter that = (ApiFieldExclusionFilter) o;
        return definition == that.definition &&
                picture == that.picture;
    }

    @Override
    public int hashCode() {

        return Objects.hash(definition, picture);
    }

    public static class Builder {
        private boolean definition;
        private boolean picture;

        public ApiFieldExclusionFilter.Builder excludeDefinition() {
            this.definition = true;
            return this;
        }

        public ApiFieldExclusionFilter.Builder excludePicture() {
            this.picture = true;
            return this;
        }

        public ApiFieldExclusionFilter build() {
            return new ApiFieldExclusionFilter(this);
        }
    }
}
