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

import java.util.List;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Repository model for subscription form used by API consumers when subscribing to APIs.
 *
 * @author Gravitee.io Team
 */
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SubscriptionForm {

    /**
     * Unique identifier of the subscription form.
     */
    private String id;

    /**
     * Environment ID this form belongs to.
     */
    private String environmentId;

    /**
     * Display name of the form, unique within its environment.
     */
    private String name;

    /**
     * Gravitee Markdown (GMD) content defining the form.
     */
    private String gmdContent;

    /**
     * Whether the form is enabled and visible to API consumers.
     */
    private boolean enabled;

    /**
     * Whether this form is the environment default, used for every API without a dedicated form.
     * At most one form per environment is the default.
     */
    private boolean defaultForm;

    /**
     * Identifiers of the APIs this form is dedicated to. An API is mapped to at most one form.
     */
    @Builder.Default
    private List<String> apiIds = List.of();

    /**
     * JSON string of validation constraints per field key, derived from the GMD content.
     * {@code null} when nothing is stored (empty rule sets are typically not persisted).
     */
    private String validationConstraints;

    /**
     * Uniqueness key derived from {@link #name}: trimmed and lowercased, so two forms of an environment cannot
     * differ by case or padding alone. Persisted next to the name, where it carries the unique index the
     * implementations rely on; it is never set by the caller.
     */
    public String getNormalizedName() {
        return name == null ? null : name.trim().toLowerCase(Locale.ROOT);
    }
}
