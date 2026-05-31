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
package io.gravitee.rest.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.quality.ApiQualityMetricCategories;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ApiQualityMetricCategoriesTest {

    @InjectMocks
    private ApiQualityMetricCategories srv = new ApiQualityMetricCategories();

    @Test
    public void shouldBeValidWithCategory() {
        ApiEntity api = mock(ApiEntity.class);
        when(api.getCategories()).thenReturn(Collections.singleton("category"));

        boolean valid = srv.isValid(GraviteeContext.getExecutionContext(), api);

        assertTrue(valid);
    }

    @Test
    public void shouldNotBeValidWithEmptyCategories() {
        ApiEntity api = mock(ApiEntity.class);
        when(api.getCategories()).thenReturn(Collections.emptySet());

        boolean valid = srv.isValid(GraviteeContext.getExecutionContext(), api);

        assertFalse(valid);
    }

    @Test
    public void shouldNotBeValidWithNull() {
        ApiEntity api = mock(ApiEntity.class);
        when(api.getCategories()).thenReturn(null);

        boolean valid = srv.isValid(GraviteeContext.getExecutionContext(), api);

        assertFalse(valid);
    }
}
