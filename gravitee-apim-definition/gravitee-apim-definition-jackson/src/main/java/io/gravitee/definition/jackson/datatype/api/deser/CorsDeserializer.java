/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import lombok.CustomLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class CorsDeserializer extends StdScalarDeserializer<Cors> {

    private static final Logger log = LoggerFactory.getLogger(CorsDeserializer.class);

    public CorsDeserializer(Class<Cors> vc) {
        super(vc);
    }

    @Override
    public Cors deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Cors cors = new Cors();

        JsonNode enabledNode = node.get("enabled");
        if (enabledNode != null) {
            boolean enabled = enabledNode.asBoolean(false);
            cors.setEnabled(enabled);
        }

        if (cors.isEnabled()) {
            handleCredentials(node, cors);

            handleOrigin(node, cors);
            handleHeaders(node, cors);
            handleMethods(node, cors);
            handleExposeHeaders(node, cors);

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

    private static void handleCredentials(JsonNode node, Cors cors) {
        JsonNode allowCredentialsNode = node.get("allowCredentials");
        if (allowCredentialsNode != null) {
            boolean allowCredentials = allowCredentialsNode.asBoolean(false);
            cors.setAccessControlAllowCredentials(allowCredentials);
        } else {
            cors.setAccessControlAllowCredentials(false);
        }
    }

    private static void handleOrigin(JsonNode node, Cors cors) {
        JsonNode allowOriginNode = node.get("allowOrigin");
        Set<String> allowOrigin = new LinkedCaseInsensitiveSet();
        Set<Pattern> allowOriginRegex = new LinkedHashSet<>();
        cors.setAccessControlAllowOrigin(allowOrigin);
        cors.setAccessControlAllowOriginRegex(allowOriginRegex);
        if (allowOriginNode != null) {
            allowOriginNode
                .elements()
                .forEachRemaining(jsonNode -> {
                    String origin = jsonNode.asText();
                    allowOrigin.add(origin);
                    if (!"*".equals(origin) && (origin.contains("(") || origin.contains("[") || origin.contains("*"))) {
                        try {
                            allowOriginRegex.add(Pattern.compile(origin));
                        } catch (PatternSyntaxException pse) {
                            log.error("Allow origin regex invalid: {} {}", origin, pse.getMessage());
                        }
                    }
                });
        }
    }

    private static void handleHeaders(JsonNode node, Cors cors) {
        JsonNode allowHeadersNode = node.get("allowHeaders");
        Set<String> allowHeaders = new LinkedCaseInsensitiveSet();
        cors.setAccessControlAllowHeaders(allowHeaders);
        if (allowHeadersNode != null) {
            allowHeadersNode.elements().forEachRemaining(jsonNode -> allowHeaders.add(jsonNode.asText()));
        }
    }

    private static void handleMethods(JsonNode node, Cors cors) {
        JsonNode allowMethodsNode = node.get("allowMethods");
        Set<String> allowMethods = new LinkedHashSet<>();
        cors.setAccessControlAllowMethods(allowMethods);
        if (allowMethodsNode != null) {
            allowMethodsNode.elements().forEachRemaining(jsonNode -> allowMethods.add(jsonNode.asText()));
        }
    }

    private static void handleExposeHeaders(JsonNode node, Cors cors) {
        JsonNode exposeHeadersNode = node.get("exposeHeaders");
        Set<String> exposeHeaders = new LinkedCaseInsensitiveSet();
        cors.setAccessControlExposeHeaders(exposeHeaders);
        if (exposeHeadersNode != null) {
            exposeHeadersNode.elements().forEachRemaining(jsonNode -> exposeHeaders.add(jsonNode.asText()));
        }
    }
}
