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
package io.gravitee.apim.core.subscription_form.domain_service;

import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * Resolves EL expressions embedded in subscription form option fields.
 *
 * <p>Used in two contexts:</p>
 * <ul>
 *   <li><b>Form retrieval</b> — resolves {@link SubscriptionFormSchema.DynamicOptions} expressions for
 *       option-bearing fields and returns a {@code fieldKey → resolved options} map to the frontend.</li>
 *   <li><b>Subscription validation</b> — resolves dynamic option constraints ({@link io.gravitee.apim.core.subscription_form.model.Constraint.OneOf}
 *       and {@link io.gravitee.apim.core.subscription_form.model.Constraint.EachOf} with expression/fallback metadata)
 *       before the validator runs.</li>
 * </ul>
 *
 * <p>Each method has two variants: one that accepts an API context (environment + API id) for full metadata
 * resolution, and one without — for situations where no API context is available (e.g. Console Form Builder).
 * The no-context variants use the configured fallback values directly.</p>
 *
 * @author Gravitee.io Team
 */
public interface SubscriptionFormElResolverDomainService {
    /**
     * Resolves EL expressions for all dynamic-option fields in the schema against API + environment metadata.
     *
     * @param schema        the subscription form schema
     * @param environmentId the environment identifier
     * @param apiId         the API identifier
     * @return a map of {@code fieldKey → effective option list} for every field that has dynamic options
     */
    Map<String, List<String>> resolveSchemaOptions(SubscriptionFormSchema schema, String environmentId, String apiId);

    /**
     * No-context variant — resolves expressions without API/environment metadata and falls back when resolution fails.
     */
    Map<String, List<String>> resolveSchemaOptions(SubscriptionFormSchema schema);

    /**
     * Resolves dynamic option constraints (OneOf/EachOf with expression/fallback metadata)
     * to effective options using API + environment metadata.
     *
     * @param constraints   the stored constraints (some OneOf/EachOf constraints may carry expression/fallback metadata)
     * @param environmentId the environment identifier
     * @param apiId         the API identifier
     * @return a new {@link SubscriptionFormFieldConstraints} with all dynamic options resolved
     */
    SubscriptionFormFieldConstraints resolveConstraints(
        @Nonnull SubscriptionFormFieldConstraints constraints,
        String environmentId,
        String apiId
    );

    /**
     * No-context variant — resolves dynamic options using configured fallback values when context-based resolution is not possible.
     */
    SubscriptionFormFieldConstraints resolveConstraints(@Nonnull SubscriptionFormFieldConstraints constraints);
}
