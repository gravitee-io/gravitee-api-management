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
package inmemory;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractUseCaseTest {

    protected static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    protected static final String GENERATED_UUID = "generated-id";
    protected final String ORG_ID = "org-id";
    protected final String ENV_ID = "env-id";
    protected final String USER_ID = "user-id";
    protected final AuditInfo AUDIT_INFO = AuditInfo
        .builder()
        .organizationId(ORG_ID)
        .environmentId(ENV_ID)
        .actor(AuditActor.builder().userId(USER_ID).build())
        .build();

    protected final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    protected final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> GENERATED_UUID);
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }
}
