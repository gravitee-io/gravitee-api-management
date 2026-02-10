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
     * Gravitee Markdown (GMD) content defining the form.
     */
    private String gmdContent;

    /**
     * Whether the form is enabled and visible to API consumers.
     */
    private boolean enabled;
}
