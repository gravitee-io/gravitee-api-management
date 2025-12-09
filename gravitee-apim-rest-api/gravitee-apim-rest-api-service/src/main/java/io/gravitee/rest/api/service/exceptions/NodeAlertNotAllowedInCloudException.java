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

import java.util.Map;

public class NodeAlertNotAllowedInCloudException extends AbstractManagementException {

    private final String triggerSource;

    public NodeAlertNotAllowedInCloudException(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    @Override
    public String getMessage() {
        return "NODE-based alert triggers are not allowed in cloud-hosted mode.";
    }

    @Override
    public int getHttpStatusCode() {
        return 400;
    }

    @Override
    public String getTechnicalCode() {
        return "alert.cloud.nodeNotAllowed";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of("triggerSource", triggerSource);
    }
}
