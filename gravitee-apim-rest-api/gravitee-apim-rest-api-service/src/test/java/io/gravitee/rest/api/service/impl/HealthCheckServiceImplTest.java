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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.repository.healthcheck.api.HealthCheckRepository;
import io.gravitee.repository.healthcheck.query.FieldBucket;
import io.gravitee.repository.healthcheck.query.availability.AvailabilityQuery;
import io.gravitee.repository.healthcheck.query.availability.AvailabilityResponse;
import io.gravitee.rest.api.model.healthcheck.ApiMetrics;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.InstanceService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.*;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class HealthCheckServiceImplTest {

    @Mock
    private HealthCheckRepository healthCheckRepository;

    @Mock
    private ApiSearchService apiSearchService;

    @InjectMocks
    private HealthCheckServiceImpl healthCheckServiceImpl;

    @Test
    public void should_getAvailability_metadata() throws Exception {
        FieldBucket fieldBucketEndpoint = new FieldBucket("endpoint");
        fieldBucketEndpoint.setValues(Collections.emptyList());

        FieldBucket fieldBucketDeleted = new FieldBucket("deleted");
        fieldBucketDeleted.setValues(Collections.emptyList());

        AvailabilityResponse availabilityResponse = new AvailabilityResponse();
        availabilityResponse.setEndpointAvailabilities(List.of(fieldBucketEndpoint, fieldBucketDeleted));

        when(healthCheckRepository.query(any())).thenReturn(availabilityResponse);

        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");

        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setEndpoints(List.of(endpoint));

        ApiEntity api = new ApiEntity();
        api.setId("apiId");
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setEndpointGroups(List.of(endpointGroup));

        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), "apiId")).thenReturn(api);

        ApiMetrics apiMetrics = healthCheckServiceImpl.getAvailability(
            GraviteeContext.getExecutionContext(),
            "apiId",
            AvailabilityQuery.Field.ENDPOINT.name()
        );

        assertEquals(Map.of("target", "http"), apiMetrics.getMetadata().get("endpoint"));
        assertEquals(Map.of("deleted", "true"), apiMetrics.getMetadata().get("deleted"));
    }
}
