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
package io.gravitee.rest.api.model;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * Managed keys for page documentation configuration
 *
 */
public final class PageConfigurationKeys {

    // Keys
    public static final String LINK_INHERIT = "inherit";
    public static final String LINK_IS_FOLDER = "isFolder";
    public static final String LINK_RESOURCE_TYPE = "resourceType";
    public static final String SWAGGER_SWAGGERUI_TRY_IT = "tryIt";
    public static final String SWAGGER_SWAGGERUI_TRY_IT_ANONYMOUS = "tryItAnonymous";
    public static final String SWAGGER_SWAGGERUI_TRY_IT_URL = "tryItURL";
    public static final String SWAGGER_SWAGGERUI_SHOW_URL = "showURL";
    public static final String SWAGGER_SWAGGERUI_DISPLAY_OPERATION_ID = "displayOperationId";
    public static final String SWAGGER_SWAGGERUI_DOC_EXPANSION = "docExpansion";
    public static final String SWAGGER_SWAGGERUI_ENABLE_FILTERING = "enableFiltering";
    public static final String SWAGGER_SWAGGERUI_SHOW_EXTENSIONS = "showExtensions";
    public static final String SWAGGER_SWAGGERUI_SHOW_COMMON_EXTENSIONS = "showCommonExtensions";
    public static final String SWAGGER_SWAGGERUI_MAX_DISPLAYED_TAGS = "maxDisplayedTags";
    public static final String SWAGGER_VIEWER = "viewer";
    public static final String TRANSLATION_LANG = "lang";
    public static final String TRANSLATION_INHERIT_CONTENT = "inheritContent";

    // Possibles Values
    public static final String LINK_RESOURCE_TYPE_EXTERNAL = "external";
    public static final String LINK_RESOURCE_TYPE_PAGE = "page";
    public static final String LINK_RESOURCE_TYPE_CATEGORY = "category";
}
