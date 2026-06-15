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
package io.gravitee.gamma.rest.core.observability.logs.model;

/**
 * Per-API enrichment context resolved once while computing the query scope and carried alongside the
 * search query, so the data port can attach display values to each log row without re-loading the API
 * entities.
 *
 * @param name    Human-readable API name.
 * @param apiType API kind as the canonical wire value (the {@code ApiType} enum name, e.g.
 *                {@code "HTTP_PROXY"}), matching the {@code apiType} field documented for log rows and
 *                the {@code API_TYPE} filter values.
 *
 * @author GraviteeSource Team
 */
public record ApiReference(String name, String apiType) {}
