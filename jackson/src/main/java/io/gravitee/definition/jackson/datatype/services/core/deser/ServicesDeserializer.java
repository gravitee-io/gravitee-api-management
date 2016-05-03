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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.Service;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.healthcheck.HealthCheck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class ServicesDeserializer extends StdScalarDeserializer<Services> {

    private final Map<String, Class<? extends Service>> registeredServices = new HashMap<>();
    {
        registeredServices.put(HealthCheck.SERVICE_KEY, HealthCheck.class);
    }

    public ServicesDeserializer(Class<Services> vc) {
        super(vc);
    }

    public ServicesDeserializer() {
        super(Services.class);
    }

    @Override
    public Services deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Services services = new Services();

        List<Service> individualServices = new ArrayList<>();

        node.fields().forEachRemaining(jsonNode -> {
            try {
                JsonNode serviceNode = node.findParent(jsonNode.getKey());
                Service service = serviceNode.traverse(jp.getCodec()).readValueAs(Service.class);
                if (service != null) {
                    individualServices.add(service);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        services.set(individualServices);

        return services;

/*
        String serviceName = node.fieldNames().next();

        Class<? extends Service> serviceClass = registeredServices.get(serviceName);
        if (serviceClass == null) {
            return null;
        }

        return node.elements().next().traverse(jp.getCodec()).readValueAs(new TypeReference<Service>() {
            @Override
            public Type getType() {
                return serviceClass;
            }
        });
*/
    }
}
