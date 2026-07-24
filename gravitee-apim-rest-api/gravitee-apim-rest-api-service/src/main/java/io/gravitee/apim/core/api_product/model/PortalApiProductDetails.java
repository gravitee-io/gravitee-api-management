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
package io.gravitee.apim.core.api_product.model;

import java.util.List;

public record PortalApiProductDetails(
    String id,
    String name,
    String description,
    String version,
    ApiProductKind kind,
    String navigationItemId,
    List<String> tags,
    List<ApiSummary> apis
) {
    public PortalApiProductDetails {
        tags = tags == null ? List.of() : List.copyOf(tags);
        apis = apis == null ? List.of() : List.copyOf(apis);
    }

    public record ApiSummary(String id, String name, String version) {}
}
