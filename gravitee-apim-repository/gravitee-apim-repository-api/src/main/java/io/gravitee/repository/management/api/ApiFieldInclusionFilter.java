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

import java.util.ArrayList;

public class ApiFieldInclusionFilter {

    private final boolean categories;

    private ApiFieldInclusionFilter(Builder builder) {
        categories = builder.categories;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasCategories() {
        return categories;
    }

    public String[] includedFields() {
        ArrayList<String> fields = new ArrayList<>();
        if (categories) {
            fields.add("categories");
        }
        return fields.toArray(new String[0]);
    }

    public static class Builder {

        private boolean categories;

        public Builder includeCategories() {
            categories = true;
            return this;
        }

        public ApiFieldInclusionFilter build() {
            return new ApiFieldInclusionFilter(this);
        }
    }
}
