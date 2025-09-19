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
import io.gravitee.common.util.Maps;
import io.gravitee.rest.api.model.PageType;
import java.util.Collections;
import java.util.Map;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 */
public class PageUsedAsGeneralConditionsException extends AbstractManagementException {

    private String action;
    private String planName;
    private String pageName;
    private String pageId;

    public PageUsedAsGeneralConditionsException(String pageId, String pageName, String action, String planName) {
        this.action = action;
        this.planName = planName;
        this.pageName = pageName;
        this.pageId = pageId;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return "Unable to " + action + " the page. Plan '" + planName + "' uses it as General Conditions.";
    }

    @Override
    public String getTechnicalCode() {
        return "page.used_as_general_condition";
    }

    @Override
    public Map<String, String> getParameters() {
        return Maps.<String, String>builder()
            .put("action", action)
            .put("planName", planName)
            .put("pageName", pageName)
            .put("pageId", pageId)
            .build();
    }
}
