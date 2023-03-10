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

import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.vertx.ext.web.RoutingContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParamUtils {

    public static Sortable readSortable(final RoutingContext ctx) {
        try {
            String orderParam = ctx.request().getParam("order");
            String fieldParam = ctx.request().getParam("field");
            if (fieldParam != null) {
                return new SortableBuilder().field(fieldParam).order(Order.valueOf(orderParam)).build();
            }
        } catch (Exception e) {
            // Ignore any exception
        }
        return null;
    }

    public static Pageable readPageable(final RoutingContext ctx) {
        try {
            Long page = getPageNumber(ctx, null);
            Long size = getPageSize(ctx, null);
            if (page != null && size != null) {
                return new PageableBuilder().pageNumber(page.intValue()).pageSize(size.intValue()).build();
            }
        } catch (Exception e) {
            // Ignore any exception
        }
        return null;
    }

    public static Long getPageSize(RoutingContext ctx, Long defaultValue) {
        final String sPageSize = ctx.request().getParam("size");
        try {
            return Long.parseLong(sPageSize);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static Long getPageNumber(RoutingContext ctx, Long defaultValue) {
        final String sPageNumber = ctx.request().getParam("page");

        try {
            return Long.parseLong(sPageNumber);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }
}
