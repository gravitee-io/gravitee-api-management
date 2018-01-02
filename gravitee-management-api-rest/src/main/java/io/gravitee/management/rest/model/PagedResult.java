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
package io.gravitee.management.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class PagedResult<T> {
    private final List<T> data;
    private final Map<String, Map<String, Object>> metadata;
    private Page page;

    public PagedResult(List<T> data, Map<String, Map<String, Object>> metadata) {
        this.data = data;
        this.metadata = metadata;
    }

    public List<T> getData() {
        return data;
    }

    public Map<String, Map<String, Object>> getMetadata() {
        return metadata;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public class Page {
        /**
         * the current page number. Start to 1
         */
        private final int current;

        /**
         * the requested number of elements per page
         */
        @JsonProperty("per_page")
        private final  int perPage;

        /**
         * the size of the result
         */
        private final  int size;

        /**
         * the maximum page number
         */
        @JsonProperty("total_pages")
        private final int totalPages;

        /**
         * the count of all elements
         */
        @JsonProperty("total_elements")
        private final int totalElements;

        public Page(int current, int perPage, int size, int totalPages, int totalElements) {
            this.current = current;
            this.perPage = perPage;
            this.size = size;
            this.totalPages = totalPages;
            this.totalElements = totalElements;
        }

        public int getCurrent() {
            return current;
        }
        public int getPerPage() {
            return perPage;
        }
        public int getSize() {
            return size;
        }
        public int getTotalPages() {
            return totalPages;
        }
        public int getTotalElements() {
            return totalElements;
        }
    }
}
