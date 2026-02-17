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

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownContainer;
import lombok.Builder;
import lombok.Getter;

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
 * <p>Mutation (update, enable, disable) is done via instance methods that modify internal state,
 * similar to {@link io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent}.</p>
 *
 * @author Gravitee.io Team
 */
@Getter
@Builder
public class SubscriptionForm implements GraviteeMarkdownContainer {

    private final SubscriptionFormId id;
    private final String environmentId;

    private String gmdContent;
    private boolean enabled;

    /**
     * Updates this form with new GMD content (mutates in place).
     */
    public void update(String gmdContent) {
        this.gmdContent = gmdContent;
    }

    /**
     * Enables the subscription form (mutates in place).
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * Disables the subscription form (mutates in place).
     */
    public void disable() {
        this.enabled = false;
    }
}
