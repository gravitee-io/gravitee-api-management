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
package io.gravitee.definition.jackson.datatype.api;

import io.gravitee.definition.jackson.datatype.GraviteeModule;
import io.gravitee.definition.jackson.datatype.api.deser.*;
import io.gravitee.definition.jackson.datatype.api.ser.*;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.HttpResponse;
import io.gravitee.definition.model.debug.DebugApi;
import io.gravitee.definition.model.debug.DebugMetrics;
import io.gravitee.definition.model.debug.DebugStep;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugApiModule extends GraviteeModule {

    public DebugApiModule() {
        super("debug-apî");
        addDeserializer(DebugApi.class, new DebugApiDeserializer(DebugApi.class));
        addDeserializer(HttpRequest.class, new HttpRequestDeserializer(HttpRequest.class));
        addDeserializer(HttpResponse.class, new HttpResponseDeserializer(HttpResponse.class));
        addDeserializer(DebugStep.class, new DebugStepDeserializer(DebugStep.class));
        addDeserializer(DebugMetrics.class, new DebugMetricsDeserializer(DebugMetrics.class));

        addSerializer(DebugApi.class, new DebugApiSerializer(DebugApi.class));
        addSerializer(HttpRequest.class, new HttpRequestSerializer(HttpRequest.class));
        addSerializer(HttpResponse.class, new HttpResponseSerializer(HttpResponse.class));
        addSerializer(DebugStep.class, new DebugStepSerializer(DebugStep.class));
        addSerializer(DebugMetrics.class, new DebugMetricsSerializer(DebugMetrics.class));
    }
}
