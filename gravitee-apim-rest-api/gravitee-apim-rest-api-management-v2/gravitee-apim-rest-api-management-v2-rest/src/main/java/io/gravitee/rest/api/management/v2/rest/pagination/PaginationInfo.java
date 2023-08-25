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
package io.gravitee.rest.api.management.v2.rest.pagination;

import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;

public class PaginationInfo {

    private PaginationInfo() {}

    public static Pagination computePaginationInfo(Integer totalCount, Integer pageItemsCount, PaginationParam paginationParam) {
        Pagination pagination = new Pagination();
        if (paginationParam.getPerPage() > 0 && totalCount > 0) {
            pagination
                .page(paginationParam.getPage())
                .perPage(paginationParam.getPerPage())
                .pageItemsCount(pageItemsCount)
                .pageCount((int) Math.ceil((double) totalCount / paginationParam.getPerPage()))
                .totalCount(totalCount);
        }
        return pagination;
    }
}
