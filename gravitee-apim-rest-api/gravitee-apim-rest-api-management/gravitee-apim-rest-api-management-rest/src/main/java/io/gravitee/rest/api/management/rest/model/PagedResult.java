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
package io.gravitee.rest.api.management.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@NoArgsConstructor
public class PagedResult<T> {

    private Collection<T> data;

    @Setter
    private Map<String, Map<String, Object>> metadata;

    private Page page;

    public PagedResult(Collection<T> data, int pageNumber, int perPage, int totalElements) {
        this.data = data;
        this.page = new Page(pageNumber, perPage, data.size(), totalElements);
    }

    public PagedResult(Collection<T> data) {
        this(data, 1, data.size(), data.size());
    }

    public PagedResult(io.gravitee.common.data.domain.Page<T> page, int perPage) {
        this(page.getContent(), page.getPageNumber(), perPage, (int) page.getTotalElements());
    }

    @Getter
    public static class Page {

        /**
         * the current page number. Start to 1
         */
        private int current;

        /**
         * the requested number of elements per page
         */
        @JsonProperty("per_page")
        private int perPage;

        /**
         * the size of the result
         */
        private int size;

        /**
         * the maximum page number
         */
        @JsonProperty("total_pages")
        private int totalPages;

        /**
         * the count of all elements
         */
        @JsonProperty("total_elements")
        private int totalElements;

        public Page() {}

        public Page(int current, int perPage, int size, int totalElements) {
            this.current = current;
            this.perPage = perPage;
            this.size = size;
            this.totalPages = (int) Math.ceil((double) totalElements / (double) perPage);
            this.totalElements = totalElements;
        }
    }
}
