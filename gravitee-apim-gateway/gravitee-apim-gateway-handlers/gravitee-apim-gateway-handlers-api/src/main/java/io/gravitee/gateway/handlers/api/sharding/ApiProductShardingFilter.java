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
package io.gravitee.gateway.handlers.api.sharding;

import io.gravitee.gateway.env.GatewayConfiguration;
import java.util.Set;

/**
 * Sharding tag matching for API Products and their plans.
 *
 * <p>A product follows the same rule as an API: a tagless gateway takes everything, a tagged
 * gateway takes only what carries one of its tags — including refusing an untagged product, just as
 * {@code ApiManagerImpl} refuses an untagged API.</p>
 *
 * <p><strong>A product plan does not.</strong> An untagged plan is accepted outright, where an
 * untagged API plan goes through the ordinary rule and can make a tagged gateway skip its API. The
 * two are pinned down by tests on both sides:</p>
 * <ul>
 *   <li>API: {@code ApiManagerImplTest.should_not_deploy_api_if_plan_has_non_matching_tag}</li>
 *   <li>API Product: {@code ApiProductV4ShardingIntegrationTest.should_serve_an_untagged_plan_of_a_matching_product}</li>
 * </ul>
 *
 * <p>The short-circuit below is deliberate — it was written, not inherited — but its rationale was
 * never recorded. The defensible reading is that a product plan already sits behind its product's
 * tags, so filtering the same perimeter twice buys nothing. Whether that should be aligned with API
 * plans is an open product question; this note exists so that whoever picks up a bug about a
 * product plan appearing on a gateway that should not serve it lands here directly.</p>
 */
public final class ApiProductShardingFilter {

    private ApiProductShardingFilter() {}

    public static boolean matchesProductTags(GatewayConfiguration gatewayConfiguration, Set<String> productTags) {
        return gatewayConfiguration.hasMatchingTags(productTags);
    }

    public static boolean matchesPlanTags(GatewayConfiguration gatewayConfiguration, Set<String> planTags) {
        // Diverges from API plans on purpose — see the class javadoc before changing this.
        if (planTags == null || planTags.isEmpty()) {
            return true;
        }
        return gatewayConfiguration.hasMatchingTags(planTags);
    }
}
