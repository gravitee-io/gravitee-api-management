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
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.cache.standalone.StandaloneCache;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowPolicyResolverFactory {

    public static final long CACHE_MAX_SIZE = 15;
    public static final long CACHE_TIME_TO_IDLE = 3600;

    @VisibleForTesting
    final Cache<Flow, FlowPolicyResolver> cache;

    public FlowPolicyResolverFactory() {
        final CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setMaxSize(CACHE_MAX_SIZE);
        cacheConfiguration.setTimeToIdleSeconds(CACHE_TIME_TO_IDLE);
        cache = new StandaloneCache<>("flowPolicyResolverFactoryCache", cacheConfiguration);
    }

    public FlowPolicyResolver create(Flow flow, FlowResolver flowResolver) {
        FlowPolicyResolver cachedFlow = cache.get(flow);
        if (cachedFlow == null) {
            final FlowPolicyResolver flowPolicyResolver = new FlowPolicyResolver(flow, flowResolver);
            cache.put(flow, flowPolicyResolver);
            cachedFlow = flowPolicyResolver;
        }
        return cachedFlow;
    }
}
