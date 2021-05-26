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
package io.gravitee.definition.jackson.datatype.api.ser.endpoint;

import static java.lang.Boolean.FALSE;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.gravitee.definition.jackson.datatype.api.ser.EndpointSerializer;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpEndpointSerializer<T extends HttpEndpoint> extends EndpointSerializer<T> {

    public HttpEndpointSerializer(Class<T> t) {
        super(t);
    }

    @Override
    protected void doSerialize(T endpoint, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
        super.doSerialize(endpoint, jgen, serializerProvider);

        if (endpoint.getHealthCheck() != null) {
            jgen.writeObjectField("healthcheck", endpoint.getHealthCheck());
        }

        if (endpoint.getTenants() != null) {
            jgen.writeArrayFieldStart("tenants");
            endpoint
                .getTenants()
                .forEach(
                    tenant -> {
                        try {
                            jgen.writeString(tenant);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                );

            jgen.writeEndArray();
        }

        if (endpoint.getInherit() == null || endpoint.getInherit().equals(FALSE)) {
            HttpClientOptions options = (endpoint.getHttpClientOptions() != null)
                ? endpoint.getHttpClientOptions()
                : new HttpClientOptions();
            jgen.writeObjectField("http", options);

            if (endpoint.getHttpProxy() != null) {
                jgen.writeObjectField("proxy", endpoint.getHttpProxy());
            }

            if (endpoint.getHttpClientSslOptions() != null) {
                jgen.writeObjectField("ssl", endpoint.getHttpClientSslOptions());
            }

            if (endpoint.getHeaders() != null && !endpoint.getHeaders().isEmpty()) {
                jgen.writeObjectField("headers", endpoint.getHeaders());
            }
        }
    }
}
