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
package io.gravitee.repository.management;

import static io.gravitee.repository.management.model.CustomUserFieldReferenceType.ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.model.CustomUserField;
import io.gravitee.repository.management.model.MetadataFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Test;

public class CustomUserFieldsRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/customuserfields-tests/";
    }

    @Test
    public void shouldFindById() throws Exception {
        final Optional<CustomUserField> optionalCustomUserField = customUserFieldsRepository.findById("string", "DEFAULT", ORGANIZATION);
        assertThat(optionalCustomUserField).isNotNull();
        assertThat(optionalCustomUserField.isPresent()).isTrue();
        assertThat(optionalCustomUserField.get().getKey()).isEqualTo("string");
        assertThat(optionalCustomUserField.get().getReferenceId()).isEqualTo("DEFAULT");
        assertThat(optionalCustomUserField.get().getReferenceType()).isEqualTo(ORGANIZATION);
        assertThat(optionalCustomUserField.get().getLabel()).isEqualTo("String");
        assertThat(optionalCustomUserField.get().isRequired()).isFalse();
        assertThat(optionalCustomUserField.get().getCreatedAt()).isEqualTo(new Date(1486771200000L));
        assertThat(optionalCustomUserField.get().getUpdatedAt()).isEqualTo(new Date(1486771200000L));
        assertThat(optionalCustomUserField.get().getValues()).isNotNull().containsExactlyInAnyOrder("test_values");
    }

    @Test
    public void shouldFindByRef() throws Exception {
        final List<CustomUserField> customUserFields = customUserFieldsRepository.findByReferenceIdAndReferenceType(
            "DEFAULT",
            ORGANIZATION
        );
        assertThat(customUserFields).isNotNull();
        assertThat(customUserFields).size().isEqualTo(4);
        assertThat(customUserFields.stream().map(CustomUserField::getKey).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("string", "boolean", "updateKey", "deleteKey");
    }

    @Test
    public void shouldCreate() throws Exception {
        CustomUserField newCufCustomUserField = new CustomUserField();
        newCufCustomUserField.setKey("newkey");
        newCufCustomUserField.setLabel("newkeyLabel");
        newCufCustomUserField.setReferenceId("DEFAULT");
        newCufCustomUserField.setReferenceType(ORGANIZATION);
        newCufCustomUserField.setFormat(MetadataFormat.MAIL);
        newCufCustomUserField.setRequired(false);
        newCufCustomUserField.setValues(Arrays.asList("test@domain.net"));

        final Optional<CustomUserField> beforeCreateBean = customUserFieldsRepository.findById(
            newCufCustomUserField.getKey(),
            newCufCustomUserField.getReferenceId(),
            ORGANIZATION
        );
        CustomUserField createdCustomUserField = customUserFieldsRepository.create(newCufCustomUserField);
        final Optional<CustomUserField> afterCreateBean = customUserFieldsRepository.findById(
            newCufCustomUserField.getKey(),
            newCufCustomUserField.getReferenceId(),
            ORGANIZATION
        );

        assertThat(createdCustomUserField).isNotNull();
        assertThat(beforeCreateBean).isNotNull();
        assertThat(beforeCreateBean.isPresent()).isFalse();
        assertThat(afterCreateBean).isNotNull();
        assertThat(afterCreateBean.isPresent()).isTrue();

        assertThat(newCufCustomUserField.getKey()).isEqualTo(afterCreateBean.get().getKey());
        assertThat(newCufCustomUserField.getLabel()).isEqualTo(afterCreateBean.get().getLabel());
        assertThat(newCufCustomUserField.getFormat()).isEqualTo(afterCreateBean.get().getFormat());
        assertThat(newCufCustomUserField.getReferenceId()).isEqualTo(afterCreateBean.get().getReferenceId());
        assertThat(newCufCustomUserField.getReferenceType()).isEqualTo(afterCreateBean.get().getReferenceType());
        assertThat(newCufCustomUserField.getValues().get(0)).isEqualTo(afterCreateBean.get().getValues().get(0));

        assertThat(newCufCustomUserField.getKey()).isEqualTo(createdCustomUserField.getKey());
        assertThat(newCufCustomUserField.getLabel()).isEqualTo(createdCustomUserField.getLabel());
        assertThat(newCufCustomUserField.getFormat()).isEqualTo(createdCustomUserField.getFormat());
        assertThat(newCufCustomUserField.getReferenceId()).isEqualTo(createdCustomUserField.getReferenceId());
        assertThat(newCufCustomUserField.getReferenceType()).isEqualTo(createdCustomUserField.getReferenceType());
        assertThat(newCufCustomUserField.getValues().get(0)).isEqualTo(createdCustomUserField.getValues().get(0));
    }

    @Test
    public void shouldUpdate() throws Exception {
        CustomUserField customUserField = new CustomUserField();
        customUserField.setKey("updateKey");
        customUserField.setLabel("LabelUPDATED");
        customUserField.setReferenceId("DEFAULT");
        customUserField.setReferenceType(ORGANIZATION);
        customUserField.setFormat(MetadataFormat.MAIL);
        customUserField.setRequired(true);
        customUserField.setValues(Arrays.asList("test@domain.net2"));

        final Optional<CustomUserField> beforeUpdateBean = customUserFieldsRepository.findById(
            customUserField.getKey(),
            customUserField.getReferenceId(),
            customUserField.getReferenceType()
        );
        CustomUserField updatedCustomUserField = customUserFieldsRepository.update(customUserField);
        final Optional<CustomUserField> afterUpdateBean = customUserFieldsRepository.findById(
            customUserField.getKey(),
            customUserField.getReferenceId(),
            customUserField.getReferenceType()
        );

        assertThat(customUserField).isNotNull();
        assertThat(beforeUpdateBean).isNotNull();
        assertThat(beforeUpdateBean.isPresent()).isTrue();
        assertThat(afterUpdateBean).isNotNull();
        assertThat(afterUpdateBean.isPresent()).isTrue();
        assertThat(updatedCustomUserField).isNotNull();

        assertThat(customUserField.getKey()).isEqualTo(afterUpdateBean.get().getKey());
        assertThat(customUserField.getLabel()).isEqualTo(afterUpdateBean.get().getLabel());
        assertThat(customUserField.getFormat()).isEqualTo(afterUpdateBean.get().getFormat());
        assertThat(customUserField.getReferenceId()).isEqualTo(afterUpdateBean.get().getReferenceId());
        assertThat(customUserField.getValues()).hasSameSizeAs(afterUpdateBean.get().getValues());

        assertThat(beforeUpdateBean.get().getKey()).isEqualTo(afterUpdateBean.get().getKey());
        assertThat(beforeUpdateBean.get().getReferenceId()).isEqualTo(afterUpdateBean.get().getReferenceId());
        assertThat(beforeUpdateBean.get().getFormat()).isEqualTo(afterUpdateBean.get().getFormat());
        assertThat(beforeUpdateBean.get().getLabel()).isNotEqualTo(afterUpdateBean.get().getLabel());
        assertThat(beforeUpdateBean.get().isRequired()).isNotEqualTo(afterUpdateBean.get().isRequired());
        assertThat(beforeUpdateBean.get().getValues()).isNullOrEmpty();
    }

    @Test
    public void shouldDelete() throws Exception {
        final Optional<CustomUserField> beforeDelete = customUserFieldsRepository.findById("deleteKey", "DEFAULT", ORGANIZATION);
        customUserFieldsRepository.delete("deleteKey", "DEFAULT", ORGANIZATION);
        final Optional<CustomUserField> afterDelete = customUserFieldsRepository.findById("deleteKey", "DEFAULT", ORGANIZATION);

        assertThat(beforeDelete).isNotNull();
        assertThat(beforeDelete.isPresent()).isTrue();
        assertThat(afterDelete).isNotNull();
        assertThat(afterDelete.isPresent()).isFalse();
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        final List<CustomUserField> beforeDelete = customUserFieldsRepository.findByReferenceIdAndReferenceType(
            "ToBeDeleted",
            ORGANIZATION
        );
        List<String> keys = customUserFieldsRepository.deleteByReferenceIdAndReferenceType("ToBeDeleted", ORGANIZATION);

        final List<CustomUserField> afterDelete = customUserFieldsRepository.findByReferenceIdAndReferenceType("ToBeDeleted", ORGANIZATION);

        assertThat(beforeDelete).isNotNull();
        assertThat(beforeDelete.size()).isEqualTo(2);
        assertThat(keys.size()).isEqualTo(2);
        assertThat(afterDelete).isNotNull();
        assertThat(afterDelete.size()).isEqualTo(0);
    }
}
