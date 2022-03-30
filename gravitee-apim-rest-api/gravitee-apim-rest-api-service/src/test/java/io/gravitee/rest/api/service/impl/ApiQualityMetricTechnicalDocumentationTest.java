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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.quality.ApiQualityMetricTechnicalDocumentation;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiQualityMetricTechnicalDocumentationTest {

    @InjectMocks
    private ApiQualityMetricTechnicalDocumentation srv = new ApiQualityMetricTechnicalDocumentation();

    @Mock
    private PageService mockPageService;

    private static final String API_ID = "123";

    @Test
    public void shouldBeValidWithSwaggerPublished() {
        PageEntity item = mock(PageEntity.class);
        when(mockPageService.search(eq(GraviteeContext.getCurrentEnvironment()), any())).thenReturn(Collections.singletonList(item));
        ApiEntity api = mock(ApiEntity.class);
        when(api.getId()).thenReturn(API_ID);

        boolean valid = srv.isValid(GraviteeContext.getExecutionContext(), api);

        assertTrue(valid);
    }

    @Test
    public void shouldNotBeValidWithEmptyList() {
        when(mockPageService.search(eq(GraviteeContext.getCurrentEnvironment()), any())).thenReturn(Collections.emptyList());
        ApiEntity api = mock(ApiEntity.class);
        when(api.getId()).thenReturn(API_ID);

        boolean valid = srv.isValid(GraviteeContext.getExecutionContext(), api);

        assertFalse(valid);
    }
}
