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
import io.gravitee.common.util.Maps;
import java.util.Map;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageUsedByCategoryException extends AbstractManagementException {

    private String action;
    private String categoriesName;
    private String pageName;
    private String pageId;

    public PageUsedByCategoryException(String pageId, String pageName, String action, String categoriesName) {
        this.action = action;
        this.categoriesName = categoriesName;
        this.pageName = pageName;
        this.pageId = pageId;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return "Unable to " + action + " the page. Categories '" + categoriesName + "' use it.";
    }

    @Override
    public String getTechnicalCode() {
        return "page.used_by_category";
    }

    @Override
    public Map<String, String> getParameters() {
        return Maps
            .<String, String>builder()
            .put("action", action)
            .put("categoriesName", categoriesName)
            .put("pageName", pageName)
            .put("pageId", pageId)
            .build();
    }
}
