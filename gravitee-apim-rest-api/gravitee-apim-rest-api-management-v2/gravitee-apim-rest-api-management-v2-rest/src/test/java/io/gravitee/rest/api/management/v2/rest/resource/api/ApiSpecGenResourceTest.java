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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.rest.api.service.common.UuidString.generateRandom;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.specgen.model.ApiSpecGenRequestState;
import io.gravitee.apim.core.specgen.model.ApiSpecGenState;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.reactivex.rxjava3.core.Single;
import jakarta.ws.rs.client.WebTarget;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSpecGenResourceTest extends AbstractResourceTest {

    String ORGANIZATION = "ORG";
    String ENVIRONMENT = "ENV";
    String API_ID = generateRandom();

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API_ID + "/spec-gen";
    }

    WebTarget getState;

    WebTarget postJob;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        getState = rootTarget().path("/_state");
        postJob = rootTarget().path("/_start");

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        final EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT);
        environment.setName(ENVIRONMENT);
        environment.setOrganizationId(ORGANIZATION);

        when(environmentService.findByOrgAndIdOrHrid(any(), any())).thenReturn(environment);
    }

    public static Stream<ApiSpecGenRequestState> params_that_must_return_state() {
        return Arrays.stream(ApiSpecGenRequestState.values());
    }

    @ParameterizedTest
    @MethodSource("params_that_must_return_state")
    public void must_test_get_state(ApiSpecGenRequestState state) {
        when(specGenRequestUseCase.getState(eq(API_ID), any())).thenReturn(Single.just(new ApiSpecGenState(state)));

        var response = getState.request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(io.gravitee.rest.api.management.v2.rest.model.ApiSpecGenRequestState.class)
            .extracting(requestState -> String.valueOf(requestState.getState()))
            .isEqualTo(state.name());
    }

    @ParameterizedTest
    @MethodSource("params_that_must_return_state")
    public void must_test_post_job(ApiSpecGenRequestState state) {
        when(specGenRequestUseCase.postJob(eq(API_ID), any())).thenReturn(Single.just(new ApiSpecGenState(state)));

        var response = postJob.request().post(null);

        assertThat(response)
            .hasStatus(200)
            .asEntity(io.gravitee.rest.api.management.v2.rest.model.ApiSpecGenRequestState.class)
            .extracting(requestState -> String.valueOf(requestState.getState()))
            .isEqualTo(state.name());
    }
}
