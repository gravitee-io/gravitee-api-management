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
package io.gravitee.apim.core.audit.model.event;

/**
 * The events of Human In the Lead on the environment's audit log: a rule or a saved notification changed, an approval
 * held, decided, ended or expired, a call that ran after it, and the notices sent about it. They are recorded against
 * the environment, and name the rule or the notification that changed in the {@code APPROVAL_RULE} or
 * {@code HITL_NOTIFICATION} audit property.
 */
public enum ApprovalAuditEvent implements AuditEvent {
    APPROVAL_RULE_CREATED,
    APPROVAL_RULE_UPDATED,
    APPROVAL_RULE_DELETED,
    APPROVAL_OPENED,
    APPROVAL_DECIDED,
    APPROVAL_ENDED,
    APPROVAL_EXPIRED,
    APPROVAL_RAN,
    HITL_NOTICE_GIVEN_UP,
    HITL_NOTICE_REFUSED,
    HITL_NOTICE_ATTEMPTED,
    HITL_NOTIFICATION_CREATED,
    HITL_NOTIFICATION_UPDATED,
    HITL_NOTIFICATION_DELETED,
}
