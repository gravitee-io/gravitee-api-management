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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.flow.Consumer;
import io.gravitee.definition.model.flow.ConsumerType;
import io.gravitee.definition.model.v4.listener.http.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathDeserializer extends StdScalarDeserializer<Path> {

    public PathDeserializer(Class<Path> vc) {
        super(vc);
    }

    @Override
    public Path deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Path path = new Path();

        JsonNode hostNode = node.get("host");
        if (hostNode != null) {
            path.setHost(hostNode.asText());
        }

        JsonNode pathNode = node.get("path");
        if (pathNode != null) {
            String pathStr = pathNode.asText();
            if (pathStr.endsWith("/")) {
                pathStr = pathStr.substring(0, pathStr.length() - 1);
            }
            path.setPath(pathStr);
        }

        JsonNode overrideAccessNode = node.get("overrideAccess");
        if (overrideAccessNode != null) {
            path.setOverrideAccess(overrideAccessNode.asBoolean());
        }

        return path;
    }
}
