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

import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PaginationLinksTest {

    private static final URI REQUEST_URI = URI.create("http://localhost:8083/management/path");
    private static final MultivaluedMap<String, String> NO_QUERY_PARAMETERS = new MultivaluedHashMap<>();

    @Test
    void should_build_only_self_link_when_no_result() {
        int total = 0;
        int perPage = 10;

        var result = PaginationLinks.computePaginationLinks(REQUEST_URI, NO_QUERY_PARAMETERS, total, new PaginationParam(1, perPage));

        assertThat(result).isEqualTo(new Links().self(REQUEST_URI.toString()));
    }

    @Test
    void should_build_only_self_link_when_only_one_page() {
        int total = 5;
        int perPage = 10;

        var result = PaginationLinks.computePaginationLinks(REQUEST_URI, NO_QUERY_PARAMETERS, total, new PaginationParam(1, perPage));

        assertThat(result).isEqualTo(new Links().self(REQUEST_URI.toString()));
    }

    @Test
    void should_build_links_when_displaying_the_1st_page_with_default_per_page_param() {
        int total = 35;
        int perPage = 10;

        var result = PaginationLinks.computePaginationLinks(REQUEST_URI, NO_QUERY_PARAMETERS, total, new PaginationParam(1, perPage));

        assertThat(result).isEqualTo(
            new Links()
                .self(REQUEST_URI.toString())
                .first(REQUEST_URI + "?page=1")
                .next(REQUEST_URI + "?page=2")
                .last(REQUEST_URI + "?page=4")
        );
    }

    @Test
    void should_build_links_when_displaying_the_1st_page_with_provided_per_page_param() {
        int total = 35;
        int perPage = 10;

        var result = PaginationLinks.computePaginationLinks(
            URI.create(REQUEST_URI + "?perPage=" + perPage),
            new MultivaluedHashMap<>(ofEntries(entry(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, String.valueOf(perPage)))),
            total,
            new PaginationParam(1, perPage)
        );

        assertThat(result).isEqualTo(
            new Links()
                .self(REQUEST_URI + "?perPage=10")
                .first(REQUEST_URI + "?perPage=10&page=1")
                .next(REQUEST_URI + "?perPage=10&page=2")
                .last(REQUEST_URI + "?perPage=10&page=4")
        );
    }

    @Test
    void should_build_links_when_displaying_the_1st_page_when_explicitly_requested() {
        int total = 35;
        int page = 1;
        int perPage = 10;

        var result = PaginationLinks.computePaginationLinks(
            URI.create(REQUEST_URI + "?page=" + page),
            new MultivaluedHashMap<>(ofEntries(entry(PaginationParam.PAGE_QUERY_PARAM_NAME, String.valueOf(page)))),
            total,
            new PaginationParam(page, perPage)
        );

        assertThat(result).isEqualTo(
            new Links()
                .self(REQUEST_URI + "?page=1")
                .first(REQUEST_URI + "?page=1")
                .next(REQUEST_URI + "?page=2")
                .last(REQUEST_URI + "?page=4")
        );
    }

    @Test
    void should_build_links_when_displaying_a_page() {
        int total = 35;
        int page = 2;
        int perPage = 10;

        var result = PaginationLinks.computePaginationLinks(
            URI.create(REQUEST_URI + "?page=" + page),
            new MultivaluedHashMap<>(ofEntries(entry(PaginationParam.PAGE_QUERY_PARAM_NAME, String.valueOf(page)))),
            total,
            new PaginationParam(page, perPage)
        );

        assertThat(result).isEqualTo(
            new Links()
                .self(REQUEST_URI + "?page=2")
                .first(REQUEST_URI + "?page=1")
                .next(REQUEST_URI + "?page=3")
                .previous(REQUEST_URI + "?page=1")
                .last(REQUEST_URI + "?page=4")
        );
    }

    @Test
    void should_build_links_when_displaying_last_page() {
        int total = 35;
        int page = 4;
        int perPage = 10;

        var result = PaginationLinks.computePaginationLinks(
            URI.create(REQUEST_URI + "?page=" + page),
            new MultivaluedHashMap<>(ofEntries(entry(PaginationParam.PAGE_QUERY_PARAM_NAME, String.valueOf(page)))),
            total,
            new PaginationParam(page, perPage)
        );

        assertThat(result).isEqualTo(
            new Links()
                .self(REQUEST_URI + "?page=4")
                .first(REQUEST_URI + "?page=1")
                .previous(REQUEST_URI + "?page=3")
                .last(REQUEST_URI + "?page=4")
        );
    }

    @Test
    void should_keep_perPage_query_param_when_provided() {
        int total = 35;
        int page = 2;
        int perPage = 10;

        var result = PaginationLinks.computePaginationLinks(
            URI.create(REQUEST_URI + "?page=" + page + "&perPage=" + perPage),
            new MultivaluedHashMap<>(
                ofEntries(
                    entry(PaginationParam.PAGE_QUERY_PARAM_NAME, String.valueOf(page)),
                    entry(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, String.valueOf(perPage))
                )
            ),
            total,
            new PaginationParam(page, perPage)
        );

        assertThat(result).isEqualTo(
            new Links()
                .self(REQUEST_URI + "?page=2&perPage=10")
                .first(REQUEST_URI + "?page=1&perPage=10")
                .next(REQUEST_URI + "?page=3&perPage=10")
                .previous(REQUEST_URI + "?page=1&perPage=10")
                .last(REQUEST_URI + "?page=4&perPage=10")
        );
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 55 })
    void should_return_null_on_invalid_situation(int page) {
        int total = 35;
        int perPage = 10;

        var result = PaginationLinks.computePaginationLinks(REQUEST_URI, NO_QUERY_PARAMETERS, total, new PaginationParam(page, perPage));

        assertThat(result).isNull();
    }
}
