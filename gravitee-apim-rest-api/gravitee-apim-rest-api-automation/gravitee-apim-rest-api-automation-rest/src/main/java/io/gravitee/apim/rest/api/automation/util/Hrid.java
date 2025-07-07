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
package io.gravitee.apim.rest.api.automation.util;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Hrid {

    public static String toId(AuditInfo auditInfo, @NotNull @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_-]{2,}$") String hrid) {
        try {
            UUID uuid = UUID.fromString(hrid);
            return uuid.toString();
        } catch (IllegalArgumentException iae) {
            return IdBuilder.builder(auditInfo, hrid).buildId();
        }
    }

    public static String toId(ExecutionContext executionContext, @NotNull @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_-]{2,}$") String hrid) {
        try {
            UUID uuid = UUID.fromString(hrid);
            return uuid.toString();
        } catch (IllegalArgumentException iae) {
            return IdBuilder.builder(executionContext, hrid).buildId();
        }
    }

    public static String toCrossId(AuditInfo auditInfo, @NotNull @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_-]{2,}$") String hrid) {
        try {
            UUID uuid = UUID.fromString(hrid);
            return uuid.toString();
        } catch (IllegalArgumentException iae) {
            return IdBuilder.builder(auditInfo, hrid).buildCrossId();
        }
    }

    public static String toCrossId(
        ExecutionContext executionContext,
        @NotNull @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_-]{2,}$") String hrid
    ) {
        try {
            UUID uuid = UUID.fromString(hrid);
            return uuid.toString();
        } catch (IllegalArgumentException iae) {
            return IdBuilder.builder(executionContext, hrid).buildCrossId();
        }
    }
}
