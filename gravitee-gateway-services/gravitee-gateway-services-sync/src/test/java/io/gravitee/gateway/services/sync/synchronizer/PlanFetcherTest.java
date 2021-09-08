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
package io.gravitee.gateway.services.sync.synchronizer;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.services.sync.builder.RepositoryApiBuilder;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanFetcherTest extends TestCase {

    @InjectMocks
    private PlanFetcher planFetcher = new PlanFetcher(100, Plan.Status.PUBLISHED, Plan.Status.DEPRECATED);

    @Mock
    private PlanRepository planRepository;

    @Test
    public void shouldFetchV1ApiPlans() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
            .id("api-test")
            .updatedAt(new Date())
            .definition("test")
            .build();

        Api mockApi = mockApi(api);
        mockApi.setDefinitionVersion(DefinitionVersion.V1);

        final Plan plan = new Plan();
        plan.setApi(mockApi.getId());
        when(planRepository.findByApis(anyList())).thenReturn(singletonList(plan));

        Flowable upstream = Flowable.create(
            emitter -> {
                emitter.onNext(mockApi);
                emitter.onComplete();
            },
            BackpressureStrategy.BUFFER
        );
        upstream.compose(planFetcher::fetchApiPlans).count().blockingGet();

        verify(planRepository, times(1)).findByApis(anyList());
    }

    @Test
    public void shouldFilterV1Plan() {
        io.gravitee.definition.model.Plan plan = mock(io.gravitee.definition.model.Plan.class);
        when(plan.getStatus()).thenReturn("published");
        assertTrue(planFetcher.filterPlan(plan));
    }

    @Test
    public void shouldFetchV2ApiPlans() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
            .id("api-test")
            .updatedAt(new Date())
            .definition("test")
            .build();

        Api mockApi = mockApi(api);

        final Plan plan = new Plan();
        plan.setApi(mockApi.getId());

        Flowable upstream = Flowable.create(
            emitter -> {
                emitter.onNext(mockApi);
                emitter.onComplete();
            },
            BackpressureStrategy.BUFFER
        );
        upstream.compose(planFetcher::fetchApiPlans).count().blockingGet();

        verify(planRepository, times(0)).findByApis(anyList());
    }

    @Test
    public void shouldFilterV2Plan() {
        Plan plan = mock(Plan.class);
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        assertTrue(planFetcher.filterPlan(plan));
    }

    public Api mockApi(final io.gravitee.repository.management.model.Api api) throws Exception {
        return mockApi(api, new String[] {});
    }

    public Api mockApi(final io.gravitee.repository.management.model.Api api, final String[] tags) throws Exception {
        final Api mockApi = new Api();
        mockApi.setId(api.getId());
        mockApi.setTags(new HashSet<>(Arrays.asList(tags)));
        mockApi.setDefinitionVersion(DefinitionVersion.V2);
        return mockApi;
    }
}
