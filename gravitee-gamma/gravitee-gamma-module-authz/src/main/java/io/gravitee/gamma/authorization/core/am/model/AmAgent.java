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
package io.gravitee.gamma.authorization.core.am.model;

/**
 * An AM agent as the sync needs it, decoupled from the AM SDK so the core layer stays free of the
 * SDK dependency. Agents are AM applications of type AGENT; the infra {@code AmDirectoryClient}
 * adapter maps the SDK model onto this record. {@code clientId} is the agent's OAuth client_id and
 * keys the derived PRINCIPAL entity id. Nullable fields ({@code name}, {@code agentType}) reflect
 * what AM populated.
 */
public record AmAgent(String id, String clientId, String name, String agentType) {}
