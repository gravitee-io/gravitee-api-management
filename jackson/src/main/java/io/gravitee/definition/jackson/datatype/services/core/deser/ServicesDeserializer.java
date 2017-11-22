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
package io.gravitee.definition.jackson.datatype.services.core.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.Service;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ServicesDeserializer extends StdScalarDeserializer<Services> {

    private final Map<String, Class<? extends Service>> registeredServices = new HashMap<>();
    {
        registeredServices.put(HealthCheckService.SERVICE_KEY, HealthCheckService.class);
        registeredServices.put(DynamicPropertyService.SERVICE_KEY, DynamicPropertyService.class);
        registeredServices.put(EndpointDiscoveryService.SERVICE_KEY, EndpointDiscoveryService.class);
    }

    public ServicesDeserializer(Class<Services> vc) {
        super(vc);
    }

    @Override
    public Services deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Services services = new Services();
        List<Service> individualServices = new ArrayList<>();

        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            try {
                String serviceType = fieldNames.next();
                JsonNode serviceNode = node.findValue(serviceType);

                Class<? extends Service> serviceClass = registeredServices.get(serviceType);
                if (serviceClass != null) {
                    Service service = serviceNode.traverse(jp.getCodec()).readValueAs(new TypeReference<Service>() {
                        @Override
                        public Type getType() {
                            return serviceClass;
                        }
                    });

                    if (service != null) {
                        individualServices.add(service);
                    }
                }
            } catch (IOException ioe) {
                throw ctxt.mappingException(ioe.getMessage());
            }
        }

        services.set(individualServices);

        return services;
    }
}
