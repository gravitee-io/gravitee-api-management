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
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.valueOf;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StillPrimaryOwnerException extends AbstractManagementException {

    private final long apiCount;
    private final long applicationCount;
    private final ApiPrimaryOwnerMode primaryOwnerMode;

    public StillPrimaryOwnerException(long apiCount, long applicationCount) {
        this.apiCount = apiCount;
        this.applicationCount = applicationCount;
        this.primaryOwnerMode = ApiPrimaryOwnerMode.USER;
    }

    public StillPrimaryOwnerException(long apiCount, ApiPrimaryOwnerMode primaryOwnerMode) {
        this.apiCount = apiCount;
        this.applicationCount = 0;
        this.primaryOwnerMode = primaryOwnerMode;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        String message = "The " + primaryOwnerMode.name().toLowerCase() + " is still primary owner of '" + apiCount + "' APIs";
        if (ApiPrimaryOwnerMode.USER == primaryOwnerMode) {
            message += " and '" + applicationCount + "' Applications.";
        }
        return message;
    }

    @Override
    public String getTechnicalCode() {
        return primaryOwnerMode.name().toLowerCase() + ".notDeletable";
    }

    @Override
    public Map<String, String> getParameters() {
        return new HashMap<String, String>() {
            {
                put("apiCount", valueOf(apiCount));
                put("applicationCount", valueOf(applicationCount));
                put("primaryOwnerMode", primaryOwnerMode.name());
            }
        };
    }
}
