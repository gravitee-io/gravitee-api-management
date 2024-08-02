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
import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

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
        assertNotNull(optionalCustomUserField);
        assertTrue(optionalCustomUserField.isPresent());
        assertEquals("FindById('string, default) : Invalid CustomUserField.key", "string", optionalCustomUserField.get().getKey());
        assertEquals(
            "FindById('string, default) : Invalid CustomUserField.refId",
            "DEFAULT",
            optionalCustomUserField.get().getReferenceId()
        );
        assertEquals(
            "FindById('string, default) : Invalid CustomUserField.refType",
            ORGANIZATION,
            optionalCustomUserField.get().getReferenceType()
        );
        assertEquals("FindById('string, default) : Invalid CustomUserField.label", "String", optionalCustomUserField.get().getLabel());
        assertFalse("FindById('string, default) : Invalid CustomUserField.required", optionalCustomUserField.get().isRequired());
        assertTrue(
            "FindById('string, default) : Invalid CustomUserField.createdAt",
            compareDate(new Date(1486771200000L), optionalCustomUserField.get().getCreatedAt())
        );
        assertTrue(
            "FindById('string, default) : Invalid CustomUserField.updatedAt",
            compareDate(new Date(1486771200000L), optionalCustomUserField.get().getUpdatedAt())
        );
        assertNotNull("FindById('string, default) : Invalid CustomUserField.values", optionalCustomUserField.get().getValues());
        assertThat(
            "FindById('string, default) : Invalid CustomUserField.values",
            optionalCustomUserField.get().getValues(),
            containsInAnyOrder("test_values")
        );
    }

    @Test
    public void shouldFindByRef() throws Exception {
        final List<CustomUserField> customUserFields = customUserFieldsRepository.findByReferenceIdAndReferenceType(
            "DEFAULT",
            ORGANIZATION
        );
        assertNotNull("FindByRef(Default) Should return non null list ", customUserFields);
        assertEquals("FindByRef(Default) Should return 4 elements", 4, customUserFields.size());
        assertThat(
            customUserFields.stream().map(CustomUserField::getKey).collect(Collectors.toList()),
            containsInAnyOrder("string", "boolean", "updateKey", "deleteKey")
        );
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

        assertNotNull("customUserFieldsRepository.create should return an CustomUserField", createdCustomUserField);
        assertNotNull(beforeCreateBean);
        assertFalse("customUserFieldsRepository should not found value before create", beforeCreateBean.isPresent());
        assertNotNull(afterCreateBean);
        assertTrue("customUserFieldsRepository should found value before create", afterCreateBean.isPresent());

        assertEquals("Invalid CustomUserField.key", newCufCustomUserField.getKey(), afterCreateBean.get().getKey());
        assertEquals("Invalid CustomUserField.label", newCufCustomUserField.getLabel(), afterCreateBean.get().getLabel());
        assertEquals("Invalid CustomUserField.format", newCufCustomUserField.getFormat(), afterCreateBean.get().getFormat());
        assertEquals("Invalid CustomUserField.refId", newCufCustomUserField.getReferenceId(), afterCreateBean.get().getReferenceId());
        assertEquals("Invalid CustomUserField.refType", newCufCustomUserField.getReferenceType(), afterCreateBean.get().getReferenceType());
        assertEquals("Invalid CustomUserField.values", afterCreateBean.get().getValues().get(0), newCufCustomUserField.getValues().get(0));

        assertEquals("Invalid CustomUserField.key", newCufCustomUserField.getKey(), createdCustomUserField.getKey());
        assertEquals("Invalid CustomUserField.label", newCufCustomUserField.getLabel(), createdCustomUserField.getLabel());
        assertEquals("Invalid CustomUserField.format", newCufCustomUserField.getFormat(), createdCustomUserField.getFormat());
        assertEquals("Invalid CustomUserField.refId", newCufCustomUserField.getReferenceId(), createdCustomUserField.getReferenceId());
        assertEquals(
            "Invalid CustomUserField.refType",
            newCufCustomUserField.getReferenceType(),
            createdCustomUserField.getReferenceType()
        );
        assertEquals("Invalid CustomUserField.values", createdCustomUserField.getValues().get(0), newCufCustomUserField.getValues().get(0));
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

        assertNotNull(customUserField);
        assertNotNull(beforeUpdateBean);
        assertTrue(beforeUpdateBean.isPresent());
        assertNotNull(afterUpdateBean);
        assertTrue(afterUpdateBean.isPresent());
        assertNotNull(updatedCustomUserField);

        assertEquals("Invalid CustomUserField.key", customUserField.getKey(), afterUpdateBean.get().getKey());
        assertEquals("Invalid CustomUserField.label", customUserField.getLabel(), afterUpdateBean.get().getLabel());
        assertEquals("Invalid CustomUserField.format", customUserField.getFormat(), afterUpdateBean.get().getFormat());
        assertEquals("Invalid CustomUserField.refId", customUserField.getReferenceId(), afterUpdateBean.get().getReferenceId());
        assertEquals(
            "Invalid CustomUserField.values.size = 1",
            customUserField.getValues().size(),
            afterUpdateBean.get().getValues().size()
        );

        assertEquals("Invalid CustomUserField.key", customUserField.getKey(), afterUpdateBean.get().getKey());
        assertEquals("Invalid CustomUserField.refId", beforeUpdateBean.get().getReferenceId(), afterUpdateBean.get().getReferenceId());
        assertEquals("Invalid CustomUserField.format", beforeUpdateBean.get().getFormat(), afterUpdateBean.get().getFormat());
        assertNotEquals("Invalid CustomUserField.label", beforeUpdateBean.get().getLabel(), afterUpdateBean.get().getLabel());
        assertNotEquals("Invalid CustomUserField.required", beforeUpdateBean.get().isRequired(), afterUpdateBean.get().isRequired());
        assertTrue(
            "Invalid CustomUserField.values.empty",
            beforeUpdateBean.get().getValues() == null || beforeUpdateBean.get().getValues().isEmpty()
        );
    }

    @Test
    public void shouldDelete() throws Exception {
        final Optional<CustomUserField> beforeDelete = customUserFieldsRepository.findById("deleteKey", "DEFAULT", ORGANIZATION);
        customUserFieldsRepository.delete("deleteKey", "DEFAULT", ORGANIZATION);
        final Optional<CustomUserField> afterDelete = customUserFieldsRepository.findById("deleteKey", "DEFAULT", ORGANIZATION);

        assertNotNull(beforeDelete);
        assertTrue(beforeDelete.isPresent());
        assertNotNull(afterDelete);
        assertFalse(afterDelete.isPresent());
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        final List<String> beforeDelete = customUserFieldsRepository
            .findByReferenceIdAndReferenceType("ToBeDeleted", ORGANIZATION)
            .stream()
            .map(CustomUserField::getKey)
            .toList();
        List<String> deleted = customUserFieldsRepository.deleteByReferenceIdAndReferenceType("ToBeDeleted", ORGANIZATION);

        final List<CustomUserField> afterDelete = customUserFieldsRepository.findByReferenceIdAndReferenceType("ToBeDeleted", ORGANIZATION);

        assertEquals(beforeDelete.size(), deleted.size());
        assertTrue(beforeDelete.containsAll(deleted));
        assertEquals(0, afterDelete.size());
    }
}
