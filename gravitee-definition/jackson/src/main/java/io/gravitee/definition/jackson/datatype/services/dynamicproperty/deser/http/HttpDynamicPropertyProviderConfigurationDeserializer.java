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
package io.gravitee.definition.jackson.datatype.services.dynamicproperty.deser.http;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;

import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpDynamicPropertyProviderConfigurationDeserializer extends StdScalarDeserializer<HttpDynamicPropertyProviderConfiguration> {

    public HttpDynamicPropertyProviderConfigurationDeserializer(Class<HttpDynamicPropertyProviderConfiguration> vc) {
        super(vc);
    }

    @Override
    public HttpDynamicPropertyProviderConfiguration deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        HttpDynamicPropertyProviderConfiguration configuration = new HttpDynamicPropertyProviderConfiguration();

        final JsonNode urlNode = node.get("url");
        if (urlNode != null) {
            configuration.setUrl(urlNode.asText());
        } else {
            throw ctxt.mappingException("[dynamic-property] [HTTP] URL is required");
        }

        final JsonNode specificationNode = node.get("specification");
        if (specificationNode != null) {
            configuration.setSpecification(specificationNode.asText());
        } else {
            throw ctxt.mappingException("[dynamic-property] [HTTP] Specification is required");
        }

        final JsonNode useSystemProxy = node.get("useSystemProxy");
        if (useSystemProxy != null) {
            configuration.setUseSystemProxy(useSystemProxy.asBoolean());
        }

        return configuration;
    }
}