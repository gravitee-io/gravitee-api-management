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
package io.gravitee.rest.api.service.common;

import io.gravitee.apim.core.audit.model.AuditInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class IdBuilderTest {

    @Test
    void generate_should_generate_same_cross_id() {
        AuditInfo audit = AuditInfo.builder().organizationId("org").environmentId("env").build();
        Assertions.assertEquals(IdBuilder.builder(audit, "foo").buildCrossId(), IdBuilder.builder(audit, "foo").buildCrossId());
    }

    @Test
    void generate_should_generate_same_cross_id_with_audit_or_context() {
        AuditInfo audit = AuditInfo.builder().organizationId("org").environmentId("env").build();
        Assertions.assertEquals(IdBuilder.builder(audit, "foo").buildCrossId(), IdBuilder.builder(audit, "foo").buildCrossId());

        var execContext = new ExecutionContext("org", "env");
        Assertions.assertEquals(IdBuilder.builder(execContext, "foo").buildCrossId(), IdBuilder.builder(execContext, "foo").buildCrossId());

        Assertions.assertEquals(IdBuilder.builder(execContext, "foo").buildCrossId(), IdBuilder.builder(audit, "foo").buildCrossId());
    }

    @Test
    void generate_should_generate_different_cross_id() {
        AuditInfo audit = AuditInfo.builder().organizationId("org").environmentId("env").build();
        Assertions.assertNotEquals(IdBuilder.builder(audit, "foo").buildCrossId(), IdBuilder.builder(audit, "bar").buildCrossId());
    }

    @Test
    void generate_should_generate_same_id() {
        AuditInfo auditInfo = AuditInfo.builder().organizationId("org").environmentId("env").build();
        String firstUuid = IdBuilder.builder(auditInfo, "foo").buildId();
        String secondUuid = IdBuilder.builder(auditInfo, "foo").buildId();

        Assertions.assertEquals(firstUuid, secondUuid);

        firstUuid = IdBuilder.builder(auditInfo, "foo").withExtraId("bar").buildId();
        secondUuid = IdBuilder.builder(auditInfo, "foo").withExtraId("bar").buildId();

        Assertions.assertEquals(firstUuid, secondUuid);
    }

    @Test
    void generate_should_generate_different_id() {
        AuditInfo auditInfo = AuditInfo.builder().organizationId("org").environmentId("env").build();
        String firstUuid = IdBuilder.builder(auditInfo, "foo").buildId();
        String secondUuid = IdBuilder.builder(auditInfo, "bar").buildId();

        Assertions.assertNotEquals(firstUuid, secondUuid);

        firstUuid = IdBuilder.builder(auditInfo, "foo").withExtraId("baz").buildId();
        secondUuid = IdBuilder.builder(auditInfo, "bar").withExtraId("baz").buildId();

        Assertions.assertNotEquals(firstUuid, secondUuid);

        firstUuid = IdBuilder.builder(auditInfo, "foo").withExtraId("bar").buildId();
        secondUuid = IdBuilder.builder(auditInfo, "foo").withExtraId("baz").buildId();

        Assertions.assertNotEquals(firstUuid, secondUuid);
    }
}
