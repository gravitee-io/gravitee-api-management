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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataFormat;
import io.gravitee.repository.management.model.MetadataReferenceType;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MetadataDefaultReferenceUpgraderTest {

    private static final ZonedDateTime CREATED_AT = Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault());
    private static final ZonedDateTime UPDATED_AT = Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault());

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private MetadataRepository metadataRepository;

    private MetadataDefaultReferenceUpgrader upgrader;

    @Before
    public void setUp() throws Exception {
        upgrader = new MetadataDefaultReferenceUpgrader(environmentRepository, metadataRepository);
    }

    @Test
    public void should_upgrade_only_default_metadata() throws TechnicalException {
        when(environmentRepository.findAll())
            .thenReturn(Set.of(Environment.builder().id("DEFAULT").build(), Environment.builder().id("env#1").build()));

        Set<Metadata> metadataList = Set.of(
            aMetadata("_", MetadataReferenceType.ENVIRONMENT, "key#1"),
            aMetadata("_", MetadataReferenceType.API, "key#2"),
            aMetadata("env#1", MetadataReferenceType.ENVIRONMENT),
            aMetadata("api#1", MetadataReferenceType.API),
            aMetadata("app#1", MetadataReferenceType.APPLICATION),
            aMetadata("user#1", MetadataReferenceType.USER)
        );
        when(metadataRepository.findAll()).thenReturn(metadataList);

        assertTrue(upgrader.upgrade());

        verify(metadataRepository, times(4)).create(any());
        verify(metadataRepository)
            .create(argThat(metadata -> metadata.equals(aMetadata("DEFAULT", MetadataReferenceType.ENVIRONMENT, "key#1"))));
        verify(metadataRepository)
            .create(argThat(metadata -> metadata.equals(aMetadata("DEFAULT", MetadataReferenceType.ENVIRONMENT, "key#2"))));
        verify(metadataRepository)
            .create(argThat(metadata -> metadata.equals(aMetadata("env#1", MetadataReferenceType.ENVIRONMENT, "key#1"))));
        verify(metadataRepository)
            .create(argThat(metadata -> metadata.equals(aMetadata("env#1", MetadataReferenceType.ENVIRONMENT, "key#2"))));
        verify(metadataRepository, times(2)).delete(any(), any(), any());
        verify(metadataRepository).delete(eq("key#1"), eq("_"), eq(MetadataReferenceType.ENVIRONMENT));
        verify(metadataRepository).delete(eq("key#2"), eq("_"), eq(MetadataReferenceType.API));
    }

    @Test
    public void should_upgrade_if_key_exist() throws TechnicalException {
        when(environmentRepository.findAll())
            .thenReturn(Set.of(Environment.builder().id("DEFAULT").build(), Environment.builder().id("env#1").build()));

        Set<Metadata> metadataList = Set.of(
            aMetadata("_", MetadataReferenceType.ENVIRONMENT, "key#1"),
            aMetadata("_", MetadataReferenceType.API, "key#2"),
            aMetadata("env#1", MetadataReferenceType.ENVIRONMENT, "key#1"),
            aMetadata("env#1", MetadataReferenceType.ENVIRONMENT),
            aMetadata("api#1", MetadataReferenceType.API),
            aMetadata("app#1", MetadataReferenceType.APPLICATION),
            aMetadata("user#1", MetadataReferenceType.USER)
        );
        when(metadataRepository.findAll()).thenReturn(metadataList);
        when(metadataRepository.create(aMetadata("env#1", MetadataReferenceType.ENVIRONMENT, "key#1")))
            .thenThrow(DuplicateKeyException.class);

        assertTrue(upgrader.upgrade());

        verify(metadataRepository, times(4)).create(any());
        verify(metadataRepository)
            .create(argThat(metadata -> metadata.equals(aMetadata("DEFAULT", MetadataReferenceType.ENVIRONMENT, "key#1"))));
        verify(metadataRepository)
            .create(argThat(metadata -> metadata.equals(aMetadata("DEFAULT", MetadataReferenceType.ENVIRONMENT, "key#2"))));
        verify(metadataRepository)
            .create(argThat(metadata -> metadata.equals(aMetadata("env#1", MetadataReferenceType.ENVIRONMENT, "key#1"))));
        verify(metadataRepository)
            .create(argThat(metadata -> metadata.equals(aMetadata("env#1", MetadataReferenceType.ENVIRONMENT, "key#2"))));
        verify(metadataRepository, times(2)).delete(any(), any(), any());
        verify(metadataRepository).delete(eq("key#1"), eq("_"), eq(MetadataReferenceType.ENVIRONMENT));
        verify(metadataRepository).delete(eq("key#2"), eq("_"), eq(MetadataReferenceType.API));
    }

    @Test
    public void should_not_create_if_not_find_default_metadata() throws TechnicalException {
        when(environmentRepository.findAll())
            .thenReturn(Set.of(Environment.builder().id("DEFAULT").build(), Environment.builder().id("env#1").build()));

        when(metadataRepository.findAll()).thenReturn(Collections.emptySet());

        assertTrue(upgrader.upgrade());

        verify(metadataRepository, never()).create(any());
        verify(metadataRepository, never()).delete(any(), any(), any());
    }

    @Test
    public void should_stop_if_cannot_find_all_env() throws TechnicalException {
        when(environmentRepository.findAll()).thenThrow(new TechnicalException("Cannot find all env"));

        assertFalse(upgrader.upgrade());

        verify(metadataRepository, never()).create(any());
        verify(metadataRepository, never()).delete(any(), any(), any());
    }

    @Test
    public void should_stop_if_cannot_create_env() throws TechnicalException {
        when(environmentRepository.findAll())
            .thenReturn(Set.of(Environment.builder().id("DEFAULT").build(), Environment.builder().id("env#1").build()));

        Set<Metadata> metadataList = Set.of(
            aMetadata("_", MetadataReferenceType.ENVIRONMENT, "key#1"),
            aMetadata("_", MetadataReferenceType.API, "key#2"),
            aMetadata("env#1", MetadataReferenceType.ENVIRONMENT),
            aMetadata("api#1", MetadataReferenceType.API),
            aMetadata("app#1", MetadataReferenceType.APPLICATION),
            aMetadata("user#1", MetadataReferenceType.USER)
        );
        when(metadataRepository.findAll()).thenReturn(metadataList);
        when(metadataRepository.create(any())).thenThrow(new TechnicalException("Cannot create metadata"));

        assertFalse(upgrader.upgrade());

        verify(metadataRepository).create(any());
    }

    private Metadata aMetadata(String referenceId, MetadataReferenceType referenceType) {
        return aMetadata(referenceId, referenceType, "my-key");
    }

    private Metadata aMetadata(String referenceId, MetadataReferenceType referenceType, String key) {
        return aMetadata(referenceId, referenceType, key, "my-name", "my-value", MetadataFormat.STRING);
    }

    private Metadata aMetadata(
        String referenceId,
        MetadataReferenceType referenceType,
        String key,
        String name,
        String value,
        MetadataFormat format
    ) {
        return Metadata
            .builder()
            .referenceId(referenceId)
            .referenceType(referenceType)
            .key(key)
            .name(name)
            .value(value != null ? value.toString() : null)
            .format(format)
            .createdAt(Date.from(CREATED_AT.toInstant()))
            .updatedAt(Date.from(UPDATED_AT.toInstant()))
            .build();
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.DEFAULT_METADATA_UPGRADER, upgrader.getOrder());
    }
}
