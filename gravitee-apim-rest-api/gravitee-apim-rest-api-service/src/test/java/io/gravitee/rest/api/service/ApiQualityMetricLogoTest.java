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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.quality.ApiQualityMetricLogo;
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
public class ApiQualityMetricLogoTest {

    @InjectMocks
    private ApiQualityMetricLogo srv = new ApiQualityMetricLogo();

    @Mock
    private ApiService mockApiService;

    private static final String API_ID = "123";

    @Test
    public void shouldNotBeValidIfNull() {
        InlinePictureEntity apiPicture = mock(InlinePictureEntity.class);
        when(mockApiService.getPicture(API_ID)).thenReturn(apiPicture);
        ApiEntity api = mock(ApiEntity.class);
        when(api.getId()).thenReturn(API_ID);

        boolean valid = srv.isValid(api, GraviteeContext.getCurrentEnvironment());

        assertFalse(valid);
    }

    @Test
    public void shouldBeValidIfNotNull() {
        InlinePictureEntity apiPicture = mock(InlinePictureEntity.class);
        when(apiPicture.getContent()).thenReturn("abcd".getBytes());
        when(mockApiService.getPicture(API_ID)).thenReturn(apiPicture);
        ApiEntity api = mock(ApiEntity.class);
        when(api.getId()).thenReturn(API_ID);

        boolean valid = srv.isValid(api, GraviteeContext.getCurrentEnvironment());

        assertTrue(valid);
    }
}
