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
package io.gravitee.definition.jackson.datatype.api.deser.endpoint;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.ProtocolVersion;
import io.gravitee.definition.model.endpoint.GrpcEndpoint;

import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GrpcEndpointDeserializer extends HttpEndpointDeserializer<GrpcEndpoint> {
    public GrpcEndpointDeserializer(Class<GrpcEndpoint> vc) {
        super(vc);
    }

    @Override
    protected GrpcEndpoint deserialize(GrpcEndpoint endpoint, JsonNode node, DeserializationContext ctxt) throws IOException {
        GrpcEndpoint grpcEndpoint = super.deserialize(endpoint, node, ctxt);

        // Force HTTP_2
        if (endpoint.getHttpClientOptions() != null) {
            endpoint.getHttpClientOptions().setVersion(ProtocolVersion.HTTP_2);
        }

        return grpcEndpoint;
    }

    @Override
    protected GrpcEndpoint createEndpoint(String name, String target) {
        return new GrpcEndpoint(name, target);
    }
}
