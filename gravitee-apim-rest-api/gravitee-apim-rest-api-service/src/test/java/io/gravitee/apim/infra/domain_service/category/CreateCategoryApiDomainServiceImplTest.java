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
package io.gravitee.apim.infra.domain_service.category;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.rest.api.service.v4.ApiCategoryService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateCategoryApiDomainServiceImplTest {

    @Mock
    ApiCategoryService apiCategoryService;

    @InjectMocks
    CreateCategoryApiDomainServiceImpl cut;

    private static final String API_ID = "api-id";
    private static final String CATEGORY_ID = "category-id";

    @Test
    void addApiToCategories() {
        cut.addApiToCategories(API_ID, Set.of(CATEGORY_ID));

        verify(apiCategoryService, times(1)).addApiToCategories(API_ID, Set.of(CATEGORY_ID));
    }
}
