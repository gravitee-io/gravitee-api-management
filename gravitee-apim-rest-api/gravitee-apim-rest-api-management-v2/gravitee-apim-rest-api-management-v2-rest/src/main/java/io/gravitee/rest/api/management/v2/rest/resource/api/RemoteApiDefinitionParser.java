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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.ws.rs.BadRequestException;
import java.util.Set;
import lombok.CustomLog;

/**
 * Parses and validates a Gravitee {@link ExportApiV4} definition fetched from a remote URL.
 *
 * <p>Shared between {@code ApisResource} (create-from-URL) and {@code ApiResource} (update-from-URL).
 * The post-fetch parse + validate path is identical across both endpoints, so it lives here rather than
 * being duplicated in each resource. Registered as a Spring bean by {@code RestManagementConfiguration}
 * (and the test counterpart) — no {@code @Component} annotation, following the explicit-bean convention
 * used throughout the v2 rest module.
 */
@CustomLog
public class RemoteApiDefinitionParser {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final ObjectMapper objectMapper;

    public RemoteApiDefinitionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Deserializes the JSON content into an {@link ExportApiV4} and runs Bean Validation against it.
     *
     * <p>Bean Validation is run explicitly because the caller invokes the existing JSON import methods as
     * plain Java methods (not via JAX-RS dispatch), which would otherwise bypass {@code @Valid} constraints
     * on those endpoint parameters.
     *
     * @throws BadRequestException if the content is not parseable JSON, or if it is valid JSON that is not a
     *     Gravitee export (e.g. {@code {"hello":"world"}} deserializes into an {@link ExportApiV4} with a null
     *     {@code api}, which would otherwise NPE in the downstream mapper). The Jackson exception detail is
     *     logged at warn level but never returned to the caller, to avoid leaking internal class paths or
     *     fragments of the remote body through the error message.
     * @throws ConstraintViolationException if the parsed definition fails Bean Validation.
     */
    public ExportApiV4 parseAndValidate(String apiDefinitionContent) {
        ExportApiV4 apiToImport;
        try {
            apiToImport = objectMapper.readValue(apiDefinitionContent, ExportApiV4.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse API definition fetched from URL", e);
            throw new BadRequestException("Invalid API definition format");
        }
        if (apiToImport == null || apiToImport.getApi() == null) {
            log.warn("API definition fetched from URL is valid JSON but is missing the required 'api' object");
            throw new BadRequestException("Invalid API definition");
        }
        Set<ConstraintViolation<ExportApiV4>> violations = VALIDATOR.validate(apiToImport);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return apiToImport;
    }
}
