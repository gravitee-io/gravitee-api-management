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
package io.gravitee.rest.api.management.v2.rest.resource.param;

import io.gravitee.rest.api.model.common.PageableImpl;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class PaginationParam {

    public static final String PAGE_QUERY_PARAM_NAME = "page";
    public static final String PER_PAGE_QUERY_PARAM_NAME = "perPage";

    private static final String PAGE_QUERY_PARAM_DEFAULT = "1";
    private static final String PER_PAGE_QUERY_PARAM_DEFAULT = "10";

    @DefaultValue(PAGE_QUERY_PARAM_DEFAULT)
    @QueryParam(PAGE_QUERY_PARAM_NAME)
    @Min(value = 1, message = "Pagination page param must be >= 1")
    Integer page;

    @DefaultValue(PER_PAGE_QUERY_PARAM_DEFAULT)
    @QueryParam(PER_PAGE_QUERY_PARAM_NAME)
    @Min(value = 1, message = "Pagination perPage param must be >= 1")
    Integer perPage;

    public PaginationParam(Integer page, Integer perPage) {
        if (perPage < 1) throw new IllegalArgumentException("Pagination perPage param must be >= 1");

        this.page = page;
        this.perPage = perPage;
    }

    public io.gravitee.rest.api.model.common.Pageable toPageable() {
        return new PageableImpl(this.getPage(), this.getPerPage());
    }
}
