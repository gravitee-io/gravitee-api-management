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
package io.gravitee.definition.jackson.datatype.services.healthcheck;

import io.gravitee.definition.jackson.datatype.GraviteeModule;
import io.gravitee.definition.jackson.datatype.services.healthcheck.deser.*;
import io.gravitee.definition.jackson.datatype.services.healthcheck.ser.*;
import io.gravitee.definition.model.services.healthcheck.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthCheckModule extends GraviteeModule {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public HealthCheckModule() {
        super(HealthCheckService.SERVICE_KEY);
        // first deserializers
        addDeserializer(EndpointHealthCheckService.class, new EndpointHealthCheckDeserializer(EndpointHealthCheckService.class));
        addDeserializer(HealthCheckService.class, new HealthCheckDeserializer(HealthCheckService.class));
        addDeserializer(HealthCheckStep.class, new StepDeserializer(HealthCheckStep.class));
        addDeserializer(HealthCheckRequest.class, new RequestDeserializer(HealthCheckRequest.class));
        addDeserializer(HealthCheckResponse.class, new ResponseDeserializer(HealthCheckResponse.class));

        // then serializers:
        addSerializer(EndpointHealthCheckService.class, new EndpointHealthCheckSerializer(EndpointHealthCheckService.class));
        addSerializer(HealthCheckService.class, new HealthCheckSerializer(HealthCheckService.class));
        addSerializer(HealthCheckStep.class, new StepSerializer(HealthCheckStep.class));
        addSerializer(HealthCheckRequest.class, new RequestSerializer(HealthCheckRequest.class));
        addSerializer(HealthCheckResponse.class, new ResponseSerializer(HealthCheckResponse.class));
    }
}
