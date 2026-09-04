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
package io.gravitee.apim.rest.api.automation.exception.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal_page.exception.ConflictingNavigationItemStateException;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import jakarta.ws.rs.core.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ConflictDomainMapperTest {

    @Test
    void should_return_409_with_conflict_technical_code_for_conflicting_state() {
        var mapper = new ConflictDomainMapper();
        var exception = ConflictingNavigationItemStateException.descendantsMustBePrivateFirst("nav-id-42");

        var response = mapper.toResponse(exception);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getStatus()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
            var body = (Error) response.getEntity();
            soft.assertThat(body.getHttpStatus()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
            soft.assertThat(body.getTechnicalCode()).isEqualTo("conflict");
            soft.assertThat(body.getMessage()).contains("nav-id-42");
        });
    }

    @Test
    void validation_domain_mapper_still_returns_400_for_regression() {
        var mapper = new ValidationDomainMapper();
        var response = mapper.toResponse(new ValidationDomainException("boom") {});

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }
}
