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
package io.gravitee.gateway.handlers.api.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.datatype.api.deser.ApiDeserializer;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.gateway.handlers.api.definition.Api;
import java.io.IOException;
import java.util.Date;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiDefinitionDeserializer extends ApiDeserializer<Api> {

    private static final long serialVersionUID = -7354218924791722617L;

    public ApiDefinitionDeserializer() {
        super(Api.class);
    }

    @Override
    public Api deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        final Api api = new Api();

        // Deserialize api.
        this.deserialize(jp, ctxt, api, node);

        JsonNode deployedAtNode = node.get("deployedAt");
        if (deployedAtNode != null) {
            api.setDeployedAt(new Date(deployedAtNode.asLong()));
        }

        JsonNode environmentIdNode = node.get("environmentId");
        if (environmentIdNode != null) {
            api.setEnvironmentId(environmentIdNode.asText());
        }

        JsonNode enabledNode = node.get("enabled");
        if (enabledNode != null) {
            api.setEnabled(enabledNode.asBoolean());
        }

        JsonNode definitionContextNode = node.get("definitionContext");
        if (definitionContextNode != null) {
            api.setDefinitionContext(definitionContextNode.traverse(jp.getCodec()).readValueAs(DefinitionContext.class));
        }

        return api;
    }
}
