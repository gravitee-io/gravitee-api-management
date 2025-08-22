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

import java.util.HashMap;
import java.util.Map;

public class PortalPageNotFoundException extends AbstractNotFoundException {

    private final String pageId;

    public PortalPageNotFoundException(String pageId) {
        this.pageId = pageId;
    }

    @Override
    public String getMessage() {
        return "Portal page not found: " + pageId;
    }

    @Override
    public String getTechnicalCode() {
        return "portalPage.notFound";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("pageId", pageId);
        return params;
    }
}
