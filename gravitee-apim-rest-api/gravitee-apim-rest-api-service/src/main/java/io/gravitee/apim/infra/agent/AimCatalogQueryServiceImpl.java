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
package io.gravitee.apim.infra.agent;

import io.gravitee.apim.core.agent.model.PortalAgentCard;
import io.gravitee.apim.core.agent.service_provider.AimCatalogQueryService;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@CustomLog
@RequiredArgsConstructor
public class AimCatalogQueryServiceImpl implements AimCatalogQueryService {

    private final ApplicationContext applicationContext;

    private volatile AimCatalogQueryService delegate;

    @Override
    public Optional<PortalAgentCard> findById(String environmentId, String agentId) {
        var catalog = resolveDelegate();
        if (catalog == null) {
            log.debug("AIM catalog is not available; agent [{}] cannot be loaded", agentId);
            return Optional.empty();
        }
        return catalog.findById(environmentId, agentId);
    }

    private AimCatalogQueryService resolveDelegate() {
        if (delegate != null) {
            return delegate;
        }
        var found = applicationContext
            .getBeansOfType(AimCatalogQueryService.class)
            .values()
            .stream()
            .filter(service -> service != this)
            .findFirst()
            .orElse(null);
        if (found != null) {
            delegate = found;
        }
        return found;
    }
}
