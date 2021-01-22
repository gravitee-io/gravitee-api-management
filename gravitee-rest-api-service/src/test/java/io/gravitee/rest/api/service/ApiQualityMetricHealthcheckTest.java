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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.quality.ApiQualityMetricHealthcheck;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiQualityMetricHealthcheckTest {

    @InjectMocks
    private ApiQualityMetricHealthcheck srv = new ApiQualityMetricHealthcheck();

    @Mock
    private ApiService apiService;

    @Test
    public void shouldBeValid() {
        when(apiService.hasHealthCheckEnabled(any(), eq(true))).thenReturn(true);
        boolean valid = srv.isValid(mock(ApiEntity.class));

        assertTrue(valid);
    }

    @Test
    public void shouldNotBeValid() {
        when(apiService.hasHealthCheckEnabled(any(), eq(true))).thenReturn(false);
        boolean valid = srv.isValid(mock(ApiEntity.class));

        assertFalse(valid);
    }
}
