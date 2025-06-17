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
package io.gravitee.gateway.reactive.debug.handlers.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.gravitee.gateway.debug.definition.DebugApi;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.core.context.AbstractRequest;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.debug.reactor.context.DebugExecutionContext;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.vertx.core.MultiMap;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DebugSyncApiReactorTest {

    private DebugSyncApiReactor debugSyncApiReactor;

    @BeforeEach
    public void beforeEach() {
        DebugApi debugApi = new DebugApi("uuid-12345", new io.gravitee.definition.model.debug.DebugApi());
        debugApi.setDeployedAt(Date.from(Instant.now()));
        debugSyncApiReactor = DebugSyncApiReactor.builder().api(debugApi).build();
    }

    @Nested
    class handleProcess {

        @Test
        public void path_with_subpath_and_trailing_slash() {
            MutableRequest request = new AbstractRequest() {
                {
                    MultiMap multiMap = HeadersMultiMap.headers();
                    this.headers = new VertxHttpHeaders(multiMap);
                    this.path = "/uuid-12345-test/v1/";
                    this.contextPath = "/uuid-12345-test/";
                    this.pathInfo = "/v1";
                }
            };
            MutableExecutionContext context = new DebugExecutionContext(request, null);
            context.metrics(Metrics.builder().build());
            debugSyncApiReactor.handleProcess(context);
            assertAll(
                () -> assertThat(context.request().path()).isEqualTo("/test/v1/"),
                () -> assertThat(context.request().contextPath()).isEqualTo("/test/"),
                () -> assertThat(context.request().pathInfo()).isEqualTo("/v1")
            );
        }

        @Test
        public void path_with_subpath_and_without_trailing_slash() {
            MutableRequest request = new AbstractRequest() {
                {
                    MultiMap multiMap = HeadersMultiMap.headers();
                    this.headers = new VertxHttpHeaders(multiMap);
                    this.path = "/uuid-12345-test/v1";
                    this.contextPath = "/uuid-12345-test/";
                    this.pathInfo = "/v1";
                }
            };
            MutableExecutionContext context = new DebugExecutionContext(request, null);
            context.metrics(Metrics.builder().build());
            debugSyncApiReactor.handleProcess(context);
            assertAll(
                () -> assertThat(context.request().path()).isEqualTo("/test/v1/"),
                () -> assertThat(context.request().contextPath()).isEqualTo("/test/"),
                () -> assertThat(context.request().pathInfo()).isEqualTo("/v1")
            );
        }
    }
}
