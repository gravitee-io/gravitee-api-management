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
package io.gravitee.rest.api.service.notification;

public enum HitlHook implements Hook {
    HITL_APPROVAL_REQUIRED(
        "Approval required",
        "Email sent to the approvers when a tool call matched a rule and is waiting for a decision. Variables: agent, tool, proxy, rule, approvers, expiresAt, timeLeft, approvalUrl.",
        "HITL"
    ),
    HITL_APPROVAL_REMINDER(
        "Approval reminder",
        "Email sent to the approvers while a tool call still waits after the reminder interval. Variables: tool, agent, rule, expiresAt, timeLeft, approvalUrl.",
        "HITL"
    ),
    HITL_APPROVED(
        "Approved",
        "Email sent when an approver approved a tool call and a grant exists. Variables: tool, decidedAt, grant.kind, grant.minutes, grant.coversUntil.",
        "HITL"
    ),
    HITL_REJECTED("Rejected", "Email sent when an approver rejected a tool call. Variables: tool, decidedAt.", "HITL"),
    HITL_EXPIRED("Expired", "Email sent when the time to decide ended with no decision. Variables: tool, expiresAt, approvalUrl.", "HITL");

    private final String label;
    private final String description;
    private final String category;

    HitlHook(String label, String description, String category) {
        this.label = label;
        this.description = description;
        this.category = category;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public HookScope getScope() {
        return HookScope.HITL;
    }
}
