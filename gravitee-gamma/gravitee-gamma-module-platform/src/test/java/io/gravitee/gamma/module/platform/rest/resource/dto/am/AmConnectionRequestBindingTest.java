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
package io.gravitee.gamma.module.platform.rest.resource.dto.am;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.module.platform.rest.resource.dto.am.AmDtos.AmConnectionRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

// Guards the serialization + bean-validation layers the AmConnectionResource @InjectMocks wiring test
// can't reach: the original /_test drop-bug lived in Jackson binding, and @NotBlank baseUrl is only
// enforced on the save DTO. /_test deliberately takes the unannotated body (partial-body tolerance),
// so it has no DTO constraint to assert here.
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AmConnectionRequestBindingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        // ParameterMessageInterpolator avoids the optional jakarta.el dependency in unit tests.
        factory = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void deserializes_am_organization_id_from_the_request_body() throws Exception {
        var json = """
            {
              "baseUrl": "https://am.example",
              "serviceAccountAccessToken": "token",
              "amOrganizationId": "am-org",
              "environmentId": "env",
              "defaultDomainId": "dom",
              "defaultDomainHrid": "dom-hrid",
              "gatewayUrl": "https://gw"
            }
            """;

        var req = MAPPER.readValue(json, AmConnectionRequest.class);

        assertThat(req.amOrganizationId()).isEqualTo("am-org");
        assertThat(req.baseUrl()).isEqualTo("https://am.example");
        assertThat(req.serviceAccountAccessToken()).isEqualTo("token");
    }

    @Test
    void leaves_unset_fields_null_without_dropping_others() throws Exception {
        var req = MAPPER.readValue("{\"baseUrl\":\"https://am.example\",\"amOrganizationId\":\"am-org\"}", AmConnectionRequest.class);

        assertThat(req.baseUrl()).isEqualTo("https://am.example");
        assertThat(req.amOrganizationId()).isEqualTo("am-org");
        assertThat(req.serviceAccountAccessToken()).isNull();
        assertThat(req.environmentId()).isNull();
    }

    @Test
    void accepts_a_request_with_a_non_blank_base_url() {
        var req = new AmConnectionRequest("https://am.example", "token", "am-org", null, null, null, null);

        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    void rejects_a_blank_base_url() {
        var req = new AmConnectionRequest("  ", "token", "am-org", null, null, null, null);

        var violations = validator.validate(req);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("baseUrl");
    }
}
