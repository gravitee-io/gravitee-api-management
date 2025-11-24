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
package io.gravitee.rest.api.management.v2.rest.mapper;

<<<<<<< HEAD
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.ListenerModelFixtures;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.management.v2.rest.model.MembershipMemberType;
import io.gravitee.rest.api.management.v2.rest.model.PrimaryOwner;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class ImportExportApiMapperTest extends AbstractMapperTest {

    private final ImportExportApiMapper importExportApiMapper = Mappers.getMapper(ImportExportApiMapper.class);

    @Test
    void shouldMapHttpListenerWithCors() {
        HttpListener httpListener = ListenerModelFixtures.aModelHttpListener();

        var result = importExportApiMapper.mapHttpListener(httpListener);

        assertThat(result).isNotNull();
        assertThat(result.getCors()).isNotNull();

        assertThat(result.getCors().getEnabled()).isEqualTo(httpListener.getCors().isEnabled());
        assertThat(result.getCors().getAllowOrigin()).isEqualTo(httpListener.getCors().getAccessControlAllowOrigin());
        assertThat(result.getCors().getAllowHeaders()).isEqualTo(httpListener.getCors().getAccessControlAllowHeaders());
        assertThat(result.getCors().getAllowMethods()).isEqualTo(httpListener.getCors().getAccessControlAllowMethods());
        assertThat(result.getCors().getAllowCredentials()).isEqualTo(httpListener.getCors().isAccessControlAllowCredentials());
        assertThat(result.getCors().getExposeHeaders()).isEqualTo(httpListener.getCors().getAccessControlExposeHeaders());
        assertThat(result.getCors().getMaxAge()).isEqualTo(httpListener.getCors().getAccessControlMaxAge());
        assertThat(result.getCors().getRunPolicies()).isEqualTo(httpListener.getCors().isRunPolicies());
    }

    @Test
    void shouldMapHttpListenerWithNullCors() {
        HttpListener httpListener = mock(HttpListener.class);
        when(httpListener.getCors()).thenReturn(null);

        var result = importExportApiMapper.mapHttpListener(httpListener);

        assertThat(result).isNotNull();
        assertThat(result.getCors()).isNull();
    }

    @Test
    void shouldUseCorsMappperForHttpListener() {
        HttpListener httpListener = ListenerModelFixtures.aModelHttpListener();
        var expectedCors = CorsMapper.INSTANCE.map(httpListener.getCors());
        var result = importExportApiMapper.mapHttpListener(httpListener);

        assertThat(result).isNotNull();
        assertThat(result.getCors()).isNotNull();

        assertThat(result.getCors().getEnabled()).isEqualTo(expectedCors.getEnabled());
        assertThat(result.getCors().getAllowOrigin()).isEqualTo(expectedCors.getAllowOrigin());
        assertThat(result.getCors().getAllowHeaders()).isEqualTo(expectedCors.getAllowHeaders());
        assertThat(result.getCors().getAllowMethods()).isEqualTo(expectedCors.getAllowMethods());
        assertThat(result.getCors().getAllowCredentials()).isEqualTo(expectedCors.getAllowCredentials());
        assertThat(result.getCors().getExposeHeaders()).isEqualTo(expectedCors.getExposeHeaders());
        assertThat(result.getCors().getMaxAge()).isEqualTo(expectedCors.getMaxAge());
        assertThat(result.getCors().getRunPolicies()).isEqualTo(expectedCors.getRunPolicies());
=======
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.management.v2.rest.model.MembershipMemberType;
import io.gravitee.rest.api.management.v2.rest.model.PrimaryOwner;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImportExportApiMapperTest {

    @Test
    void should_map_primary_owner_and_media_fields() {
        // Given
        var po = new PrimaryOwner().id("po-id").email("po@gravitee.io").displayName("John Doe").type(MembershipMemberType.USER);

        var api = new ApiV4();
        api.setPrimaryOwner(po);

        var exportApiV4 = new ExportApiV4();
        exportApiV4.setApi(api);
        exportApiV4.setApiPicture("picture-bytes");
        exportApiV4.setApiBackground("bg-bytes");

        // When
        ImportDefinition importDefinition = ImportExportApiMapper.INSTANCE.toImportDefinition(exportApiV4);

        // Then
        var apiExport = importDefinition.getApiExport();
        assertThat(apiExport).isNotNull();
        assertThat(apiExport.getPicture()).isEqualTo("picture-bytes");
        assertThat(apiExport.getBackground()).isEqualTo("bg-bytes");
        assertThat(apiExport.getPrimaryOwner()).isNotNull();
        assertThat(apiExport.getPrimaryOwner().id()).isEqualTo("po-id");
        assertThat(apiExport.getPrimaryOwner().email()).isEqualTo("po@gravitee.io");
        assertThat(apiExport.getPrimaryOwner().displayName()).isEqualTo("John Doe");
        assertThat(apiExport.getPrimaryOwner().type()).isEqualTo(PrimaryOwnerEntity.Type.USER);
    }

    @Test
    void should_default_primary_owner_type_to_user_when_null() {
        // Given
        var po = new PrimaryOwner().id("po-id").email("po@gravitee.io").displayName("John Doe"); // no type set

        var api = new ApiV4();
        api.setPrimaryOwner(po);

        var exportApiV4 = new ExportApiV4();
        exportApiV4.setApi(api);

        // When
        var importDefinition = ImportExportApiMapper.INSTANCE.toImportDefinition(exportApiV4);

        // Then
        assertThat(importDefinition.getApiExport().getPrimaryOwner()).isNotNull();
        assertThat(importDefinition.getApiExport().getPrimaryOwner().type()).isEqualTo(PrimaryOwnerEntity.Type.USER);
    }

    @Test
    void should_not_set_primary_owner_when_absent() {
        // Given
        var api = new ApiV4();
        // no primary owner
        var exportApiV4 = new ExportApiV4();
        exportApiV4.setApi(api);

        // When
        var importDefinition = ImportExportApiMapper.INSTANCE.toImportDefinition(exportApiV4);

        // Then
        assertThat(importDefinition.getApiExport().getPrimaryOwner()).isNull();
>>>>>>> 5832b9666c (fix: set primary owner for v4 api export)
    }

    @Test
    void shouldMapPrimaryOwnerAndMediaFields() {
        // Given
        var po = new PrimaryOwner().id("po-id").email("po@gravitee.io").displayName("John Doe").type(MembershipMemberType.USER);

        var api = new ApiV4();
        api.setPrimaryOwner(po);

        var exportApiV4 = new ExportApiV4();
        exportApiV4.setApi(api);
        exportApiV4.setApiPicture("picture-bytes");
        exportApiV4.setApiBackground("bg-bytes");

        // When
        ImportDefinition importDefinition = ImportExportApiMapper.INSTANCE.toImportDefinition(exportApiV4);

        // Then
        var apiExport = importDefinition.getApiExport();
        assertThat(apiExport).isNotNull();
        assertThat(apiExport.getPicture()).isEqualTo("picture-bytes");
        assertThat(apiExport.getBackground()).isEqualTo("bg-bytes");
        assertThat(apiExport.getPrimaryOwner()).isNotNull();
        assertThat(apiExport.getPrimaryOwner().id()).isEqualTo("po-id");
        assertThat(apiExport.getPrimaryOwner().email()).isEqualTo("po@gravitee.io");
        assertThat(apiExport.getPrimaryOwner().displayName()).isEqualTo("John Doe");
        assertThat(apiExport.getPrimaryOwner().type()).isEqualTo(PrimaryOwnerEntity.Type.USER);
    }

    @Test
    void shouldDefaultPrimaryOwnerTypeToUserWhenNull() {
        // Given
        var po = new PrimaryOwner().id("po-id").email("po@gravitee.io").displayName("John Doe"); // no type set

        var api = new ApiV4();
        api.setPrimaryOwner(po);

        var exportApiV4 = new ExportApiV4();
        exportApiV4.setApi(api);

        // When
        var importDefinition = ImportExportApiMapper.INSTANCE.toImportDefinition(exportApiV4);

        // Then
        assertThat(importDefinition.getApiExport().getPrimaryOwner()).isNotNull();
        assertThat(importDefinition.getApiExport().getPrimaryOwner().type()).isEqualTo(PrimaryOwnerEntity.Type.USER);
    }

    @Test
    void shouldNotSetPrimaryOwnerWhenAbsent() {
        // Given
        var api = new ApiV4();
        // no primary owner
        var exportApiV4 = new ExportApiV4();
        exportApiV4.setApi(api);

        // When
        var importDefinition = ImportExportApiMapper.INSTANCE.toImportDefinition(exportApiV4);

        // Then
        assertThat(importDefinition.getApiExport().getPrimaryOwner()).isNull();
    }
}
