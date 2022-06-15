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

import static io.gravitee.repository.management.model.CustomUserFieldReferenceType.ORGANIZATION;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.CustomUserFieldsRepository;
import io.gravitee.repository.management.model.CustomUserField;
import io.gravitee.repository.management.model.MetadataFormat;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Arrays;
import java.util.Date;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CustomUserFieldsRepositoryMock extends AbstractRepositoryMock<CustomUserFieldsRepository> {

    public CustomUserFieldsRepositoryMock() {
        super(CustomUserFieldsRepository.class);
    }

    @Override
    protected void prepare(CustomUserFieldsRepository customUserFieldsRepository) throws Exception {
        final CustomUserField stringCustomUserField = mock(CustomUserField.class);
        when(stringCustomUserField.getKey()).thenReturn("string");
        when(stringCustomUserField.getLabel()).thenReturn("String");
        when(stringCustomUserField.getReferenceId()).thenReturn("DEFAULT");
        when(stringCustomUserField.getReferenceType()).thenReturn(ORGANIZATION);
        when(stringCustomUserField.isRequired()).thenReturn(false);
        when(stringCustomUserField.getValues()).thenReturn(Arrays.asList("test_values"));
        when(stringCustomUserField.getCreatedAt()).thenReturn(new Date(1486771200000L));
        when(stringCustomUserField.getUpdatedAt()).thenReturn(new Date(1486771200000L));

        when(
            customUserFieldsRepository.findById(
                argThat(key -> "string".equals(key)),
                argThat(refid -> "DEFAULT".equals(refid)),
                argThat(org -> ORGANIZATION.equals(org))
            )
        )
            .thenReturn(of(stringCustomUserField));

        CustomUserField string = new CustomUserField();
        string.setKey("string");
        CustomUserField bool = new CustomUserField();
        bool.setKey("boolean");
        CustomUserField update = new CustomUserField();
        update.setKey("updateKey");
        CustomUserField delete = new CustomUserField();
        delete.setKey("deleteKey");

        when(customUserFieldsRepository.findByReferenceIdAndReferenceType("DEFAULT", ORGANIZATION))
            .thenReturn(asList(string, bool, update, delete));

        // mock for shouldCreate
        CustomUserField newCustomUserField = new CustomUserField();
        newCustomUserField.setKey("newkey");
        newCustomUserField.setLabel("newkeyLabel");
        newCustomUserField.setReferenceId("DEFAULT");
        newCustomUserField.setReferenceType(ORGANIZATION);
        newCustomUserField.setFormat(MetadataFormat.MAIL);
        newCustomUserField.setRequired(false);
        newCustomUserField.setValues(Arrays.asList("test@domain.net"));
        when(
            customUserFieldsRepository.findById(
                argThat(key -> "newkey".equals(key)),
                argThat(refid -> "DEFAULT".equals(refid)),
                argThat(org -> ORGANIZATION.equals(org))
            )
        )
            .thenReturn(empty(), of(newCustomUserField));
        when(customUserFieldsRepository.create(any())).thenReturn(newCustomUserField);

        // mock for shouldUpdate
        CustomUserField toUpdateField = new CustomUserField();
        toUpdateField.setKey("updateKey");
        toUpdateField.setLabel("label");
        toUpdateField.setReferenceId("DEFAULT");
        toUpdateField.setReferenceType(ORGANIZATION);
        toUpdateField.setFormat(MetadataFormat.MAIL);
        toUpdateField.setRequired(false);

        CustomUserField updatedField = new CustomUserField();
        updatedField.setKey("updateKey");
        updatedField.setLabel("LabelUPDATED");
        updatedField.setReferenceId("DEFAULT");
        updatedField.setReferenceType(ORGANIZATION);
        updatedField.setFormat(MetadataFormat.MAIL);
        updatedField.setRequired(true);
        updatedField.setValues(Arrays.asList("test@domain.net2"));
        when(
            customUserFieldsRepository.findById(
                argThat(key -> "updateKey".equals(key)),
                argThat(refid -> "DEFAULT".equals(refid)),
                argThat(refType -> ORGANIZATION.equals(refType))
            )
        )
            .thenReturn(of(toUpdateField), of(updatedField));
        when(customUserFieldsRepository.update(any())).thenReturn(updatedField);

        when(
            customUserFieldsRepository.findById(
                argThat(key -> "deleteKey".equals(key)),
                argThat(refid -> "DEFAULT".equals(refid)),
                argThat(refType -> ORGANIZATION.equals(refType))
            )
        )
            .thenReturn(of(new CustomUserField()), empty());
    }
}
