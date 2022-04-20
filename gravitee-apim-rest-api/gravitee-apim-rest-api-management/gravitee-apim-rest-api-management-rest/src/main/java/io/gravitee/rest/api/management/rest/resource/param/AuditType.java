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
package io.gravitee.rest.api.management.rest.resource.param;

import static java.util.Map.entry;

import io.gravitee.rest.api.model.audit.AuditReferenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(enumAsRef = true)
public enum AuditType {
    ORGANIZATION,
    ENVIRONMENT,
    APPLICATION,
    API;

    private static final Map<AuditType, AuditReferenceType> AUDIT_TYPE_AUDIT_REFERENCE_TYPE_MAP = Map.ofEntries(
        entry(AuditType.ORGANIZATION, AuditReferenceType.ORGANIZATION),
        entry(AuditType.ENVIRONMENT, AuditReferenceType.ENVIRONMENT),
        entry(AuditType.APPLICATION, AuditReferenceType.APPLICATION),
        entry(AuditType.API, AuditReferenceType.API)
    );

    public static AuditReferenceType fromAuditType(AuditType auditType) {
        return AUDIT_TYPE_AUDIT_REFERENCE_TYPE_MAP.get(auditType);
    }
}
