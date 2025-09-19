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
package fixtures.core.model;

import io.gravitee.apim.core.audit.model.AuditEntity;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Supplier;

public class AuditFixtures {

    private AuditFixtures() {}

    private static final Supplier<AuditEntity.AuditEntityBuilder> BASE = () ->
        AuditEntity.builder()
            .id("audit-id")
            .organizationId("organization-id")
            .environmentId("environment-id")
            .user("user-id")
            .event("event-1")
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .patch(
                """
                [{ "op": "add", "path": "/hello", "value": ["world"] }]"""
            );

    public static AuditEntity anApiAudit() {
        return BASE.get().referenceType(AuditEntity.AuditReferenceType.API).referenceId("api-id").properties(Map.of()).build();
    }
}
