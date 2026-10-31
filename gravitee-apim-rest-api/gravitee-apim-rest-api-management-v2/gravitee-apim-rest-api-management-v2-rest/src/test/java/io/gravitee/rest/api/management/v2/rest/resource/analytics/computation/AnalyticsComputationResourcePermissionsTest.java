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
package io.gravitee.rest.api.management.v2.rest.resource.analytics.computation;

import static assertions.MAPIAssertions.assertThat;
import static fixtures.AnalyticsEngineFixtures.*;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoaderResolver;
import io.gravitee.apim.core.analytics_engine.domain_service.BucketNamesPostProcessor;
import io.gravitee.apim.core.analytics_engine.domain_service.QueryFilterTransformer;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsScope;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.repository.analytics.engine.api.result.FacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesResult;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.rest.api.management.v2.rest.resource.api.AbstractNonAdminApiResourceTest;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.ws.rs.client.Entity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Checks that {@link AnalyticsComputationResource} answers a non-admin user from the rows they read, whatever their
 * environment permissions.
 *
 * @author GraviteeSource Team
 */
class AnalyticsComputationResourcePermissionsTest extends AbstractNonAdminApiResourceTest {

    @Autowired
    AnalyticsRepository analyticsRepository;

    @Autowired
    QueryFilterTransformer queryFilterTransformer;

    @Autowired
    AnalyticsQueryContextLoaderResolver analyticsQueryContextLoader;

    @Autowired
    BucketNamesPostProcessor bucketNamesPostProcessor;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/analytics";
    }

    @BeforeEach
    void init2() {
        Mockito.reset(analyticsRepository, queryFilterTransformer, analyticsQueryContextLoader, bucketNamesPostProcessor);
    }

    private void givenNoEnvironmentPermissionNorMembership() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(membershipService.getMembershipsByMemberAndReference(any(), any(), any())).thenReturn(Set.of());
    }

    private void givenTheCallerReadsNoApi() {
        when(analyticsQueryContextLoader.load(any(), any(), any())).thenReturn(
            new AnalyticsQueryContext(
                null,
                new ExecutionContext(ORGANIZATION, ENVIRONMENT),
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Optional.empty()
            )
        );
        when(queryFilterTransformer.transform(any(AnalyticsQueryContext.class), any())).thenAnswer(inv -> inv.getArgument(1));
        when(analyticsRepository.searchHTTPMeasures(any(), any())).thenReturn(new MeasuresResult(List.of()));
        when(analyticsRepository.searchMessageMeasures(any(), any())).thenReturn(new MeasuresResult(List.of()));
        when(analyticsRepository.searchHTTPFacets(any(), any())).thenReturn(new FacetsResult(List.of()));
        when(analyticsRepository.searchHTTPTimeSeries(any(), any())).thenReturn(new TimeSeriesResult(List.of()));
        when(bucketNamesPostProcessor.mapBucketNames(any(), any(), any(FacetsResponse.class))).thenAnswer(inv -> inv.getArgument(2));
        when(bucketNamesPostProcessor.mapBucketNames(any(), any(), any(TimeSeriesResponse.class))).thenAnswer(inv -> inv.getArgument(2));
    }

    /** The rows decide what is counted: the aggregates of the APIs the caller reads, none here. */
    @Test
    void should_compute_measures_without_any_environment_permission() {
        givenNoEnvironmentPermissionNorMembership();
        givenTheCallerReadsNoApi();

        var response = rootTarget().path("measures").request().post(Entity.json(aCountMeasureRequest()));

        assertThat(response).hasStatus(OK_200);
        verify(analyticsQueryContextLoader).load(any(), eq(AnalyticsScope.MANAGEMENT), eq(ApiDataKind.AGGREGATES));
    }

    @Test
    void should_compute_facets_without_any_environment_permission() {
        givenNoEnvironmentPermissionNorMembership();
        givenTheCallerReadsNoApi();

        var response = rootTarget().path("facets").request().post(Entity.json(aRequestCountFacetRequest()));

        assertThat(response).hasStatus(OK_200);
        verify(analyticsQueryContextLoader).load(any(), eq(AnalyticsScope.MANAGEMENT), eq(ApiDataKind.AGGREGATES));
    }

    @Test
    void should_compute_time_series_without_any_environment_permission() {
        givenNoEnvironmentPermissionNorMembership();
        givenTheCallerReadsNoApi();

        var response = rootTarget().path("time-series").request().post(Entity.json(aRequestCountTimeSeries()));

        assertThat(response).hasStatus(OK_200);
        verify(analyticsQueryContextLoader).load(any(), eq(AnalyticsScope.MANAGEMENT), eq(ApiDataKind.AGGREGATES));
    }
}
