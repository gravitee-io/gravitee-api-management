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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PageService_FindMaxPageOrderByApiTest {

    private static final String API_ID = "myAPI";

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Test
    public void shouldFindMaxPageOrderByApiName() throws TechnicalException {
        when(pageRepository.findMaxPageReferenceIdAndReferenceTypeOrder(API_ID, PageReferenceType.API)).thenReturn(10);

        assertEquals(10, pageService.findMaxApiPageOrderByApi(API_ID));
    }

    @Test
    public void shouldFindMaxPageOrderByApiNameWhenNull() throws TechnicalException {
        when(pageRepository.findMaxPageReferenceIdAndReferenceTypeOrder(API_ID, PageReferenceType.API)).thenReturn(null);

        assertEquals(0, pageService.findMaxApiPageOrderByApi(API_ID));
    }

    @Test
    public void shouldNotFindMaxPageOrderByApiNameBecauseTechnicalException() throws TechnicalException {
        assertThrows(TechnicalManagementException.class, () -> {
            doThrow(TechnicalException.class)
                .when(pageRepository)
                .findMaxPageReferenceIdAndReferenceTypeOrder(API_ID, PageReferenceType.API);

            pageService.findMaxApiPageOrderByApi(API_ID);
        });
    }
}
