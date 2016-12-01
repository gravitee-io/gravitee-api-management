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
import io.gravitee.definition.jackson.datatype.services.healthcheck.deser.HealthCheckDeserializer;
import io.gravitee.definition.jackson.datatype.services.healthcheck.deser.RequestDeserializer;
import io.gravitee.definition.jackson.datatype.services.healthcheck.deser.ExpectationDeserializer;
import io.gravitee.definition.jackson.datatype.services.healthcheck.ser.RequestSerializer;
import io.gravitee.definition.jackson.datatype.services.healthcheck.ser.ExpectationSerializer;
import io.gravitee.definition.jackson.datatype.services.healthcheck.ser.HealthCheckSerializer;
import io.gravitee.definition.model.services.healthcheck.HealthCheck;
import io.gravitee.definition.model.services.healthcheck.Request;
import io.gravitee.definition.model.services.healthcheck.Expectation;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthCheckModule extends GraviteeModule {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public HealthCheckModule() {
        super(HealthCheck.SERVICE_KEY);

        // first deserializers
        addDeserializer(HealthCheck.class, new HealthCheckDeserializer(HealthCheck.class));
        addDeserializer(Request.class, new RequestDeserializer(Request.class));
        addDeserializer(Expectation.class, new ExpectationDeserializer(Expectation.class));

        // then serializers:
        addSerializer(HealthCheck.class, new HealthCheckSerializer(HealthCheck.class));
        addSerializer(Request.class, new RequestSerializer(Request.class));
        addSerializer(Expectation.class, new ExpectationSerializer(Expectation.class));
    }
}