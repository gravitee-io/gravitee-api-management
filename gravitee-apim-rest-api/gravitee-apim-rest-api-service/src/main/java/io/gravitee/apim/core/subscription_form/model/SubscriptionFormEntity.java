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
package io.gravitee.apim.core.subscription_form.model;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain entity representing a subscription form used by API consumers
 * when subscribing to APIs in the Developer Portal.
 *
 * <p>The form content is defined using Gravitee Markdown (GMD) syntax with
 * form components like gmd-input, gmd-textarea, gmd-select, gmd-checkbox, gmd-radio.</p>
 *
 * <p>Currently, forms are scoped to the environment level (one form per environment).
 * Future versions may support per-API or per-plan forms.</p>
 *
 * @author Gravitee.io Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SubscriptionFormEntity {

    /**
     * Unique identifier of the subscription form.
     */
    private String id;

    /**
     * Environment ID this form belongs to.
     * Forms are currently scoped at the environment level.
     */
    private String environmentId;

    /**
     * Gravitee Markdown (GMD) content defining the form.
     * Supports form components like gmd-input, gmd-textarea, gmd-select, gmd-checkbox, gmd-radio.
     */
    private String gmdContent;

    /**
     * Whether the form is enabled and visible to API consumers in the Developer Portal.
     * When disabled, the form will not be displayed during the API subscription process.
     */
    private boolean enabled;

    /**
     * Creation timestamp.
     */
    private ZonedDateTime createdAt;

    /**
     * Last update timestamp.
     */
    private ZonedDateTime updatedAt;

    /**
     * Updates this form with new GMD content.
     *
     * @param gmdContent new GMD content for the form
     * @param now current timestamp for updatedAt
     * @return updated SubscriptionFormEntity instance
     */
    public SubscriptionFormEntity update(String gmdContent, ZonedDateTime now) {
        return this.toBuilder().gmdContent(gmdContent).updatedAt(now).build();
    }

    /**
     * Enables the subscription form.
     *
     * @param now current timestamp for updatedAt
     * @return updated SubscriptionFormEntity instance with enabled=true
     */
    public SubscriptionFormEntity enable(ZonedDateTime now) {
        return this.toBuilder().enabled(true).updatedAt(now).build();
    }

    /**
     * Disables the subscription form.
     *
     * @param now current timestamp for updatedAt
     * @return updated SubscriptionFormEntity instance with enabled=false
     */
    public SubscriptionFormEntity disable(ZonedDateTime now) {
        return this.toBuilder().enabled(false).updatedAt(now).build();
    }
}
