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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.service.jackson.ser.api.*;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author Nicolas Geraud (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_gRPC_ExportAsJsonTest extends ApiService_gRPC_ExportAsJsonTestSetup {

    @Test
    public void shouldConvertAsJsonForExport() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonForExport_3_0() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_3_0, "3_0");
    }

    @Test
    public void shouldConvertAsJsonForExport_1_15() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_1_15, "1_15");
    }

    @Test
    public void shouldConvertAsJsonForExport_1_20() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_1_20, "1_20");
    }

    @Test
    public void shouldConvertAsJsonForExport_1_25() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_1_25, "1_25");
    }
}
