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
package io.gravitee.apim.core.audit.use_case;

import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.not;

import inmemory.AuditCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.common.utils.TimeProvider;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RemoveOldAuditDataUseCaseTest {

    private static final ZonedDateTime TOO_OLD = TimeProvider.now().minusHours(1);
    private static final ZonedDateTime NOT_TOO_OLD = TimeProvider.now();
    private static final String TARGETED_ENV = "DEFAULT";
    private static final Condition<AuditEntity> TOO_OLD_DATA = new Condition<>(e -> TOO_OLD.equals(e.getCreatedAt()), "too old");
    private static final Condition<AuditEntity> TARGETED_DATA = new Condition<>(
        e -> TARGETED_ENV.equals(e.getEnvironmentId()),
        "targeted env"
    );

    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    RemoveOldAuditDataUseCase sut = new RemoveOldAuditDataUseCase(auditCrudService);

    @AfterEach
    void tearDown() {
        auditCrudService.reset();
    }

    @Test
    void shouldRemoveOldDataOfEnvironment() {
        // Given
        var input = new RemoveOldAuditDataUseCase.Input(TARGETED_ENV, Duration.ofMinutes(5));

        for (var env : List.of(TARGETED_ENV, "envNoTargeted")) {
            for (var creationDate : List.of(TOO_OLD, NOT_TOO_OLD)) {
                auditCrudService.create(AuditEntity.builder().createdAt(creationDate).environmentId(env).build());
            }
        }

        // When
        sut.execute(input);

        // Then
        assertThat(auditCrudService.storage()).are(anyOf(not(TOO_OLD_DATA), not(TARGETED_DATA)));
    }
}
