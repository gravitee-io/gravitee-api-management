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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InitializerOrder {

    private InitializerOrder() {}

    public static final int COCKPIT_ID_INITIALIZER = 200;
    public static final int DEFAULT_API_HEADER_INITIALIZER = 300;
    public static final int DEFAULT_CATEGORY_INITIALIZER = 200;
    public static final int DEFAULT_DASHBOARDS_INITIALIZER = 100;
    public static final int DEFAULT_METADATA_INITIALIZER = 100;
    public static final int DEFAULT_ORGANIZATION_ADMIN_ROLE_INITIALIZER = 130;
    public static final int DEFAULT_PAGE_REVISION_INITIALIZER = 200;
    public static final int DEFAULT_PARAMETER_INITIALIZER = 200;
    public static final int DEFAULT_THEME_INITIALIZER = 400;
    public static final int DEFAULT_USER_STATUS_INITIALIZER = 200;
    public static final int DOCUMENTATION_SYSTEM_FOLDER_INITIALIZER = 100;
    public static final int IDENTITY_PROVIDER_ACTIVATION_INITIALIZER = 400;
    public static final int IDENTITY_PROVIDER_INITIALIZER = 350;
    public static final int SEARCH_INDEX_INITIALIZER = 250;
}
