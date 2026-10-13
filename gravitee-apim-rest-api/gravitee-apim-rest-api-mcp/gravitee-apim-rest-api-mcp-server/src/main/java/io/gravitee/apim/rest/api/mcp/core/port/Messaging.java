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
package io.gravitee.apim.rest.api.mcp.core.port;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.mcp.core.model.Notification;
import java.util.List;

/**
 * APIM's own messaging: who a notification reaches, and sending it, audited under the caller's name.
 */
public interface Messaging {
    /** The people a notification would reach; empty for a webhook. */
    List<Recipient> recipients(AuditInfo caller, String apiId, Notification notification);

    /** @return how many recipients it went to */
    int send(AuditInfo caller, String apiId, Notification notification);

    record Recipient(String id, String name, String email) {}
}
