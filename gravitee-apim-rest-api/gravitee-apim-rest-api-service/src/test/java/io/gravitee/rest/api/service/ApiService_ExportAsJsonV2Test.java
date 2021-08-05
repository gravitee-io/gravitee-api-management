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
package io.gravitee.rest.api.service;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
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
public class ApiService_ExportAsJsonV2Test extends ApiService_gRPC_ExportAsJsonTestSetup {

    @Override
    protected io.gravitee.definition.model.Api buildApiDefinition(Api api) {
        io.gravitee.definition.model.Api apiDef = super.buildApiDefinition(api);
        api.setDescription("Gravitee.io 2.0.0");
        api.setEnvironmentId("DEFAULT");
        apiDef.setDefinitionVersion(DefinitionVersion.V2);
        return apiDef;
    }

    @Test
    public void shouldConvertAsJsonForExport_3_0V2() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_3_0, "3_0V2");
    }
}
