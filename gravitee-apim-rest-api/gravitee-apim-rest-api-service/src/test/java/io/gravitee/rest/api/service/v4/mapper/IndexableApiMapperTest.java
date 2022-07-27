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
package io.gravitee.rest.api.service.v4.mapper;

import static org.mockito.Mockito.verify;

import io.gravitee.definition.model.DefinitionVersion;
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
public class IndexableApiMapperTest {

    @Mock
    private ApiMapper apiMapper;

    @Mock
    private ApiConverter apiConverter;

    private IndexableApiMapper indexableApiMapper;

    @Before
    public void before() {
        indexableApiMapper = new IndexableApiMapper(apiMapper, apiConverter);
    }

    @Test
    public void shouldCallV4ApiMapperWhenDefinitionVersionV4() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V4);

        indexableApiMapper.toGenericApi(api, null);
        verify(apiMapper).toEntity(api, null);
    }

    @Test
    public void shouldCallV2ApiMapperWhenV2DefinitionVersion() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V2);

        indexableApiMapper.toGenericApi(api, null);
        verify(apiConverter).toApiEntity(api, null);
    }

    @Test
    public void shouldCallV2ApiMapperWhenV1DefinitionVersion() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V1);

        indexableApiMapper.toGenericApi(api, null);
        verify(apiConverter).toApiEntity(api, null);
    }

    @Test
    public void shouldCallV2ApiMapperWhenNoDefinitionVersion() {
        Api api = new Api();

        indexableApiMapper.toGenericApi(api, null);
        verify(apiConverter).toApiEntity(api, null);
    }
}
