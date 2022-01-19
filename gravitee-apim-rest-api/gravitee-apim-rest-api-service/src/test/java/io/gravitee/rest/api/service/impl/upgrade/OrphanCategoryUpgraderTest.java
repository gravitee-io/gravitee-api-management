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
package io.gravitee.rest.api.service.impl.upgrade;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Category;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OrphanCategoryUpgraderTest {

    @InjectMocks
    @Spy
    private OrphanCategoryUpgrader upgrader = new OrphanCategoryUpgrader();

    @Mock
    private InstallationService installationService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Test
    public void upgrade_should_not_run_cause_already_executed_with_success() {
        setUpgradeStatus(UpgradeStatus.SUCCESS);
        assertFalse(upgrader.upgrade());
        verify(installationService, never()).setAdditionalInformation(any());
    }

    @Test
    public void upgrade_should_not_run_because_already_running() {
        setUpgradeStatus(UpgradeStatus.RUNNING);
        assertFalse(upgrader.upgrade());
        verify(installationService, never()).setAdditionalInformation(any());
    }

    @Test
    public void upgrade_should_run_because_already_executed_but_failed() {
        setUpgradeStatus(UpgradeStatus.FAILURE);
        assertTrue(upgrader.upgrade());
        verify(installationService, times(2)).setAdditionalInformation(any());
    }

    @Test
    public void upgrade_should_remove_orphan_categories() throws TechnicalException {
        final String orphanCategoryId = UuidString.generateRandom();

        Category existingCategory = new Category();
        existingCategory.setId(UuidString.generateRandom());
        when(categoryRepository.findAll()).thenReturn(Set.of(existingCategory));

        Api apiWithOrphanCategory = new Api();
        apiWithOrphanCategory.setCategories(Set.of(orphanCategoryId, existingCategory.getId()));
        when(apiRepository.findAll()).thenReturn(Set.of(apiWithOrphanCategory));

        setUpgradeStatus(null);
        assertTrue(upgrader.upgrade());

        assertEquals(1, apiWithOrphanCategory.getCategories().size());
        assertFalse(apiWithOrphanCategory.getCategories().contains(orphanCategoryId));
        assertTrue(apiWithOrphanCategory.getCategories().contains(existingCategory.getId()));

        verify(installationService, times(2)).setAdditionalInformation(any());
    }

    private void setUpgradeStatus(UpgradeStatus status) {
        InstallationEntity installation = mock(InstallationEntity.class);
        Map<String, String> statusData = new HashMap<>();
        statusData.put(InstallationService.ORPHAN_CATEGORY_UPGRADER_STATUS, status == null ? null : status.name());
        when(installation.getAdditionalInformation()).thenReturn(statusData);
        when(installationService.getOrInitialize()).thenReturn(installation);
    }
}
