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

import io.gravitee.management.model.EntrypointEntity;
import io.gravitee.management.model.NewEntryPointEntity;
import io.gravitee.management.model.UpdateEntryPointEntity;
import io.gravitee.management.service.exceptions.EntrypointNotFoundException;
import io.gravitee.management.service.exceptions.EntrypointTagsAlreadyExistsException;
import io.gravitee.management.service.impl.EntrypointServiceImpl;
import io.gravitee.repository.management.api.EntrypointRepository;
import io.gravitee.repository.management.model.Entrypoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.util.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EntrypointServiceTest {

    private static final String ID = "123";
    private static final String VALUE = "https://api.mycompany.com";
    private static final String TAG = "private;product";
    private static final String[] TAGS = new String[]{"private", "product"};

    private static final String NEW_VALUE = "https://public-api.mycompany.com";
    private static final String NEW_TAG = "public;product";
    private static final String[] NEW_TAGS = new String[]{"public", "product"};

    private static final String UNKNOWN_ID = "unknown";

    @InjectMocks
    private EntrypointService entrypointService = new EntrypointServiceImpl();

    @Mock
    private AuditService auditService;
    @Mock
    private EntrypointRepository entrypointRepository;
    private final Entrypoint entrypointCreated = new Entrypoint();
    private final Entrypoint entrypointUpdated = new Entrypoint();

    @Before
    public void init() throws Exception {
        entrypointCreated.setId(ID);
        entrypointCreated.setValue(VALUE);
        entrypointCreated.setTags(TAG);
        when(entrypointRepository.create(any())).thenReturn(entrypointCreated);
        when(entrypointRepository.findById(ID)).thenReturn(of(entrypointCreated));

        entrypointUpdated.setId(ID);
        entrypointUpdated.setValue(NEW_VALUE);
        entrypointUpdated.setTags(NEW_TAG);
        when(entrypointRepository.update(any())).thenReturn(entrypointUpdated);

        when(entrypointRepository.findById(UNKNOWN_ID)).thenReturn(empty());
    }

    @Test
    public void shouldCreate() {
        final NewEntryPointEntity entrypoint = new NewEntryPointEntity();
        entrypoint.setValue(VALUE);
        entrypoint.setTags(TAGS);
        final EntrypointEntity entrypointEntity = entrypointService.create(entrypoint);
        assertEquals(ID, entrypointEntity.getId());
        assertEquals(VALUE, entrypointEntity.getValue());
        assertNotNull(entrypointEntity.getTags());
        assertEquals(2, entrypointEntity.getTags().length);
    }

    @Test
    public void shouldUpdate() {
        final UpdateEntryPointEntity entrypoint = new UpdateEntryPointEntity();
        entrypoint.setId(ID);
        entrypoint.setValue(NEW_VALUE);
        entrypoint.setTags(NEW_TAGS);
        final EntrypointEntity entrypointEntity = entrypointService.update(entrypoint);
        assertEquals(ID, entrypointEntity.getId());
        assertEquals(NEW_VALUE, entrypointEntity.getValue());
        assertNotNull(entrypointEntity.getTags());
        assertEquals(2, entrypointEntity.getTags().length);
    }

    @Test
    public void shouldUpdateWithSameTags() throws Exception {
        // use to check existing tags excluding current entry point
        when(entrypointRepository.findAll()).thenReturn(newHashSet(singletonList(entrypointUpdated)));

        final UpdateEntryPointEntity entrypoint = new UpdateEntryPointEntity();
        entrypoint.setId(ID);
        entrypoint.setValue(NEW_VALUE);
        entrypoint.setTags(TAGS);
        final EntrypointEntity entrypointEntity = entrypointService.update(entrypoint);
        assertEquals(ID, entrypointEntity.getId());
        assertEquals(NEW_VALUE, entrypointEntity.getValue());
        assertNotNull(entrypointEntity.getTags());
        assertEquals(2, entrypointEntity.getTags().length);
    }

    @Test
    public void shouldDelete() throws Exception {
        entrypointService.delete(ID);
        verify(entrypointRepository).delete(ID);
    }

    @Test
    public void shouldFindAll() throws Exception {
        when(entrypointRepository.findAll()).thenReturn(newHashSet(singletonList(entrypointCreated)));
        final List<EntrypointEntity> entrypoints = entrypointService.findAll();
        assertNotNull(entrypoints);
        assertEquals(1, entrypoints.size());
    }

    @Test(expected = EntrypointNotFoundException.class)
    public void shouldNotUpdate() {
        final UpdateEntryPointEntity entrypoint = new UpdateEntryPointEntity();
        entrypoint.setId(UNKNOWN_ID);
        entrypointService.update(entrypoint);
    }

    @Test(expected = EntrypointNotFoundException.class)
    public void shouldNotDelete() {
        entrypointService.delete(UNKNOWN_ID);
    }

    @Test(expected = EntrypointTagsAlreadyExistsException.class)
    public void shouldNotCreateWithSameTags() throws Exception {
        when(entrypointRepository.findAll()).thenReturn(newHashSet(singletonList(entrypointCreated)));

        final NewEntryPointEntity entrypoint = new NewEntryPointEntity();
        entrypoint.setTags(new String[]{"product", "private"});
        entrypointService.create(entrypoint);
    }

    @Test(expected = EntrypointTagsAlreadyExistsException.class)
    public void shouldNotUpdateWithSameTags() throws Exception {
        when(entrypointRepository.findAll()).thenReturn(newHashSet(singletonList(entrypointUpdated)));

        final UpdateEntryPointEntity entrypoint = new UpdateEntryPointEntity();
        entrypoint.setId("new ID");
        entrypoint.setValue(VALUE);
        entrypoint.setTags(NEW_TAGS);
        entrypointService.update(entrypoint);
    }
}
