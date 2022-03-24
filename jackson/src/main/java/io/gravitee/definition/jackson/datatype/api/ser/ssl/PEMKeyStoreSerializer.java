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
package io.gravitee.definition.jackson.datatype.api.ser.ssl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.gravitee.definition.model.ssl.pem.PEMKeyStore;

import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PEMKeyStoreSerializer extends KeyStoreSerializer<PEMKeyStore>  {

    public PEMKeyStoreSerializer(Class<PEMKeyStore> t) {
        super(t);
    }

    @Override
    protected void doSerialize(PEMKeyStore keyStore, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
        super.doSerialize(keyStore, jgen, serializerProvider);

        writeStringField(jgen, "certPath", keyStore.getCertPath());
        writeStringField(jgen, "certContent", keyStore.getCertContent());
        writeStringField(jgen, "keyPath", keyStore.getKeyPath());
        writeStringField(jgen, "keyContent", keyStore.getKeyContent());
    }
}
