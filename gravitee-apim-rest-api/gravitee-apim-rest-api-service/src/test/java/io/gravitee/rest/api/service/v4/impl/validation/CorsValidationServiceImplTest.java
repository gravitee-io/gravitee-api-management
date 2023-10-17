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
package io.gravitee.rest.api.service.v4.impl.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.gravitee.definition.model.Cors;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.service.exceptions.AllowOriginNotAllowedException;
import io.gravitee.rest.api.service.v4.validation.CorsValidationService;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class CorsValidationServiceImplTest {

    private CorsValidationService corsValidationService;

    @BeforeEach
    public void setUp() {
        corsValidationService = new CorsValidationServiceImpl();
    }

    private static Stream<Arguments> invalidCorsOrigins() {
        return Stream.of(
            Arguments.of(
                Set.of(
                    "http://example.com",
                    "localhost", // Not allowed
                    "https://10.140.238.25:8080",
                    "(http|https)://[a-z]{6}.domain.[a-zA-Z]{2,6}",
                    ".*.company.com"
                )
            ),
            Arguments.of(Set.of("a{")),
            Arguments.of(Set.of("$some_env_var2"))
        );
    }

    @ParameterizedTest
    @MethodSource("invalidCorsOrigins")
    void shouldThrowWhenValidatingInvalidCorsOrigins(Set<String> origins) {
        Cors cors = new Cors();
        cors.setAccessControlAllowOrigin(origins);

        assertThrows(AllowOriginNotAllowedException.class, () -> corsValidationService.validateAndSanitize(cors));
    }

    @Test
    void shouldHaveAllowOriginWildcardAllowed() {
        Cors cors = new Cors();
        cors.setEnabled(true);
        cors.setAccessControlAllowOrigin(Collections.singleton("*"));
        Cors sanitizedCors = corsValidationService.validateAndSanitize(cors);

        assertThat(cors).isSameAs(sanitizedCors);
    }

    @Test
    void shouldHaveAllowOriginNullAllowed() throws TechnicalException {
        Cors cors = new Cors();
        cors.setEnabled(true);
        cors.setAccessControlAllowOrigin(Collections.singleton("null"));
        Cors sanitizedCors = corsValidationService.validateAndSanitize(cors);

        assertThat(cors).isSameAs(sanitizedCors);
    }
}
