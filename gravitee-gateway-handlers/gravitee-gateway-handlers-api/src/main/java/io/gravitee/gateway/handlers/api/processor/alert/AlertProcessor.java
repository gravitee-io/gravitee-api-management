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
package io.gravitee.gateway.handlers.api.processor.alert;

import io.gravitee.alert.api.event.Event;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertProcessor extends AbstractProcessor<ExecutionContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertProcessor.class);

    private static final String REQUEST_TYPE = "REQUEST";
    private static final String CONTEXT_GATEWAY = "Gateway";
    private static final String CONTEXT_HOSTNAME = "Hostname";
    private static final String CONTEXT_PORT = "Port";
    private static final String CONTEXT_TENANT = "Tenant";
    private static final String PROP_REQUEST_ID = "Request id";
    private static final String PROP_CONTEXT_PATH = "Context path";
    private static final String PROP_API = "API";
    private static final String PROP_APPLICATION = "APPLICATION";
    private static final String PROP_PLAN = "Plan";
    private static final String PROP_RESPONSE_STATUS = "Response status";
    private static final String PROP_LATENCY = "Latency in ms";
    private static final String PROP_QUOTA_COUNT = "Quota count";
    private static final String PROP_QUOTA_LIMIT = "Quota limit";
    private static final String PROP_QUOTA_PERCENT = "Quota percent";

    @Autowired
    private AlertEngineService alertEngineService;

    @Autowired
    private Node node;

    @Value("${http.port:8082}")
    private String port;

    @Override
    public void handle(ExecutionContext context) {
        try {
            if (alertEngineService != null) {
                final Event.Builder event = new Event.Builder()
                        .timestamp(context.request().timestamp())
                        .context(CONTEXT_GATEWAY, node.id())
                        .context(CONTEXT_HOSTNAME, node.hostname())
                        .context(CONTEXT_PORT, port)
                        .type(REQUEST_TYPE)
                        .prop(PROP_REQUEST_ID, context.request().id())
                        .prop(PROP_CONTEXT_PATH, context.getAttribute(ExecutionContext.ATTR_CONTEXT_PATH))
                        .prop(PROP_API, context.getAttribute(ExecutionContext.ATTR_API))
                        .prop(PROP_APPLICATION, context.getAttribute(ExecutionContext.ATTR_APPLICATION))
                        .prop(PROP_PLAN, context.getAttribute(ExecutionContext.ATTR_PLAN))
                        .prop(PROP_RESPONSE_STATUS, context.response().status())
                        .prop(PROP_LATENCY, context.request().metrics().getProxyLatencyMs());

                final Object tenant = node.metadata().get("tenant");
                if (tenant != null) {
                    event.context(CONTEXT_TENANT, (String) tenant);
                }

                final Long quotaCount = (Long) context.getAttribute(ExecutionContext.ATTR_QUOTA_COUNT);
                if (quotaCount != null) {
                    final Long quotaLimit = (Long) context.getAttribute(ExecutionContext.ATTR_QUOTA_LIMIT);
                    if (quotaLimit != null) {
                        event
                                .prop(PROP_QUOTA_COUNT, quotaCount)
                                .prop(PROP_QUOTA_LIMIT, quotaLimit)
                                .prop(PROP_QUOTA_PERCENT, Double.valueOf(quotaCount) / Double.valueOf(quotaLimit) * 100);
                    }
                }
                alertEngineService.send(event.build());
            }
        } catch (Exception ex) {
            LOGGER.error("An error occurs while sending alert", ex);
        }
         finally {
            next.handle(null);
        }
    }
}
