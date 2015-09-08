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
package io.gravitee.gateway.core.definition.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.core.definition.PathDefinition;
import io.gravitee.gateway.core.definition.SubPathDefinition;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class SubPathDefinitionDeserializer extends JsonDeserializer<SubPathDefinition> {

    @Override
    public SubPathDefinition deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        SubPathDefinition pathDefinition = new SubPathDefinition();

        node.fieldNames().forEachRemaining(new Consumer<String>() {
            @Override
            public void accept(String field) {
                JsonNode subNode = node.findValue(field);

                switch(field) {
                    case "methods":
                        if (subNode == null) {
                            pathDefinition.setMethods(new HttpMethod[] {HttpMethod.GET, HttpMethod.POST, HttpMethod.CONNECT, HttpMethod.DELETE});
                        } else {

                        }
                        break;
                    default:
                        // We are in the case of a policy
                        break;
                }
            }
        });

        JsonNode nmode = node.findValue("methods");
        if (nmode == null) {
            pathDefinition.setMethods(new HttpMethod[] {HttpMethod.GET, HttpMethod.POST, HttpMethod.CONNECT, HttpMethod.DELETE});
        } else {

        }
        System.out.println(nmode);


        /*
        System.out.println("-----------------");
        System.out.println(node.toString());
        */

        return pathDefinition;
    }
}
