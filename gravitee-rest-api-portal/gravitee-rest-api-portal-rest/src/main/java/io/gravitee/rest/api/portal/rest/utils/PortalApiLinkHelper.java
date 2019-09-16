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

import javax.ws.rs.core.UriBuilder;

import io.gravitee.rest.api.service.common.GraviteeContext;

public final class PortalApiLinkHelper {
    
    private PortalApiLinkHelper() {}
    
    public static String apisURL(UriBuilder baseUriBuilder) {
        return resourcesURL(baseUriBuilder, null, "apis");
    }
    public static String apisURL(UriBuilder baseUriBuilder, String apiId) {
        return resourcesURL(baseUriBuilder, apiId, "apis");
    }

    public static String applicationsURL(UriBuilder baseUriBuilder) {
        return resourcesURL(baseUriBuilder, null, "applications");
    }
    public static String applicationsURL(UriBuilder baseUriBuilder, String applicationId) {
        return resourcesURL(baseUriBuilder, applicationId, "applications");
    }
    
    public static String viewsURL(UriBuilder baseUriBuilder) {
        return resourcesURL(baseUriBuilder, null, "views");
    }
    public static String viewsURL(UriBuilder baseUriBuilder, String viewId) {
        return resourcesURL(baseUriBuilder, viewId, "views");
    }
    
    private static String resourcesURL(UriBuilder baseUriBuilder, String resourceId, String resourceName) {
        UriBuilder resourcesURLBuilder = baseUriBuilder.path(GraviteeContext.getCurrentEnvironment()).path(resourceName);
        if(resourceId != null && !resourceId.isEmpty()) {
            resourcesURLBuilder = resourcesURLBuilder.path(resourceId);
        }
        return resourcesURLBuilder.build().toString();
    }
}
