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
package io.gravitee.definition.jackson.datatype.api.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.common.util.LinkedCaseInsensitiveSet;
import io.gravitee.definition.model.Cors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CorsDeserializer extends StdScalarDeserializer<Cors> {

    private final Logger logger = LoggerFactory.getLogger(CorsDeserializer.class);

    public CorsDeserializer(Class<Cors> vc) {
        super(vc);
    }

    @Override
    public Cors deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Cors cors = new Cors();

        JsonNode enabledNode = node.get("enabled");
        if (enabledNode != null) {
            boolean enabled = enabledNode.asBoolean(false);
            cors.setEnabled(enabled);
        }

        if (cors.isEnabled()) {
            JsonNode allowCredentialsNode = node.get("allowCredentials");
            if (allowCredentialsNode != null) {
                boolean allowCredentials = allowCredentialsNode.asBoolean(false);
                cors.setAccessControlAllowCredentials(allowCredentials);
            } else {
                cors.setAccessControlAllowCredentials(false);
            }

            JsonNode allowOriginNode = node.get("allowOrigin");
            Set<String> allowOrigin = new LinkedCaseInsensitiveSet();
            Set<Pattern> allowOriginRegex = new HashSet<>();
            cors.setAccessControlAllowOrigin(allowOrigin);
            cors.setAccessControlAllowOriginRegex(allowOriginRegex);
            if (allowOriginNode != null) {
                allowOriginNode.elements().forEachRemaining(jsonNode -> {
                    allowOrigin.add(jsonNode.asText());
                    if (!"*".equals(jsonNode.asText()) && (
                            jsonNode.asText().contains("(") ||
                            jsonNode.asText().contains("[") ||
                            jsonNode.asText().contains("*"))) {
                        try {
                            allowOriginRegex.add(Pattern.compile(jsonNode.asText()));
                        } catch (PatternSyntaxException pse) {
                            logger.error("Allow origin regex invalid: " + jsonNode.asText(), pse.getMessage());
                        }
                    }
                });
            }

            JsonNode allowHeadersNode = node.get("allowHeaders");
            Set<String> allowHeaders = new LinkedCaseInsensitiveSet();
            cors.setAccessControlAllowHeaders(allowHeaders);
            if (allowHeadersNode != null) {
                allowHeadersNode.elements().forEachRemaining(jsonNode -> {
                    allowHeaders.add(jsonNode.asText());
                });
            }

            JsonNode allowMethodsNode = node.get("allowMethods");
            Set<String> allowMethods = new HashSet<>();
            cors.setAccessControlAllowMethods(allowMethods);
            if (allowMethodsNode != null) {
                allowMethodsNode.elements().forEachRemaining(jsonNode -> {
                    allowMethods.add(jsonNode.asText());
                });
            }

            JsonNode exposeHeadersNode = node.get("exposeHeaders");
            Set<String> exposeHeaders = new LinkedCaseInsensitiveSet();
            cors.setAccessControlExposeHeaders(exposeHeaders);
            if (exposeHeadersNode != null) {
                exposeHeadersNode.elements().forEachRemaining(jsonNode -> {
                    exposeHeaders.add(jsonNode.asText());
                });
            }

            JsonNode maxAgeNode = node.get("maxAge");
            if (maxAgeNode != null) {
                cors.setAccessControlMaxAge(maxAgeNode.asInt(-1));
            } else {
                cors.setAccessControlMaxAge(-1);
            }
            cors.setRunPolicies(node.path("runPolicies").asBoolean(false));
        }

        return cors;
    }
}
