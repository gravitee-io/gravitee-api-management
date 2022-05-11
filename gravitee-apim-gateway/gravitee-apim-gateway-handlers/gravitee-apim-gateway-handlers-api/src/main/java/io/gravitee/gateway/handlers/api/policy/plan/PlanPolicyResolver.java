/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.handlers.api.policy.plan;

import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Rule;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.policy.RuleBasedPolicyResolver;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.StreamType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A policy resolver based on the plan subscribed by the consumer.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanPolicyResolver extends RuleBasedPolicyResolver {

    private final Logger logger = LoggerFactory.getLogger(PlanPolicyResolver.class);

    private static final String DEFAULT_PLAN_PATH = "/";

    private final Api api;

    public PlanPolicyResolver(Api api) {
        this.api = api;
    }

    @Override
    public List<PolicyMetadata> resolve(StreamType streamType, ExecutionContext context) {
        Plan apiPlan = api.getPlan(context.request().metrics().getPlan());

        // No plan is matching the plan associated to the secured request
        // The call is probably not relative to the same API.
        if (apiPlan != null) {
            Map<String, List<Rule>> paths = apiPlan.getPaths();

            if (paths != null && !paths.isEmpty()) {
                // For 1.0.0, there is only a single root path defined
                // Must be reconsidered when user will be able to manage policies at the plan level by himself
                List<Rule> rootPath = paths.get(DEFAULT_PLAN_PATH);

                return resolve(context, rootPath);
            }
        } else {
            logger.warn(
                "No plan has been selected to process request {}. Returning an unauthorized HTTP status (401)",
                context.request().id()
            );
            return null;
        }

        return Collections.emptyList();
    }
}
