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
package io.gravitee.gateway.reactive.reactor.processor.alert;

import io.gravitee.alert.api.event.Event;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.reactivex.rxjava3.core.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertProcessor.class);

    private static final String REQUEST_TYPE = "REQUEST";

    private static final String PROCESSOR_GEOIP = "geoip";
    private static final String PROCESSOR_USERAGENT = "useragent";

    private static final String CONTEXT_NODE_ID = "node.id";
    private static final String CONTEXT_NODE_HOSTNAME = "node.hostname";
    private static final String CONTEXT_NODE_APPLICATION = "node.application";

    private static final String CONTEXT_GATEWAY_PORT = "gateway.port";

    private static final String PROP_TENANT = "tenant";

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

    public AlertProcessor(final AlertEventProducer eventProducer, final Node node, final String port) {
        this.eventProducer = eventProducer;
        this.node = node;
        this.port = port;
    }

    @Override
    public String getId() {
        return "processor-alert";
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable
            .fromRunnable(() ->
                eventProducer.send(
                    Event
                        .at(ctx.request().timestamp())
                        .type(REQUEST_TYPE)
                        .context(CONTEXT_NODE_ID, node.id())
                        .context(CONTEXT_NODE_HOSTNAME, node.hostname())
                        .context(CONTEXT_NODE_APPLICATION, node.application())
                        .context(CONTEXT_GATEWAY_PORT, port)
                        .context(PROCESSOR_GEOIP, PROP_REQUEST_IP)
                        .context(PROCESSOR_USERAGENT, PROP_REQUEST_USER_AGENT)
                        .property(PROP_TENANT, () -> node.metadata().get("tenant"))
                        .property(PROP_REQUEST_ID, ctx.request().id())
                        .property(PROP_REQUEST_USER_AGENT, ctx.metrics().getUserAgent())
                        .property(PROP_REQUEST_CONTENT_LENGTH, ctx.metrics().getRequestContentLength())
                        .property(PROP_REQUEST_IP, ctx.metrics().getRemoteAddress())
                        .property(PROP_API_ID, ctx.<String>getAttribute(ContextAttributes.ATTR_API))
                        .property(PROP_APPLICATION_ID, ctx.<String>getAttribute(ContextAttributes.ATTR_APPLICATION))
                        .property(PROP_PLAN_ID, ctx.<String>getAttribute(ContextAttributes.ATTR_PLAN))
                        .property(PROP_RESPONSE_STATUS, ctx.response().status())
                        .property(PROP_RESPONSE_LATENCY, ctx.metrics().getGatewayLatencyMs())
                        .property(PROP_RESPONSE_RESPONSE_TIME, ctx.metrics().getGatewayResponseTimeMs())
                        .property(PROP_RESPONSE_UPSTREAM_RESPONSE_TIME, ctx.metrics().getEndpointResponseTimeMs())
                        .property(PROP_RESPONSE_CONTENT_LENGTH, ctx.metrics().getResponseContentLength())
                        .property(PROP_USER_ID, ctx.metrics().getUser())
                        .property(PROP_QUOTA_COUNTER, ctx.<String>getAttribute(ContextAttributes.ATTR_QUOTA_COUNT))
                        .property(PROP_QUOTA_LIMIT, ctx.<String>getAttribute(ContextAttributes.ATTR_QUOTA_LIMIT))
                        .property(PROP_ERROR_KEY, ctx.metrics().getErrorKey())
                        .organization(ctx.getAttribute(ContextAttributes.ATTR_ORGANIZATION))
                        .environment(ctx.getAttribute(ContextAttributes.ATTR_ENVIRONMENT))
                        .installation((String) node.metadata().get(Node.META_INSTALLATION))
                        .build()
                )
            )
            .doOnError(throwable -> LOGGER.error("An error occurs while sending alert", throwable))
            .onErrorComplete();
    }
}
