package io.gravitee.rest.api.service.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiConverterTest {

    @InjectMocks
    private ApiConverter apiConverter;

    @Test
    public void toUpdateApiEntity_should_keep_crossId() {
        ApiEntity apiEntity = buildTestApiEntity();
        apiEntity.setCrossId("test-cross-id");

        UpdateApiEntity updateApiEntity = apiConverter.toUpdateApiEntity(apiEntity);

        assertEquals("test-cross-id", updateApiEntity.getCrossId());
    }

    @Test
    public void toUpdateApiEntity_should_reset_crossId_if_param_set_to_true() {
        ApiEntity apiEntity = buildTestApiEntity();
        apiEntity.setCrossId("test-cross-id");

        UpdateApiEntity updateApiEntity = apiConverter.toUpdateApiEntity(apiEntity, true);

        assertNull("test-cross-id", updateApiEntity.getCrossId());
    }

    private ApiEntity buildTestApiEntity() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        return apiEntity;
    }
}
