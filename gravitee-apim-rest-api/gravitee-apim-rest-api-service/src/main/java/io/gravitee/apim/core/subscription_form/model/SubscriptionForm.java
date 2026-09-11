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

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;

/**
 * Domain entity representing a subscription form used by API consumers
 * when subscribing to APIs in the Developer Portal.
 *
 * <p>The form content is defined using Gravitee Markdown (GMD) syntax with
 * form components like gmd-input, gmd-textarea, gmd-select, gmd-checkbox, gmd-radio.</p>
 *
 * <p>An environment holds a catalog of named forms. Exactly one of them is the environment
 * <em>default</em>, used for every API that has no dedicated form.</p>
 *
 * <p>Mutation (update, enable, disable) is done via instance methods that modify internal state,
 * similar to {@link io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent}.</p>
 *
 * @author Gravitee.io Team
 */
@Getter
@Builder
public class SubscriptionForm {

    private final SubscriptionFormId id;
    private final String environmentId;

    /**
     * The {@link io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent} holding the form
     * definition, shared with every other GMD-backed portal content. {@code null} until the form has been
     * persisted, and on a legacy form whose definition is still stored inline: the CRUD service creates
     * the content on the next write in both cases.
     */
    @Nullable
    private final PortalPageContentId portalPageContentId;

    /** Display name, unique within the environment. */
    private String name;

    /** The form definition, loaded from {@link #portalPageContentId}. */
    private GraviteeMarkdown gmdContent;
    private boolean enabled;

    /** Whether this form is the environment default. */
    private boolean defaultForm;

    private SubscriptionFormFieldConstraints validationConstraints;

    /**
     * Updates the definition of this form (mutates in place).
     */
    public void update(GraviteeMarkdown gmdContent, SubscriptionFormFieldConstraints constraints) {
        this.gmdContent = gmdContent;
        this.validationConstraints = constraints;
    }

    /**
     * Renames this form (mutates in place).
     */
    public void rename(String name) {
        this.name = name;
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

    /**
     * Makes this form the environment default (mutates in place).
     */
    public void markAsDefault() {
        this.defaultForm = true;
    }

    /**
     * Withdraws the environment default role from this form (mutates in place).
     */
    public void unmarkAsDefault() {
        this.defaultForm = false;
    }
}
