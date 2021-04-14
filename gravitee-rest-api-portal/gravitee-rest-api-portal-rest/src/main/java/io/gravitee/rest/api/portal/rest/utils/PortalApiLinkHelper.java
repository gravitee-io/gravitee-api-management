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
package io.gravitee.rest.api.portal.rest.utils;

import io.gravitee.rest.api.service.common.GraviteeContext;
import javax.ws.rs.core.UriBuilder;

public final class PortalApiLinkHelper {

    private PortalApiLinkHelper() {}

    public static String apisURL(UriBuilder baseUriBuilder) {
        return resourcesURL(baseUriBuilder, null, "apis");
    }

    public static String apisURL(UriBuilder baseUriBuilder, String apiId) {
        return resourcesURL(baseUriBuilder, apiId, "apis");
    }

    public static String apiPagesURL(UriBuilder baseUriBuilder, String apiId) {
        return resourcesURL(baseUriBuilder, apiId, "apis", null, "pages");
    }

    public static String apiPagesURL(UriBuilder baseUriBuilder, String apiId, String pageId) {
        return resourcesURL(baseUriBuilder, apiId, "apis", pageId, "pages");
    }

    public static String applicationsURL(UriBuilder baseUriBuilder) {
        return resourcesURL(baseUriBuilder, null, "applications");
    }

    public static String applicationsURL(UriBuilder baseUriBuilder, String applicationId) {
        return resourcesURL(baseUriBuilder, applicationId, "applications");
    }

    public static String pagesURL(UriBuilder baseUriBuilder) {
        return resourcesURL(baseUriBuilder, null, "pages");
    }

    public static String pagesURL(UriBuilder baseUriBuilder, String pageId) {
        return resourcesURL(baseUriBuilder, pageId, "pages");
    }

    public static String themeURL(UriBuilder baseUriBuilder, String id) {
        return resourcesURL(baseUriBuilder, id, "theme");
    }

    public static String userURL(UriBuilder baseUriBuilder) {
        return resourcesURL(baseUriBuilder, null, "user");
    }

    public static String usersURL(UriBuilder baseUriBuilder) {
        return resourcesURL(baseUriBuilder, null, "users");
    }

    public static String usersURL(UriBuilder baseUriBuilder, String userId) {
        return resourcesURL(baseUriBuilder, userId, "users");
    }

    public static String categoriesURL(UriBuilder baseUriBuilder) {
        return resourcesURL(baseUriBuilder, null, "categories");
    }

    public static String categoriesURL(UriBuilder baseUriBuilder, String categoryId) {
        return resourcesURL(baseUriBuilder, categoryId, "categories");
    }

    private static String resourcesURL(UriBuilder baseUriBuilder, String resourceId, String resourceName) {
        return resourcesURL(baseUriBuilder, resourceId, resourceName, null, null);
    }

    private static String resourcesURL(
        UriBuilder baseUriBuilder,
        String resourceId,
        String resourceName,
        String subResourceId,
        String subResourceName
    ) {
        UriBuilder resourcesURLBuilder = baseUriBuilder
            .path("environments")
            .path(GraviteeContext.getCurrentEnvironment())
            .path(resourceName);
        if (resourceId != null && !resourceId.isEmpty()) {
            resourcesURLBuilder = resourcesURLBuilder.path(resourceId);
        }
        if (subResourceName != null && !subResourceName.isEmpty()) {
            resourcesURLBuilder = resourcesURLBuilder.path(subResourceName);
        }
        if (subResourceId != null && !subResourceId.isEmpty()) {
            resourcesURLBuilder = resourcesURLBuilder.path(subResourceId);
        }
        return resourcesURLBuilder.build().toString();
    }
}
