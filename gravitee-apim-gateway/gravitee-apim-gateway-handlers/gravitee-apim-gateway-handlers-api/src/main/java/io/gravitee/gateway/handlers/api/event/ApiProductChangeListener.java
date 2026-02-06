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
package io.gravitee.gateway.handlers.api.event;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactive.handlers.api.v4.DefaultApiReactor;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Listener for ApiProductChangedEvent that refreshes security chains for affected APIs.
 * When an API Product changes (deploy/update/undeploy), this listener refreshes the
 * security chain of all APIs that are part of the product without requiring API redeploy.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class ApiProductChangeListener
    extends AbstractService<ApiProductChangeListener>
    implements EventListener<ApiProductEventType, ApiProductChangedEvent> {

    private final ApiManager apiManager;
    private final ReactorHandlerRegistry reactorHandlerRegistry;

    @Override
    public void onEvent(Event<ApiProductEventType, ApiProductChangedEvent> event) {
        ApiProductChangedEvent productEvent = event.content();
        if (productEvent == null) {
            return;
        }

        log.debug(
            "Processing ApiProductChangedEvent for product [{}] (changeType: {}) affecting {} APIs",
            productEvent.getProductId(),
            productEvent.getChangeType(),
            productEvent.getApiIds().size()
        );

        for (String apiId : productEvent.getApiIds()) {
            try {
                refreshSecurityChainForApi(apiId, productEvent.getProductId());
            } catch (Exception e) {
                log.warn("Failed to refresh security chain for API [{}] due to product [{}] change", apiId, productEvent.getProductId(), e);
                // Continue with other APIs - don't fail the whole event processing
            }
        }
    }

    /**
     * Refresh security chain for a specific API.
     *
     * @param apiId the API ID
     * @param productId the API Product ID that triggered the refresh
     */
    private void refreshSecurityChainForApi(String apiId, String productId) {
        ReactableApi<?> reactableApi = apiManager.get(apiId);
        if (reactableApi == null) {
            log.debug("Reactable API [{}] not found, skipping security chain refresh", apiId);
            return;
        }

        Optional<ReactorHandler> handlerOpt = reactorHandlerRegistry.getReactorHandler(reactableApi);
        if (handlerOpt.isEmpty()) {
            log.debug("Reactor handler not found for API [{}], skipping security chain refresh", apiId);
            return;
        }

        ReactorHandler handler = handlerOpt.get();
        if (handler instanceof DefaultApiReactor defaultApiReactor) {
            defaultApiReactor.refreshSecurityChain();
            log.debug("Refreshed security chain for API [{}] due to product [{}] change", apiId, productId);
        } else {
            log.debug(
                "Reactor handler for API [{}] is not a DefaultApiReactor (type: {}), skipping security chain refresh",
                apiId,
                handler.getClass().getName()
            );
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        // EventManager subscription will be handled by Spring configuration
        // or we need to inject EventManager and subscribe here
        log.info("ApiProductChangeListener started");
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        log.info("ApiProductChangeListener stopped");
    }
}
