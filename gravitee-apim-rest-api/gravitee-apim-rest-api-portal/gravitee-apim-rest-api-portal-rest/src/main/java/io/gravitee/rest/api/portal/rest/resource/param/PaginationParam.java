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
package io.gravitee.rest.api.portal.rest.resource.param;

import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PaginationParam {

    public static final String PAGE_QUERY_PARAM_NAME = "page";
    public static final String SIZE_QUERY_PARAM_NAME = "size";

    private static final String PAGE_QUERY_PARAM_DEFAULT = "1";
    private static final String SIZE_QUERY_PARAM_DEFAULT = "10";

    @DefaultValue(PAGE_QUERY_PARAM_DEFAULT)
    @QueryParam(PAGE_QUERY_PARAM_NAME)
    @Min(1)
    Integer page;

    @DefaultValue(SIZE_QUERY_PARAM_DEFAULT)
    @QueryParam(SIZE_QUERY_PARAM_NAME)
    @Min(-1)
    Integer size;

    public boolean hasPagination() {
        return this.getSize() != null && !this.getSize().equals(-1);
    }
}
