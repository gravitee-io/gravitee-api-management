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
package io.gravitee.definition.jackson.services.healthcheck;

import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.services.healthcheck.HealthCheckResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class HealthCheckResponseSerializerTest extends AbstractTest {

    @Test
    void testHealthResponseSerializer() throws IOException {
        HealthCheckResponse healthCheckResponse = new HealthCheckResponse();
        String expected = IOUtils
            .toString(
                read("/io/gravitee/definition/jackson/services/healtcheck/api-healthcheck-response-expected.json"),
                Charset.defaultCharset()
            )
            .trim();
        String given = objectMapper().writeValueAsString(healthCheckResponse);
        Assertions.assertEquals(expected, given);
    }
}
