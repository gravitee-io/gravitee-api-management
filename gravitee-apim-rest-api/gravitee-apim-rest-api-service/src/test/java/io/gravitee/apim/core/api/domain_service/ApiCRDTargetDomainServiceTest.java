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
package io.gravitee.apim.core.api.domain_service;

import static fixtures.core.model.ApiFixtures.aProxyApiV4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiCRDTargetDomainServiceTest {

    private static final String ENV_ID = "environment-id";

    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();

    private final ApiCRDTargetDomainService cut = new ApiCRDTargetDomainService(
        new ApiQueryServiceInMemory(apiCrudService),
        apiCrudService
    );

    @Test
    void should_find_the_api_by_cross_id() {
        var api = anApi("api-id", ENV_ID, "cross-id");
        apiCrudService.initWith(List.of(api));

        assertThat(cut.findExistingApi(ENV_ID, aSpec("another-id", "cross-id"))).contains(api);
    }

    @Test
    void should_find_nothing_when_the_cross_id_is_not_found_even_when_the_id_names_an_api_of_the_environment() {
        apiCrudService.initWith(List.of(anApi("api-id", ENV_ID, "cross-id")));

        assertThat(cut.findExistingApi(ENV_ID, aSpec("api-id", "unknown-cross-id"))).isEmpty();
    }

    @Test
    void should_find_the_api_named_by_the_id_when_the_spec_has_no_cross_id() {
        var apiWithoutCrossId = anApi("api-without-cross-id", ENV_ID, null);
        var named = anApi("api-id", ENV_ID, "cross-id");
        apiCrudService.initWith(List.of(apiWithoutCrossId, named));

        assertThat(cut.findExistingApi(ENV_ID, aSpec("api-id", null))).contains(named);
    }

    @Test
    void should_find_nothing_without_cross_id_nor_id_even_when_an_api_has_no_cross_id() {
        apiCrudService.initWith(List.of(anApi("api-without-cross-id", ENV_ID, null)));

        assertThat(cut.findExistingApi(ENV_ID, aSpec(null, null))).isEmpty();
    }

    @Test
    void should_find_nothing_when_no_api_has_the_id_and_the_spec_has_no_cross_id() {
        apiCrudService.initWith(List.of(anApi("api-without-cross-id", ENV_ID, null)));

        assertThat(cut.findExistingApi(ENV_ID, aSpec("unknown-id", null))).isEmpty();
    }

    @Test
    void should_refuse_an_id_that_names_an_api_of_another_environment() {
        apiCrudService.initWith(List.of(anApi("api-id", "other-environment-id", "cross-id")));

        assertThatExceptionOfType(ValidationDomainException.class)
            .isThrownBy(() -> cut.findExistingApi(ENV_ID, aSpec("api-id", "cross-id")))
            .withMessageContaining("[api-id]")
            .withMessageContaining("another environment");
    }

    @Test
    void should_refuse_an_id_that_names_an_api_of_another_environment_when_the_spec_has_no_cross_id() {
        apiCrudService.initWith(List.of(anApi("api-id", "other-environment-id", null)));

        assertThatExceptionOfType(ValidationDomainException.class).isThrownBy(() -> cut.findExistingApi(ENV_ID, aSpec("api-id", null)));
    }

    private static Api anApi(String id, String environmentId, String crossId) {
        return aProxyApiV4().toBuilder().id(id).environmentId(environmentId).crossId(crossId).build();
    }

    private static ApiCRDSpec aSpec(String id, String crossId) {
        return ApiCRDSpec.builder().id(id).crossId(crossId).build();
    }
}
