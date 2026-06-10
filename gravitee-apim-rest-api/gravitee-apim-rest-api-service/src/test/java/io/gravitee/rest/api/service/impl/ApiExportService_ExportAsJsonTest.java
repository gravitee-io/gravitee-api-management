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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
<<<<<<< HEAD
=======
import static org.assertj.core.api.Assertions.assertThatThrownBy;
>>>>>>> dd949b5fe5 (fix(rest-api): fail API export when serialization fails instead of returning empty)
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.definition.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.GroupsNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
<<<<<<< HEAD
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
=======
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
>>>>>>> dd949b5fe5 (fix(rest-api): fail API export when serialization fails instead of returning empty)

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

    @Test
    public void exportAsJson_throwsTechnicalManagementException_whenSerializationFails() {
        // API references a group that no longer exists: the serializer throws and must not be swallowed
        when(groupService.findByIds(apiEntity.getGroups())).thenThrow(new GroupsNotFoundException(Set.of("my-group")));

        assertThatThrownBy(() ->
            apiExportService.exportAsJson(GraviteeContext.getExecutionContext(), API_ID, ApiSerializer.Version.DEFAULT.getVersion())
        )
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining("my-group");
    }
}
