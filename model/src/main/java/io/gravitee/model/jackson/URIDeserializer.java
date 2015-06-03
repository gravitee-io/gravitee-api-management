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
package io.gravitee.model.jackson;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;

import java.net.URI;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class URIDeserializer extends FromStringDeserializer<URI> {
    public static final URIDeserializer instance = new URIDeserializer();

    public URIDeserializer() {
        super(URI.class);
    }

    protected URI _deserialize(String var1, DeserializationContext var2) throws IllegalArgumentException {
        return URI.create(var1);
    }

    protected URI _deserializeFromEmptyString() {
        return URI.create("");
    }
}