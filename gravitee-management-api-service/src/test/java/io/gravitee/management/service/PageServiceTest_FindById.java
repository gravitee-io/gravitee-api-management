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
package io.gravitee.management.service;

import io.gravitee.management.model.PageEntity;
import io.gravitee.management.service.exceptions.PageNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.PageServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageServiceTest_FindById {

    private static final String PAGE_ID = "ba01aef0-e3da-4499-81ae-f0e3daa4995a";

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Mock
    private Page page1;

    @Test
    public void shouldFindById() throws TechnicalException {
        when(page1.getId()).thenReturn(PAGE_ID);
        when(page1.getOrder()).thenReturn(1);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));

        final PageEntity pageEntity = pageService.findById(PAGE_ID);

        assertNotNull(pageEntity);
        assertEquals(1, pageEntity.getOrder());
        assertEquals(PAGE_ID, pageEntity.getId());
    }

    @Test(expected = PageNotFoundException.class)
    public void shouldNotFindByIdBecauseNotFound() throws TechnicalException {
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.empty());

        pageService.findById(PAGE_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdBecauseTechnicalException() throws TechnicalException {
        when(pageRepository.findById(PAGE_ID)).thenThrow(TechnicalException.class);

        pageService.findById(PAGE_ID);
    }
}
