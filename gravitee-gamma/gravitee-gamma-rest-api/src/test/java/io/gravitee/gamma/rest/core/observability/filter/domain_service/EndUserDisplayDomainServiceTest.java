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
package io.gravitee.gamma.rest.core.observability.filter.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EndUserDisplayDomainServiceTest {

    private static final String DISPLAYING_ORG = "org-displaying";

    private final EndUserDisplayDomainService service = new EndUserDisplayDomainService(DISPLAYING_ORG::equals);

    @Test
    void should_refuse_the_end_user_field_when_the_organization_does_not_display_it() {
        assertThatThrownBy(() -> service.requireDisplayedIfNamed("org-hiding", Stream.of("API", "USER")))
            .isInstanceOfSatisfying(UnsupportedObservabilityFilterException.class, exception ->
                assertThat(exception.getTechnicalCode()).isEqualTo("observability.filter.end_user_not_displayed")
            )
            .hasMessageContaining("USER");
    }

    @Test
    void should_accept_the_end_user_field_when_the_organization_displays_it() {
        assertThatCode(() -> service.requireDisplayedIfNamed(DISPLAYING_ORG, Stream.of("USER"))).doesNotThrowAnyException();
    }

    @Test
    void should_accept_other_fields_when_the_organization_does_not_display_the_end_user() {
        assertThatCode(() -> service.requireDisplayedIfNamed("org-hiding", Stream.of("API", "CONSUMER_IP"))).doesNotThrowAnyException();
    }
}
