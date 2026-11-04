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
package io.gravitee.rest.api.service.notification;

import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.PARAM_API;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.PARAM_APPLICATION;

import io.gravitee.apim.core.notification.model.ApiNotificationTemplateData;
import io.gravitee.apim.core.notification.model.ApplicationNotificationTemplateData;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiModel;
import java.util.Map;
import lombok.CustomLog;

/**
 * Finds the API and the application a portal notification is about, from the parameters of its template, falling
 * back to the reference the notification was triggered on.
 */
@CustomLog
public final class NotificationReferenceExtractor {

    private NotificationReferenceExtractor() {}

    public static NotificationReferences extract(Object params, NotificationReferenceType referenceType, String referenceId) {
        var paramsMap = params instanceof Map<?, ?> map ? map : Map.of();

        var apiId = apiIdOf(paramsMap.get(PARAM_API));
        if (apiId == null && referenceType == NotificationReferenceType.API) {
            apiId = referenceId;
        }
        var applicationId = applicationIdOf(paramsMap.get(PARAM_APPLICATION));
        if (applicationId == null && referenceType == NotificationReferenceType.APPLICATION) {
            applicationId = referenceId;
        }
        return new NotificationReferences(apiId, applicationId);
    }

    private static String apiIdOf(Object api) {
        return switch (api) {
            case null -> null;
            case GenericApiEntity entity -> entity.getId();
            case GenericApiModel model -> model.getId();
            case ApiNotificationTemplateData templateData -> templateData.getId();
            case Api repositoryApi -> repositoryApi.getId();
            default -> {
                log.debug("Cannot read the API id of a notification parameter of type {}", api.getClass().getName());
                yield null;
            }
        };
    }

    private static String applicationIdOf(Object application) {
        return switch (application) {
            case null -> null;
            case BaseApplicationEntity entity -> entity.getId();
            case ApplicationNotificationTemplateData templateData -> templateData.getId();
            default -> {
                log.debug("Cannot read the application id of a notification parameter of type {}", application.getClass().getName());
                yield null;
            }
        };
    }

    public record NotificationReferences(String apiId, String applicationId) {}
}
