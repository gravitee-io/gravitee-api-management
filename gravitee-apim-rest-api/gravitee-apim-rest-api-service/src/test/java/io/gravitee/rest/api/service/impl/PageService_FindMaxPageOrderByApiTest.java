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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PageServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
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

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindMaxPageOrderByApiNameBecauseTechnicalException() throws TechnicalException {
        doThrow(TechnicalException.class).when(pageRepository).findMaxPageReferenceIdAndReferenceTypeOrder(API_ID, PageReferenceType.API);

        pageService.findMaxApiPageOrderByApi(API_ID);
    }
}
