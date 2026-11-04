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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.notification.model.ApiNotificationTemplateData;
import io.gravitee.apim.core.notification.model.ApplicationNotificationTemplateData;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.ApiModel;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.notification.NotificationReferenceExtractor.NotificationReferences;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NotificationReferenceExtractorTest {

    /**
     * Every type passed today as {@code params["api"]}: a new type without a case here is not extracted.
     */
    static Stream<Arguments> apiParams() {
        var v2ApiEntity = new ApiEntity();
        v2ApiEntity.setId("v2-api-entity");
        var v4ApiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        v4ApiEntity.setId("v4-api-entity");
        var apiModel = new ApiModel();
        apiModel.setId("api-model");
        var v4ApiModel = new io.gravitee.rest.api.model.v4.api.ApiModel();
        v4ApiModel.setId("v4-api-model");
        var repositoryApi = new Api();
        repositoryApi.setId("repository-api");
        return Stream.of(
            Arguments.of(v2ApiEntity, "v2-api-entity"),
            Arguments.of(v4ApiEntity, "v4-api-entity"),
            Arguments.of(apiModel, "api-model"),
            Arguments.of(v4ApiModel, "v4-api-model"),
            Arguments.of(repositoryApi, "repository-api"),
            Arguments.of(ApiNotificationTemplateData.builder().id("template-api").build(), "template-api")
        );
    }

    /**
     * Every type passed today as {@code params["application"]}.
     */
    static Stream<Arguments> applicationParams() {
        var applicationEntity = new ApplicationEntity();
        applicationEntity.setId("application-entity");
        var baseApplicationEntity = new BaseApplicationEntity();
        baseApplicationEntity.setId("base-application-entity");
        return Stream.of(
            Arguments.of(applicationEntity, "application-entity"),
            Arguments.of(baseApplicationEntity, "base-application-entity"),
            Arguments.of(ApplicationNotificationTemplateData.builder().id("template-application").build(), "template-application")
        );
    }

    @ParameterizedTest
    @MethodSource("apiParams")
    void should_extract_api_id_from_params(Object api, String expectedId) {
        var references = NotificationReferenceExtractor.extract(Map.of("api", api), null, null);

        assertThat(references).isEqualTo(new NotificationReferences(expectedId, null));
    }

    @ParameterizedTest
    @MethodSource("applicationParams")
    void should_extract_application_id_from_params(Object application, String expectedId) {
        var references = NotificationReferenceExtractor.extract(Map.of("application", application), null, null);

        assertThat(references).isEqualTo(new NotificationReferences(null, expectedId));
    }

    @Test
    void should_extract_api_and_application() {
        var api = new ApiModel();
        api.setId("api-id");
        var application = new ApplicationEntity();
        application.setId("application-id");

        var references = NotificationReferenceExtractor.extract(
            Map.of("api", api, "application", application),
            NotificationReferenceType.APPLICATION,
            "application-id"
        );

        assertThat(references).isEqualTo(new NotificationReferences("api-id", "application-id"));
    }

    @Test
    void should_fall_back_to_api_reference_of_trigger() {
        var references = NotificationReferenceExtractor.extract(Map.of(), NotificationReferenceType.API, "api-id");

        assertThat(references).isEqualTo(new NotificationReferences("api-id", null));
    }

    @Test
    void should_fall_back_to_application_reference_of_trigger() {
        var references = NotificationReferenceExtractor.extract(Map.of(), NotificationReferenceType.APPLICATION, "application-id");

        assertThat(references).isEqualTo(new NotificationReferences(null, "application-id"));
    }

    @Test
    void should_ignore_other_reference_types() {
        var references = NotificationReferenceExtractor.extract(Map.of(), NotificationReferenceType.ENVIRONMENT, "environment-id");

        assertThat(references).isEqualTo(new NotificationReferences(null, null));
    }

    @Test
    void should_return_no_reference_for_unknown_param_type() {
        var params = new HashMap<String, Object>();
        params.put("api", "a string");
        params.put("application", 42);

        var references = NotificationReferenceExtractor.extract(params, null, null);

        assertThat(references).isEqualTo(new NotificationReferences(null, null));
    }

    @Test
    void should_return_no_reference_without_params() {
        assertThat(NotificationReferenceExtractor.extract(null, null, null)).isEqualTo(new NotificationReferences(null, null));
    }
}
