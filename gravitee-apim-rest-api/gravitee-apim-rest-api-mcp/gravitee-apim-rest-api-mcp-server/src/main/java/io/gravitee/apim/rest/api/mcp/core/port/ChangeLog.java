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
import io.gravitee.apim.rest.api.mcp.core.model.Change;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import java.util.List;

/**
 * What was changed on an API in the control plane.
 */
public interface ChangeLog {
    /** Deployments, starts and stops of the API. */
    List<Change> deployments(AuditInfo caller, String apiId, TimeWindow window);

    /** Audit trail of the API: plans, subscriptions, API keys, members, definition. */
    List<Change> audit(AuditInfo caller, String apiId, TimeWindow window);
}
