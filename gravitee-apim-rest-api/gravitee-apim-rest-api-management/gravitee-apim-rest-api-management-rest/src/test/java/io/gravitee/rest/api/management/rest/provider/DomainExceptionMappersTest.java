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
package io.gravitee.rest.api.management.rest.provider;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.exception.ConflictDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.rest.api.management.rest.model.ErrorEntity;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class DomainExceptionMappersTest {

    @Test
    public void conflict_domain_exception_maps_to_409_with_its_id() {
        var response = new ConflictDomainExceptionMapper().toResponse(new ConflictDomainException("Slug already used", "my-slug"));

        assertThat(response.getStatus()).isEqualTo(409);
        var body = (ErrorEntity) response.getEntity();
        assertThat(body.getMessage()).isEqualTo("Slug already used");
        assertThat(body.getParameters()).isEqualTo(Map.of("id", "my-slug"));
    }

    @Test
    public void conflict_domain_exception_without_id_maps_to_409() {
        var response = new ConflictDomainExceptionMapper().toResponse(new ConflictDomainException("Conflict"));

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(((ErrorEntity) response.getEntity()).getParameters()).isNull();
    }

    @Test
    public void validation_domain_exception_keeps_its_technical_code() {
        var response = new ValidationDomainExceptionMapper().toResponse(
            new ValidationDomainException("Credential rejected", "provider.credential.rejected")
        );

        assertThat(response.getStatus()).isEqualTo(400);
        var body = (ErrorEntity) response.getEntity();
        assertThat(body.getTechnicalCode()).isEqualTo("provider.credential.rejected");
        assertThat(body.getParameters()).isNull();
    }

    /**
     * The other half of the pass-through. The two-argument constructor above leaves {@code parameters} as an empty
     * map, so it only exercises the branch that yields null — inverting the guard, or dropping the argument, would
     * keep that test green while every 400 lost its parameters again.
     */
    @Test
    public void validation_domain_exception_forwards_its_parameters() {
        var response = new ValidationDomainExceptionMapper().toResponse(
            new ValidationDomainException("Invalid value", Map.of("location", "/endpointGroups"), "invalidValue")
        );

        assertThat(response.getStatus()).isEqualTo(400);
        var body = (ErrorEntity) response.getEntity();
        assertThat(body.getParameters()).isEqualTo(Map.of("location", "/endpointGroups"));
        assertThat(body.getTechnicalCode()).isEqualTo("invalidValue");
    }

    /** No code and no parameters: the response must gain neither field, which is what keeps existing 400s unchanged. */
    @Test
    public void validation_domain_exception_without_code_or_parameters_is_unchanged() {
        var response = new ValidationDomainExceptionMapper().toResponse(new ValidationDomainException("Plain failure"));

        assertThat(response.getStatus()).isEqualTo(400);
        var body = (ErrorEntity) response.getEntity();
        assertThat(body.getMessage()).isEqualTo("Plain failure");
        assertThat(body.getTechnicalCode()).isNull();
        assertThat(body.getParameters()).isNull();
    }
}
