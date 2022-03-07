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
package io.gravitee.gateway.handlers.api.policy.api;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.handlers.api.path.Path;
import io.gravitee.gateway.handlers.api.policy.RuleBasedPolicyResolver;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.StreamType;
import java.util.List;

/**
 * This resolver is used to calculate the policies to apply for a given {@link ExecutionContext} and is based
 * on the policies defined at the API level.
 *
 * Note that this resolver can be called twice during the request processing and the response processing.
 * Due to this, the resolved path is calculated only during the request and re-use for the response.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPolicyResolver extends RuleBasedPolicyResolver {

    public static final String API_RESOLVED_PATH = ExecutionContext.ATTR_PREFIX + "api-policy-path";

    @Override
    public List<PolicyMetadata> resolve(StreamType streamType, ExecutionContext context) {
        // Has been registered in path parameters processor
        final Path path = (Path) context.getAttribute(API_RESOLVED_PATH);

        // Get the policies according to the resolved path
        return resolve(context, path.getRules());
    }
}
