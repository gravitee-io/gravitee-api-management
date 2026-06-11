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
package io.gravitee.gamma.rest.resources.tracing.dto;

import java.util.List;

/**
 * Shared wire envelope for paginated responses on the trace explorer surface. Same shape as the apim
 * management v2 analytics + logs responses ({@code data} + nested {@code pagination}) so a single
 * lib-side adapter can read both. {@code page} is 1-based; {@code pageCount} is derived from
 * {@code totalCount} and the request's {@code perPage}.
 */
public record PaginatedResponseDto<T>(List<T> data, Pagination pagination) {
    public record Pagination(long totalCount, int page, int perPage, int pageCount, int pageItemsCount) {}

    public static <T> PaginatedResponseDto<T> of(List<T> data, long totalCount, int page, int perPage) {
        int pageCount = totalCount == 0 || perPage <= 0 ? 0 : (int) Math.ceil((double) totalCount / perPage);
        return new PaginatedResponseDto<>(data, new Pagination(totalCount, page, perPage, pageCount, data.size()));
    }
}
