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
import io.gravitee.rest.api.service.quality.ApiQualityMetricViews;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiQualityMetricViewsTest {
    @InjectMocks
    private ApiQualityMetricViews srv = new ApiQualityMetricViews();

    @Test
    public void shouldBeValidWithView() {
        ApiEntity api = mock(ApiEntity.class);
        when(api.getViews()).thenReturn(Collections.singleton("view"));

        boolean valid = srv.isValid(api);

        assertTrue(valid);
    }

    @Test
    public void shouldNotBeValidWithEmptyViews() {
        ApiEntity api = mock(ApiEntity.class);
        when(api.getViews()).thenReturn(Collections.emptySet());

        boolean valid = srv.isValid(api);

        assertFalse(valid);
    }

    @Test
    public void shouldNotBeValidWithNull() {
        ApiEntity api = mock(ApiEntity.class);
        when(api.getViews()).thenReturn(null);

        boolean valid = srv.isValid(api);

        assertFalse(valid);
    }
}
