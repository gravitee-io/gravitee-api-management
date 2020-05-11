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
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationGrantTypesNotAllowedException extends AbstractManagementException {

    private final String applicationType;
    private final List<String> targetGrantTypes;

    public ApplicationGrantTypesNotAllowedException(String applicationType, List<String> targetGrantTypes) {
        this.applicationType = applicationType;
        this.targetGrantTypes = targetGrantTypes;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getTechnicalCode() {
        return "application.grantTypesNotAllowed";
    }

    @Override
    public Map<String, String> getParameters() {
        return new HashMap<String, String>() {
            {
                put("applicationType", applicationType);
                put("targetGrantTypes", StringUtils.join(targetGrantTypes, "|"));
            }
        };
    }
}
