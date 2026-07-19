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

import io.gravitee.definition.model.v4.ApiType;
import java.util.EnumSet;
import java.util.Set;

/**
 * Which API types may be composed into an API Product, and therefore the only ones for which
 * {@code allowedInApiProducts} carries meaning.
 */
public final class ApiProductComposition {

    private static final Set<ApiType> COMPOSABLE_TYPES = EnumSet.of(ApiType.PROXY, ApiType.LLM_PROXY);

    private ApiProductComposition() {}

    public static boolean supports(final ApiType type) {
        return type != null && COMPOSABLE_TYPES.contains(type);
    }
}
