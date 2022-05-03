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
package io.gravitee.definition.jackson.datatype.services.discovery;

import io.gravitee.definition.jackson.datatype.GraviteeModule;
import io.gravitee.definition.jackson.datatype.services.discovery.deser.EndpointDiscoveryDeserializer;
import io.gravitee.definition.jackson.datatype.services.discovery.ser.EndpointDiscoverySerializer;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;

public class EndpointDiscoveryModule extends GraviteeModule {

    private static final long serialVersionUID = 1L;

    public EndpointDiscoveryModule() {
        super(EndpointDiscoveryService.SERVICE_KEY);
        // first deserializers
        addDeserializer(EndpointDiscoveryService.class, new EndpointDiscoveryDeserializer(EndpointDiscoveryService.class));

        // then serializers:
        addSerializer(EndpointDiscoveryService.class, new EndpointDiscoverySerializer(EndpointDiscoveryService.class));
    }
}
