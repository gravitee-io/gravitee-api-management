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
package io.gravitee.gamma.authorization.rest.dto;

import io.gravitee.gamma.authorization.paging.PagedResult;
import java.util.List;
import java.util.function.Function;

public record PagedResponseDto<T>(List<T> data, long total, int page, int perPage) {
    public static <D, R> PagedResponseDto<R> from(PagedResult<D> result, Function<D, R> mapper) {
        return new PagedResponseDto<>(result.data().stream().map(mapper).toList(), result.total(), result.page(), result.perPage());
    }
}
