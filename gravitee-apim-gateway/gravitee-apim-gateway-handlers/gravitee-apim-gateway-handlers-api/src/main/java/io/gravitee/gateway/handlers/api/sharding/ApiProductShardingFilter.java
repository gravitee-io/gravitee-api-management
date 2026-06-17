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
 * Sharding tag matching for API Products and API Product plans (aligned with API sharding in gateway).
 *
 * <p>Follows the same rules as APIs: tagless gateways retrieve everything; tagged gateways
 * only retrieve entities whose tags match.
 */
public final class ApiProductShardingFilter {

    private ApiProductShardingFilter() {}

    public static boolean matchesProductTags(GatewayConfiguration gatewayConfiguration, Set<String> productTags) {
        return gatewayConfiguration.hasMatchingTags(productTags);
    }

    public static boolean matchesPlanTags(GatewayConfiguration gatewayConfiguration, Set<String> planTags) {
        if (planTags == null || planTags.isEmpty()) {
            return true;
        }
        return gatewayConfiguration.hasMatchingTags(planTags);
    }
}
