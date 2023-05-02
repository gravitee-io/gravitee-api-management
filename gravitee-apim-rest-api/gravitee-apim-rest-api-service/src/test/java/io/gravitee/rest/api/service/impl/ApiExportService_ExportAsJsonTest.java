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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.definition.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author Nicolas Geraud (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiExportService_ExportAsJsonTest extends ApiExportService_ExportAsJsonTestSetup {

    @Test
    public void shouldConvertAsJsonForExport() throws IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonWithoutMembers() throws IOException {
        shouldConvertAsJsonWithoutMembers(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonWithoutPages() throws IOException {
        shouldConvertAsJsonWithoutPages(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonWithoutPlans() throws IOException {
        shouldConvertAsJsonWithoutPlans(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonWithoutMetadata() throws IOException {
        shouldConvertAsJsonWithoutMetadata(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonForExportWithExecutionMode() throws IOException {
        shouldConvertAsJsonForExportWithExecutionMode(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonForExportWithExecutionMode_v3() throws IOException {
        shouldConvertAsJsonForExportWithExecutionMode(ApiSerializer.Version.DEFAULT, ExecutionMode.V3);
    }

    @Test
    public void shouldConvertAsJsonForExportWithExecutionMode_v4_emulation_engine() throws IOException {
        shouldConvertAsJsonForExportWithExecutionMode(ApiSerializer.Version.DEFAULT, ExecutionMode.V4_EMULATION_ENGINE);
    }
}
