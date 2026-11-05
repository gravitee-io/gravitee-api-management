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
package io.gravitee.gamma.rest.core.guardian.model;

import io.gravitee.apim.core.audit.model.event.AuditEvent;

/**
 * What a Guardian mutation records in the audit trail.
 *
 * <p>A plain enum rather than one implementing the platform's {@code AuditEvent}: the core layer may
 * not depend on {@code io.gravitee.apim.core.audit..} (AGENTS.md §5). {@code GuardianAuditAdapter}
 * maps each constant onto the platform event of the same name.
 *
 * @author GraviteeSource Team
 */
public enum GuardianAuditEvent implements AuditEvent {
    GUARDIAN_CREATED,
    GUARDIAN_UPDATED,
    GUARDIAN_DELETED,
}
