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
package io.gravitee.gateway.reactive.reactor.processor.metrics;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.HttpRequestInternal;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.gravitee.reporter.api.v4.metric.NoopMetrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class MetricsProcessor implements Processor {

    private final GatewayConfiguration gatewayConfiguration;
    private final boolean notFoundAnalyticsEnabled;

    public MetricsProcessor(final GatewayConfiguration gatewayConfiguration) {
        this(gatewayConfiguration, false);
    }

    @Override
    public String getId() {
        return "processor-metrics";
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> prepareMetrics(ctx, isAnalyticsEnabled(ctx)));
    }

    private void prepareMetrics(final HttpExecutionContextInternal ctx, final boolean enabled) {
        Metrics.MetricsBuilder<?, ?> metricsBuilder = Metrics.builder();
        if (enabled) {
            HttpRequestInternal request = ctx.request();
            if (request != null) {
                metricsBuilder
                    .timestamp(request.timestamp())
                    .requestId(request.id())
                    .transactionId(request.transactionId())
                    .clientIdentifier(request.clientIdentifier())
                    .httpMethod(request.method())
                    .localAddress(request.localAddress())
                    .remoteAddress(request.remoteAddress())
                    .host(request.host())
                    .uri(request.uri())
                    .pathInfo(request.pathInfo());
                if (request.headers() != null) {
                    metricsBuilder.userAgent(request.headers().get(HttpHeaderNames.USER_AGENT));
                }

                Flowable<Buffer> chunks = request.chunks();
                if (chunks != null) {
                    request.chunks(
                        request
                            .chunks()
                            .doOnNext(buffer ->
                                ctx.metrics().setRequestContentLength(ctx.metrics().getRequestContentLength() + buffer.length())
                            )
                    );
                }
            } else {
                metricsBuilder.timestamp(System.currentTimeMillis());
            }

            // Set gateway tenant
            gatewayConfiguration.tenant().ifPresent(metricsBuilder::tenant);

            // Set gateway zone
            gatewayConfiguration.zone().ifPresent(metricsBuilder::zone);
            ctx.metrics(metricsBuilder.enabled(true).build());
        } else {
            ctx.metrics(new NoopMetrics());
        }
    }

    private boolean isAnalyticsEnabled(final HttpExecutionContextInternal ctx) {
        ReactableApi<?> reactableApi = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API);
        if (reactableApi != null) {
            DefinitionVersion definitionVersion = reactableApi.getDefinitionVersion();
            if (definitionVersion == DefinitionVersion.V2) {
                return true;
            } else if (definitionVersion == DefinitionVersion.V4) {
                Api apiV4 = (Api) reactableApi.getDefinition();
                Analytics analytics = apiV4.getAnalytics();
                return analytics != null && analytics.isEnabled();
            }
        }
        return notFoundAnalyticsEnabled;
    }
}
