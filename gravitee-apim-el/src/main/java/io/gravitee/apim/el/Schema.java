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
package io.gravitee.apim.el;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.gateway.handlers.api.context.ApiProperties;
import io.gravitee.gateway.reactive.api.el.EvaluableRequest;
import io.gravitee.gateway.reactive.api.el.EvaluableResponse;
import io.gravitee.gateway.reactive.handlers.api.context.SubscriptionVariable;
import io.gravitee.gateway.reactor.handler.context.EvaluableExecutionContext;
import io.gravitee.gateway.reactor.handler.context.provider.NodeProperties;
import java.util.Map;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class Schema {

    public static final String FILE = "/io/gravitee/apim/el/schema.json";

    private Schema() {}

    @JsonProperty
    private EvaluableExecutionContext context;

    @JsonProperty
    private EvaluableRequest request;

    @JsonProperty
    private EvaluableResponse response;

    @JsonProperty
    private ApiProperties api;

    @JsonProperty
    private SubscriptionVariable subscription;

    @JsonProperty
    private NodeProperties node;

    @JsonProperty
    private Map<String, Map<String, String>> dictionaries;
}
