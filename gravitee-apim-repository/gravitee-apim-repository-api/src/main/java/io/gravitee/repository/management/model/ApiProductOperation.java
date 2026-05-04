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
package io.gravitee.repository.management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single allowed HTTP operation (path + method) within an API Product's per-API filter.
 * When an API Product has an entry in {@code ApiProduct.apiOperations} for a given API,
 * only the listed operations are accessible through the product. Use {@code method = "*"}
 * to match all HTTP methods for a given path. Use {@code :param} notation for path parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiProductOperation {

    private String path;
    private String method;
}
