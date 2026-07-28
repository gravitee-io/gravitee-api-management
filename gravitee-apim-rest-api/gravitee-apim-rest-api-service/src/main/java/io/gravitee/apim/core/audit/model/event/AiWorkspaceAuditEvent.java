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
 * Recorded against the workspace itself, which is stored as an API Product.
 *
 * <p>Creating, updating and deleting a workspace already produce {@link ApiProductAuditEvent}s, and
 * its tiers produce {@link PlanAuditEvent}s, so those are deliberately absent here. The events below
 * are the ones that otherwise write a different entity — models and components mutate the underlying
 * LLM proxy, and users mutate a subscription — leaving no trace on the workspace being administered.
 */
public enum AiWorkspaceAuditEvent implements AuditEvent {
    AI_WORKSPACE_MODEL_ADDED,
    AI_WORKSPACE_MODEL_REMOVED,
    AI_WORKSPACE_COMPONENT_ADDED,
    AI_WORKSPACE_COMPONENT_REMOVED,
    AI_WORKSPACE_USER_ADDED,
    AI_WORKSPACE_USER_REMOVED,
    AI_WORKSPACE_USER_TIER_CHANGED,
}
