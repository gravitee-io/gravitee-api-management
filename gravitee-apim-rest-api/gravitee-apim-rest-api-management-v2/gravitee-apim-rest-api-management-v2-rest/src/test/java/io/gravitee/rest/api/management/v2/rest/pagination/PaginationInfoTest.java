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

import static io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo.computePaginationInfo;

import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class PaginationInfoTest {

    @Test
    void should_build_pagination_info() {
        var total = 20L;
        var pageItemsCount = 10;
        var page = 2;
        var perPage = 5;

        var pagination = computePaginationInfo(total, pageItemsCount, new PaginationParam(page, perPage));

        Assertions.assertThat(pagination).isEqualTo(
            new Pagination().page(page).perPage(perPage).totalCount(total).pageItemsCount(pageItemsCount).pageCount(4)
        );
    }

    @Test
    void should_return_empty_pagination_when_no_result() {
        var total = 0L;
        var pageItemsCount = 0;
        var page = 1;
        var perPage = 5;

        var pagination = computePaginationInfo(total, pageItemsCount, new PaginationParam(page, perPage));

        Assertions.assertThat(pagination).isEqualTo(new Pagination());
    }

    @ParameterizedTest
    @CsvSource(
        delimiterString = "|",
        textBlock = """
        # TOTAL | PER_PAGE | EXPECTED_PAGE_COUNT
        20      | 5        | 4
        25      | 9        | 3
        5       | 20       | 1
        """
    )
    void should_count_pages_based_on_total_and_provided_pagination_param(Long total, Integer perPage, Integer expectedPageCount) {
        var pagination = computePaginationInfo(total, 10, new PaginationParam(1, perPage));

        Assertions.assertThat(pagination).extracting(Pagination::getPageCount).isEqualTo(expectedPageCount);
    }
}
