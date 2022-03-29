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
package io.gravitee.definition.jackson.datatype.api.deser.ssl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.datatype.api.deser.AbstractStdScalarDeserializer;
import io.gravitee.definition.model.ssl.KeyStore;
import io.gravitee.definition.model.ssl.jks.JKSKeyStore;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JKSKeyStoreDeserializer extends AbstractStdScalarDeserializer<JKSKeyStore> {

    public JKSKeyStoreDeserializer(Class<JKSKeyStore> vc) {
        super(vc);
    }

    @Override
    public JKSKeyStore deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        JKSKeyStore keyStore = new JKSKeyStore();

        keyStore.setContent(readStringValue(node, "content"));
        keyStore.setPath(readStringValue(node, "path"));
        keyStore.setPassword(readStringValue(node, "password"));

        return keyStore;
    }
}
