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
package io.gravitee.definition.jackson.datatype;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.definition.model.v4.ssl.SslOptions;
import io.gravitee.definition.model.v4.ssl.none.NoneKeyStore;
import io.gravitee.definition.model.v4.ssl.none.NoneTrustStore;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;

class SslOptionsMapperTest {

    GraviteeMapper mapper = new GraviteeMapper();

    @Nested
    class DeserializerTest {

        @ParameterizedTest
        @ValueSource(
            strings = {
                "{ \"trustStore\": { \"type\": \"NONE\" } }",
                "{ \"trustStore\": { \"type\": \"none\" } }",
                "{ \"trustStore\": { \"type\": \"\" } }",
            }
        )
        void should_deserialize_none_truststore(String json) throws JsonProcessingException {
            var result = mapper.readValue(json, SslOptions.class);

            assertThat(result).isInstanceOf(SslOptions.class).extracting(SslOptions::getTrustStore).isInstanceOf(NoneTrustStore.class);
        }

        @ParameterizedTest
        @ValueSource(
            strings = {
                "{ \"keyStore\": { \"type\": \"NONE\" } }",
                "{ \"keyStore\": { \"type\": \"none\" } }",
                "{ \"keyStore\": { \"type\": \"\" } }",
            }
        )
        void should_deserialize_none_keystore(String json) throws JsonProcessingException {
            var result = mapper.readValue(json, SslOptions.class);

            assertThat(result).isInstanceOf(SslOptions.class).extracting(SslOptions::getKeyStore).isInstanceOf(NoneKeyStore.class);
        }
    }

    @Nested
    class SslOptionsSerializerTest {

        @Test
        void should_serialize_none_truststore() throws Exception {
            SslOptions sslOptions = new SslOptions();
            sslOptions.setTrustStore(new NoneTrustStore());

            var result = mapper.writeValueAsString(sslOptions);

            JSONAssert.assertEquals("{\"hostnameVerifier\":true,\"trustAll\":false,\"trustStore\":{\"type\":\"NONE\"}}", result, true);
        }

        @Test
        void should_serialize_none_keystore() throws Exception {
            SslOptions sslOptions = new SslOptions();
            sslOptions.setKeyStore(new NoneKeyStore());

            var result = mapper.writeValueAsString(sslOptions);

            JSONAssert.assertEquals("{\"hostnameVerifier\":true,\"trustAll\":false,\"keyStore\":{\"type\":\"NONE\"}}", result, true);
        }
    }
}
