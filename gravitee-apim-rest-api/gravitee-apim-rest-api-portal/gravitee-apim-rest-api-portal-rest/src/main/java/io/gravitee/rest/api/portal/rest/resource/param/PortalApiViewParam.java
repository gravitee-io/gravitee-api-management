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
package io.gravitee.rest.api.portal.rest.resource.param;

/**
 * This parameter is used to control the visibility of APIs in the Next Gen Portal.
 *
 * Visibility rules take into account published state of the navigation item and public/private rules.
 *
 * The legacy API lifecycle PUBLISHED state is NOT taken into account.
 */
public final class PortalApiViewParam {

    private PortalApiViewParam() {}

    public static final String QUERY_PARAM_NAME = "view";

    public static final String DOCUMENTATION = "documentation";

    public static boolean isDocumentationView(String view) {
        return view != null && DOCUMENTATION.equalsIgnoreCase(view);
    }
}
