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
package io.gravitee.definition.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class HttpClientOptionsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldForceClearTextUpgradeToFalseForHttp11() {
        // A brand-new endpoint keeps the model defaults (HTTP_1_1 + clearTextUpgrade=true), which is
        // an invalid combination: h2c is an HTTP/2-only mechanism (APIM-14613).
        HttpClientOptions options = new HttpClientOptions();

        assertThat(options.getVersion()).isEqualTo(ProtocolVersion.HTTP_1_1);
        assertThat(options.isClearTextUpgrade()).isFalse();
    }

    @Test
    void shouldNotSerializeClearTextUpgradeAsTrueForHttp11() throws Exception {
        HttpClientOptions options = new HttpClientOptions();
        options.setClearTextUpgrade(true);
        options.setVersion(ProtocolVersion.HTTP_1_1);

        // Default (bean) serialization is the path that leaked the invalid combo in the exported definition.
        boolean clearTextUpgrade = objectMapper.readTree(objectMapper.writeValueAsString(options)).path("clearTextUpgrade").asBoolean();

        assertThat(clearTextUpgrade).isFalse();
    }

    @Test
    void shouldPreserveClearTextUpgradeForHttp2() {
        HttpClientOptions options = new HttpClientOptions();
        options.setVersion(ProtocolVersion.HTTP_2);
        options.setClearTextUpgrade(true);

        assertThat(options.isClearTextUpgrade()).isTrue();
    }
}
