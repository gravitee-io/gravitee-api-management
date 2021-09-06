/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.exceptions;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.model.InstallationStatus;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InstallationNotAcceptedException extends AbstractManagementException {

    private final String installationId;
    private final Map<String, String> parameters = new HashMap<>();

    public InstallationNotAcceptedException(InstallationEntity installationEntity, InstallationStatus status) {
        this.installationId = installationEntity.getId();
        parameters.put("installationId", installationEntity.getId());
        parameters.put("cockpitURL", installationEntity.getCockpitURL());
        parameters.put("status", status.name());
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return "Installation [" + installationId + "] not accepted.";
    }

    @Override
    public String getTechnicalCode() {
        return "installation.notAccepted";
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }
}
