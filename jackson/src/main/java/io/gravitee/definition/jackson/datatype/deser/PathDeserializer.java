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
package io.gravitee.definition.jackson.datatype.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.Method;
import io.gravitee.definition.model.Path;

import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class PathDeserializer extends StdScalarDeserializer<Path> {

    public PathDeserializer(Class<Path> vc) {
        super(vc);
    }

    @Override
    public Path deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Path pathDefinition = new Path();
        pathDefinition.setPath(jp.getParsingContext().getCurrentName());

        if (node.isArray()) {
            if (node.elements().hasNext()) {
                node.elements().forEachRemaining(jsonNode -> {
                    try {
                        Method spd = jsonNode.traverse(jp.getCodec()).readValueAs(Method.class);
                        pathDefinition.getMethods().add(spd);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                // register default handler /* with no policy
                pathDefinition.getMethods().add(new Method());
            }
        }

        return pathDefinition;
    }
}
