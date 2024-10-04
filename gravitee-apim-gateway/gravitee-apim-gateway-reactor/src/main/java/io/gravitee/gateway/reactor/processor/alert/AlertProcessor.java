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
package io.gravitee.gateway.reactor.processor.alert;

import io.gravitee.alert.api.event.Event;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.NodeMetadataResolver;
import io.gravitee.plugin.alert.AlertEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertProcessor extends AbstractProcessor<ExecutionContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertProcessor.class);

    private static final String REQUEST_TYPE = "REQUEST";

    private static final String PROCESSOR_GEOIP = "geoip";
    private static final String PROCESSOR_USERAGENT = "useragent";

    private static final String CONTEXT_NODE_ID = "node.id";
    private static final String CONTEXT_NODE_HOSTNAME = "node.hostname";
    private static final String CONTEXT_NODE_APPLICATION = "node.application";

    private static final String CONTEXT_GATEWAY_PORT = "gateway.port";

    private static final String PROP_TENANT = "tenant";
    private static final String PROP_ORGANIZATION = "organization";
    private static final String PROP_ENVIRONMENT = "environment";

    private static final String PROP_REQUEST_ID = "request.id";
    private static final String PROP_REQUEST_USER_AGENT = "request.user_agent";
    private static final String PROP_REQUEST_CONTENT_LENGTH = "request.content_length";
    private static final String PROP_REQUEST_IP = "request.ip";

    private static final String PROP_USER_ID = "user";
    private static final String PROP_API_ID = "api";
    private static final String PROP_APPLICATION_ID = "application";
    private static final String PROP_PLAN_ID = "plan";

    private static final String PROP_RESPONSE_STATUS = "response.status";
    private static final String PROP_RESPONSE_LATENCY = "response.latency";
    private static final String PROP_RESPONSE_RESPONSE_TIME = "response.response_time";
    private static final String PROP_RESPONSE_CONTENT_LENGTH = "response.content_length";
    private static final String PROP_RESPONSE_UPSTREAM_RESPONSE_TIME = "response.upstream_response_time";

    private static final String PROP_QUOTA_COUNTER = "quota.counter";
    private static final String PROP_QUOTA_LIMIT = "quota.limit";

    private static final String PROP_ERROR_KEY = "error.key";

    private final AlertEventProducer eventProducer;

    private final Node node;

    private final String port;

    public AlertProcessor(AlertEventProducer eventProducer, Node node, String port) {
        this.eventProducer = eventProducer;
        this.node = node;
        this.port = port;
    }

    @Override
    public void handle(ExecutionContext context) {
        try {
            eventProducer.send(
                Event
                    .at(context.request().timestamp())
                    .type(REQUEST_TYPE)
                    .context(CONTEXT_NODE_ID, node.id())
                    .context(CONTEXT_NODE_HOSTNAME, node.hostname())
                    .context(CONTEXT_NODE_APPLICATION, node.application())
                    .context(CONTEXT_GATEWAY_PORT, port)
                    .context(PROCESSOR_GEOIP, PROP_REQUEST_IP)
                    .context(PROCESSOR_USERAGENT, PROP_REQUEST_USER_AGENT)
                    .property(PROP_TENANT, () -> node.metadata().get("tenant"))
                    .property(PROP_REQUEST_ID, context.request().id())
                    .property(PROP_REQUEST_USER_AGENT, context.request().metrics().getUserAgent())
                    .property(PROP_REQUEST_CONTENT_LENGTH, context.request().metrics().getRequestContentLength())
                    .property(PROP_REQUEST_IP, context.request().metrics().getRemoteAddress())
                    .property(PROP_API_ID, context.getAttribute(ExecutionContext.ATTR_API))
                    .property(PROP_APPLICATION_ID, context.getAttribute(ExecutionContext.ATTR_APPLICATION))
                    .property(PROP_PLAN_ID, context.getAttribute(ExecutionContext.ATTR_PLAN))
                    .property(PROP_RESPONSE_STATUS, context.response().status())
                    .property(PROP_RESPONSE_LATENCY, context.request().metrics().getProxyLatencyMs())
                    .property(PROP_RESPONSE_RESPONSE_TIME, context.request().metrics().getProxyResponseTimeMs())
                    .property(PROP_RESPONSE_UPSTREAM_RESPONSE_TIME, context.request().metrics().getApiResponseTimeMs())
                    .property(PROP_RESPONSE_CONTENT_LENGTH, context.request().metrics().getResponseContentLength())
                    .property(PROP_USER_ID, context.request().metrics().getUser())
                    .property(PROP_QUOTA_COUNTER, context.getAttribute(ExecutionContext.ATTR_QUOTA_COUNT))
                    .property(PROP_QUOTA_LIMIT, context.getAttribute(ExecutionContext.ATTR_QUOTA_LIMIT))
                    .property(PROP_ERROR_KEY, context.request().metrics().getErrorKey())
                    .organization((String) context.getAttribute(ExecutionContext.ATTR_ORGANIZATION))
                    .environment((String) context.getAttribute(ExecutionContext.ATTR_ENVIRONMENT))
                    .installation((String) node.metadata().get(Node.META_INSTALLATION))
                    .build()
            );
        } catch (Exception ex) {
            LOGGER.error("An error occurs while sending alert", ex);
        } finally {
            next.handle(context);
        }
    }

    public AlertEventProducer getEventProducer() {
        return eventProducer;
    }

    public Node getNode() {
        return node;
    }

    public String getPort() {
        return port;
    }
}
