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
package io.gravitee.rest.api.management.v2.rest.utils;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.scoring.model.EnvironmentApiScoringReport;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.UriBuilder;

public final class ManagementApiLinkHelper {

    private ManagementApiLinkHelper() {}

    public static String apiPictureURL(UriBuilder baseUriBuilder, GenericApiEntity api) {
        return resourcesURL(
            baseUriBuilder,
            api.getId(),
            "apis",
            null,
            "picture",
            null != api.getUpdatedAt() ? api.getUpdatedAt().getTime() : null
        );
    }

    public static String apiBackgroundURL(UriBuilder baseUriBuilder, GenericApiEntity api) {
        return resourcesURL(
            baseUriBuilder,
            api.getId(),
            "apis",
            null,
            "background",
            null != api.getUpdatedAt() ? api.getUpdatedAt().getTime() : null
        );
    }

    public static String apiPictureURL(UriBuilder baseUriBuilder, Api api) {
        return resourcesURL(
            baseUriBuilder,
            api.getId(),
            "apis",
            null,
            "picture",
            null != api.getUpdatedAt() ? api.getUpdatedAt().toInstant().toEpochMilli() : null
        );
    }

    public static String apiBackgroundURL(UriBuilder baseUriBuilder, Api api) {
        return resourcesURL(
            baseUriBuilder,
            api.getId(),
            "apis",
            null,
            "background",
            null != api.getUpdatedAt() ? api.getUpdatedAt().toInstant().toEpochMilli() : null
        );
    }

    public static String apiPictureURL(UriBuilder baseUriBuilder, EnvironmentApiScoringReport report) {
        return resourcesURL(
            baseUriBuilder,
            report.api().apiId(),
            "apis",
            null,
            "picture",
            null != report.api().updatedAt() ? report.api().updatedAt().toInstant().toEpochMilli() : null
        );
    }

    private static String resourcesURL(
        UriBuilder baseUriBuilder,
        String resourceId,
        String resourceName,
        String subResourceId,
        String subResourceName,
        Object hash
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
        if (null != hash) {
            resourcesURLBuilder = resourcesURLBuilder.queryParam("hash", hash);
        }
        return resourcesURLBuilder.build().toString();
    }
}
