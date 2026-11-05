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
package io.gravitee.gamma.rest.core.guardian.model;

/**
 * What the console asks for when it lists Guardians: the environment it browses, an optional free
 * text matched against the name, the page it wants and how to order it.
 *
 * <p>A domain-local record rather than the platform's {@code Pageable} / {@code Sortable}: the core
 * layer may not depend on {@code io.gravitee.rest.api..} (AGENTS.md §5, enforced by
 * {@code CoreRulesTest}). The observability domain does the same with its own page models.
 *
 * <p>{@code sortBy} is the field name, optionally prefixed with {@code -} for descending order; the
 * use case validates it against the fields it accepts before handing it over.
 *
 * @param environmentId the environment the Guardians belong to, never null
 * @param query free text matched against the Guardian name, null when the console lists everything
 * @param pageNumber one-based, as the REST layer counts pages
 * @param pageSize how many Guardians the page holds
 * @param sortBy the field to order on, null to let the repository apply its default
 *
 * @author GraviteeSource Team
 */
public record GuardianSearchQuery(String environmentId, String query, int pageNumber, int pageSize, String sortBy) {}
