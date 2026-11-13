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
package io.gravitee.rest.api.service.exceptions;

import io.gravitee.common.http.HttpStatusCode;
import java.util.Collections;
import java.util.Map;

/**
 * An update of an agent application must carry its agent settings. An application's metadata is rebuilt from the
 * settings the payload sends, so a caller that knows nothing about agent applications — a generic settings screen
 * sending app or oauth settings — would erase the link to the agent it acts for and leave the AI catalog seeing an
 * agent with no application. Refused rather than silently repaired: the caller edited something it does not own.
 */
public class AgentSettingsRequiredException extends AbstractManagementException {

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return "An agent application is managed by the AI catalog and can only be updated with its agent settings";
    }

    @Override
    public String getTechnicalCode() {
        return "application.agentSettings.required";
    }

    @Override
    public Map<String, String> getParameters() {
        return Collections.emptyMap();
    }
}
