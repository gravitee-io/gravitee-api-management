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
package io.gravitee.rest.api.service.v4.mapper;

import static org.mockito.Mockito.verify;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.converter.ApiConverter;
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
public class GenericApiMapperTest {

    @Mock
    private ApiMapper apiMapper;

    @Mock
    private ApiConverter apiConverter;

    private GenericApiMapper genericApiMapper;

    @Before
    public void before() {
        genericApiMapper = new GenericApiMapper(apiMapper, apiConverter);
    }

    @Test
    public void shouldCallHttpV4ApiMapperWhenDefinitionVersionV4AndNotNative() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.PROXY);

        genericApiMapper.toGenericApi(api, null);
        verify(apiMapper).toEntity(api, null);
    }

    @Test
    public void shouldCallNativeV4ApiMapperWhenDefinitionVersionV4AndNative() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.NATIVE);

        genericApiMapper.toGenericApi(api, null);
        verify(apiMapper).toNativeEntity(api, null);
    }

    @Test
    public void shouldCallV2ApiMapperWhenV2DefinitionVersion() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V2);

        genericApiMapper.toGenericApi(api, null);
        verify(apiConverter).toApiEntity(api, null);
    }

    @Test
    public void shouldCallV2ApiMapperWhenV1DefinitionVersion() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V1);

        genericApiMapper.toGenericApi(api, null);
        verify(apiConverter).toApiEntity(api, null);
    }

    @Test
    public void shouldCallV2ApiMapperWhenNoDefinitionVersion() {
        Api api = new Api();

        genericApiMapper.toGenericApi(api, null);
        verify(apiConverter).toApiEntity(api, null);
    }
}
