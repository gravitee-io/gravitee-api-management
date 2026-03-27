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
import java.util.List;
import java.util.Map;

/**
 * Resolves EL expressions embedded in subscription form option fields.
 *
 * <p>Used in two contexts:</p>
 * <ul>
 *   <li><b>Form retrieval</b> — resolves {@link SubscriptionFormSchema.DynamicOptions} expressions for
 *       option-bearing fields and returns a {@code fieldKey → resolved options} map to the frontend.
 *       When {@code apiId} is {@code null} (e.g. Console Form Builder) only environment-level metadata is used.</li>
 *   <li><b>Subscription validation</b> — replaces {@link io.gravitee.apim.core.subscription_form.model.Constraint.DynamicOneOf}
 *       and {@link io.gravitee.apim.core.subscription_form.model.Constraint.DynamicEachOf} constraints with
 *       their resolved static equivalents before the validator runs.</li>
 * </ul>
 *
 * @author Gravitee.io Team
 */
public interface SubscriptionFormElResolverDomainService {
    /**
     * Resolves EL expressions for all dynamic-option fields in the schema.
     *
     * @param schema        the subscription form schema (may have fields with {@link SubscriptionFormSchema.DynamicOptions})
     * @param environmentId the environment identifier (required for API metadata lookup)
     * @param apiId         the API identifier; when {@code null}, only environment-level metadata is used
     * @return a map of {@code fieldKey → effective option list} for every field that has dynamic options;
     *         fields without dynamic options are not included
     */
    Map<String, List<String>> resolveSchemaOptions(SubscriptionFormSchema schema, String environmentId, String apiId);

    /**
     * Pre-resolves {@link io.gravitee.apim.core.subscription_form.model.Constraint.DynamicOneOf} and
     * {@link io.gravitee.apim.core.subscription_form.model.Constraint.DynamicEachOf} constraints to their
     * static {@link io.gravitee.apim.core.subscription_form.model.Constraint.OneOf} /
     * {@link io.gravitee.apim.core.subscription_form.model.Constraint.EachOf} equivalents.
     *
     * @param constraints   the stored constraints (may contain dynamic variants)
     * @param environmentId the environment identifier
     * @param apiId         the API identifier
     * @return a new {@link SubscriptionFormFieldConstraints} instance with all dynamic constraints resolved
     */
    SubscriptionFormFieldConstraints resolveConstraints(SubscriptionFormFieldConstraints constraints, String environmentId, String apiId);
}
