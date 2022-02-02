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

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiExportService_gRPC_ExportAsJsonV2Test extends ApiExportService_gRPC_ExportAsJsonTestSetup {

    @Override
    protected ApiEntity prepareApiEntity(ApiEntity apiEntity) {
        ApiEntity updatedApiEntity = super.prepareApiEntity(apiEntity);
        updatedApiEntity.setDescription("Gravitee.io 2.0.0");
        updatedApiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        return updatedApiEntity;
    }

    @Test
    public void shouldConvertAsJsonForExport_3_0V2() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_3_0, "3_0V2");
    }
}
