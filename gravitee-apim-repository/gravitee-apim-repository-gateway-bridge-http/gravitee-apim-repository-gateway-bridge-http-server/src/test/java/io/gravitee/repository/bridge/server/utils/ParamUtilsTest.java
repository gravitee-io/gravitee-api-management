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
package io.gravitee.repository.bridge.server.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ParamUtilsTest {

    @Mock
    RoutingContext routingContext;

    @Mock
    HttpServerRequest httpServerRequest;

    @BeforeEach
    public void beforeEach() {
        when(routingContext.request()).thenReturn(httpServerRequest);
    }

    @Nested
    class SortableTest {

        @Test
        void should_return_sortable_when_field_params_exist() {
            when(httpServerRequest.getParam("order")).thenReturn("ASC");
            when(httpServerRequest.getParam("field")).thenReturn("field");

            Sortable sortable = ParamUtils.readSortable(routingContext);
            assertThat(sortable).isNotNull();
            assertThat(sortable.order()).isEqualTo(Order.ASC);
            assertThat(sortable.field()).isEqualTo("field");
        }

        @Test
        void should_not_return_sortable_when_field_params_doesnt_exist() {
            when(httpServerRequest.getParam("order")).thenReturn("ASC");

            Sortable sortable = ParamUtils.readSortable(routingContext);
            assertThat(sortable).isNull();
        }

        @Test
        void should_not_return_sortable_when_wrong_order_params() {
            when(httpServerRequest.getParam("order")).thenReturn("wrong");

            Sortable sortable = ParamUtils.readSortable(routingContext);
            assertThat(sortable).isNull();
        }

        @Test
        void should_not_return_sortable_when_params_dont_exist() {
            Sortable sortable = ParamUtils.readSortable(routingContext);
            assertThat(sortable).isNull();
        }
    }

    @Nested
    class PageableTest {

        @Test
        void should_return_pageable_when_field_params_exist() {
            when(httpServerRequest.getParam("size")).thenReturn("1");
            when(httpServerRequest.getParam("page")).thenReturn("1");

            Pageable pageable = ParamUtils.readPageable(routingContext);
            assertThat(pageable).isNotNull();
            assertThat(pageable.pageNumber()).isEqualTo(1);
            assertThat(pageable.pageSize()).isEqualTo(1);
        }

        @Test
        void should_not_return_sortable_when_page_params_doesnt_exist() {
            when(httpServerRequest.getParam("size")).thenReturn("1");

            Pageable pageable = ParamUtils.readPageable(routingContext);
            assertThat(pageable).isNull();
        }

        @Test
        void should_not_return_sortable_when_size_params_doesnt_exist() {
            when(httpServerRequest.getParam("page")).thenReturn("1");

            Pageable pageable = ParamUtils.readPageable(routingContext);
            assertThat(pageable).isNull();
        }

        @Test
        void should_not_return_sortable_when_params_dont_exist() {
            Pageable pageable = ParamUtils.readPageable(routingContext);
            assertThat(pageable).isNull();
        }
    }
}
