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
package io.gravitee.gateway.flow;

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.flow.policy.PolicyResolver;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.cache.standalone.StandaloneCache;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowPolicyResolver implements PolicyResolver {

    public static final long CACHE_MAX_SIZE = 15;
    public static final long CACHE_TIME_TO_IDLE = 3600;

    private final Flow flow;

    @VisibleForTesting
    final Cache<Step, PolicyMetadata> cache;

    public FlowPolicyResolver(Flow flow) {
        this.flow = flow;

        final CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setMaxSize(CACHE_MAX_SIZE);
        cacheConfiguration.setTimeToIdleSeconds(CACHE_TIME_TO_IDLE);
        cache = new StandaloneCache<>("flowPolicyResolverCache", cacheConfiguration);
    }

    @Override
    public List<PolicyMetadata> resolve(StreamType streamType, ExecutionContext context) {
        // TODO: Before executing the flow, ensure that it should be effectively run
        List<Step> steps = streamType == StreamType.ON_REQUEST ? flow.getPre() : flow.getPost();

        if (steps.isEmpty()) {
            return Collections.emptyList();
        }

        // TODO: Used by some policies (ie. rate-limit / quota)
        context.setAttribute(ExecutionContext.ATTR_RESOLVED_PATH, flow.getPath());

        return steps.stream().filter(Step::isEnabled).map(this::createOrGetCachePolicyMetadata).collect(Collectors.toList());
    }

    private PolicyMetadata createOrGetCachePolicyMetadata(Step step) {
        PolicyMetadata cachedPolicyMetadata = cache.get(step);
        if (cachedPolicyMetadata == null) {
            final PolicyMetadata policyMetadata = new PolicyMetadata(step.getPolicy(), step.getConfiguration(), step.getCondition());
            policyMetadata.metadata().put(PolicyMetadata.MetadataKeys.STAGE, flow.getStage());
            cache.put(step, policyMetadata);
            cachedPolicyMetadata = policyMetadata;
        }

        return cachedPolicyMetadata;
    }
}
