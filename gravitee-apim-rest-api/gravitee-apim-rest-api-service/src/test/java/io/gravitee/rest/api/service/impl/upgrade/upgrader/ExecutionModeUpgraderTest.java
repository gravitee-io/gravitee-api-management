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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.Api;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ExecutionModeUpgraderTest {

    @Mock
    private ApiRepository apiRepository;

    private ExecutionModeUpgrader cut;
    private GraviteeMapper graviteeMapper;

    @Before
    public void before() {
        graviteeMapper = new GraviteeMapper(false);
        cut = new ExecutionModeUpgrader(apiRepository, graviteeMapper);
    }

    @Test(expected = UpgraderException.class)
    public void upgrade_should_failed_because_of_exception() throws TechnicalException, UpgraderException {
        when(apiRepository.search(any(), any(), any())).thenThrow(new RuntimeException());

        cut.upgrade();

        verify(apiRepository, times(1)).search(any(), any(), any());
        verify(apiRepository, never()).update(any());
    }

    @Test
    public void should_order_equals_600() {
        assertThat(cut.getOrder()).isEqualTo(610);
    }

    @Test
    public void should_do_nothing_when_nothing_to_migrate() throws TechnicalException, UpgraderException {
        cut.upgrade();
        verify(apiRepository, never()).update(any());
    }

    @Test
    public void should_update_v2_api_with_jupiter_execution_mode() throws TechnicalException, UpgraderException {
        ApiCriteria onlyV2ApiCriteria = new ApiCriteria.Builder().definitionVersion(List.of(DefinitionVersion.V2)).build();
        Api jupiterApi = new Api();
        jupiterApi.setDefinition("{\"execution_mode\" : \"jupiter\"}");
        Api v3Api = new Api();
        v3Api.setDefinition("{\"execution_mode\":\"v3\"}");
        Api v4EmulationApi = new Api();
        v4EmulationApi.setDefinition("{\"execution_mode\":\"v4-emulation-engine\"}");
        when(apiRepository.search(eq(onlyV2ApiCriteria), any(), any())).thenReturn(Stream.of(jupiterApi, v3Api, v4EmulationApi));

        cut.upgrade();

        verify(apiRepository, times(1)).update(
            argThat(argument -> {
                String apiDef = argument.getDefinition();
                try {
                    JsonNode jsonNode = graviteeMapper.readTree(apiDef);
                    assertThat(jsonNode.get("execution_mode").asText()).isEqualTo(ExecutionMode.V4_EMULATION_ENGINE.getLabel());
                } catch (JsonProcessingException e) {
                    return false;
                }
                return true;
            })
        );
    }
}
