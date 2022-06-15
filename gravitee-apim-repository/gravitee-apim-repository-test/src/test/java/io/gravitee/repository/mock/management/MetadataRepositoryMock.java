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
package io.gravitee.repository.mock.management;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataFormat;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MetadataRepositoryMock extends AbstractRepositoryMock<MetadataRepository> {

    public MetadataRepositoryMock() {
        super(MetadataRepository.class);
    }

    @Override
    protected void prepare(MetadataRepository metadataRepository) throws Exception {
        final Metadata booleanMetadata = mock(Metadata.class);
        when(booleanMetadata.getKey()).thenReturn("boolean");
        when(booleanMetadata.getName()).thenReturn("Boolean");

        final Metadata stringMetadata = mock(Metadata.class);
        when(stringMetadata.getName()).thenReturn("Metadata name");
        when(stringMetadata.getFormat()).thenReturn(MetadataFormat.STRING);
        when(stringMetadata.getValue()).thenReturn("String");
        when(stringMetadata.getKey()).thenReturn("key");
        when(stringMetadata.getReferenceId()).thenReturn("apiId");
        when(stringMetadata.getReferenceType()).thenReturn(MetadataReferenceType.API);

        final Metadata metadata2Updated = mock(Metadata.class);
        when(metadata2Updated.getName()).thenReturn("New metadata");
        when(metadata2Updated.getValue()).thenReturn("New value");
        when(metadata2Updated.getFormat()).thenReturn(MetadataFormat.URL);
        when(metadata2Updated.getReferenceType()).thenReturn(MetadataReferenceType.APPLICATION);

        final List<Metadata> metadataList = asList(booleanMetadata, stringMetadata, mock(Metadata.class));
        final List<Metadata> metadataListAfterAdd = asList(booleanMetadata, stringMetadata, mock(Metadata.class), mock(Metadata.class));
        final List<Metadata> metadataListAfterDelete = asList(booleanMetadata, stringMetadata);

        when(metadataRepository.findByReferenceType(MetadataReferenceType.DEFAULT))
            .thenReturn(metadataList, metadataListAfterAdd, metadataList, metadataList);
        when(metadataRepository.findByReferenceTypeAndReferenceId(MetadataReferenceType.APPLICATION, "applicationId"))
            .thenReturn(metadataList, metadataListAfterDelete);
        when(metadataRepository.findByReferenceTypeAndReferenceId(MetadataReferenceType.API, "apiId"))
            .thenReturn(singletonList(stringMetadata));
        when(metadataRepository.findByReferenceType(MetadataReferenceType.APPLICATION)).thenReturn(singletonList(metadata2Updated));
        when(metadataRepository.findByKeyAndReferenceType("string", MetadataReferenceType.API)).thenReturn(singletonList(stringMetadata));

        when(metadataRepository.create(any(Metadata.class))).thenReturn(booleanMetadata);

        when(metadataRepository.findById("new-metadata", "_", MetadataReferenceType.DEFAULT)).thenReturn(of(stringMetadata));
        when(metadataRepository.findById("boolean", "_", MetadataReferenceType.DEFAULT))
            .thenReturn(of(booleanMetadata), of(metadata2Updated));

        when(metadataRepository.update(argThat(o -> o == null || o.getKey().equals("unknown")))).thenThrow(new IllegalStateException());
    }
}
