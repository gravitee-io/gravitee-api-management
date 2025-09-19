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
package io.gravitee.rest.api.management.rest.resource;

import static jakarta.ws.rs.client.Entity.json;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.NewTagEntity;
import io.gravitee.rest.api.model.TagEntity;
import io.gravitee.rest.api.model.UpdateTagEntity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TagsResourceTest extends AbstractResourceTest {

    @Inject
    private LicenseManager licenseManager;

    private License license;

    @Before
    public void init() {
        license = mock(License.class);
        when(licenseManager.getPlatformLicense()).thenReturn(license);
    }

    @Override
    protected String contextPath() {
        return "configuration/tags/";
    }

    @Test
    public void should_forbid_create_tag_without_sharding_tags_feature() {
        when(license.isFeatureEnabled("apim-sharding-tags")).thenReturn(false);
        when(permissionService.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
        NewTagEntity newTag = new NewTagEntity();
        newTag.setName("tag-name");
        Response response = orgTarget().request().post(json(newTag));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_allow_create_tag_with_sharding_tags_feature() {
        when(license.isFeatureEnabled("apim-sharding-tags")).thenReturn(true);
        when(permissionService.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
        NewTagEntity newTag = new NewTagEntity();
        newTag.setName("tag-name");
        TagEntity tag = new TagEntity();
        tag.setId("tag-id");
        when(tagService.create(any(), any(NewTagEntity.class), any(), any())).thenReturn(tag);
        Response response = orgTarget().request().post(json(newTag));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        TagEntity tagEntity = response.readEntity(TagEntity.class);
        assertEquals(tag.getId(), tagEntity.getId());
    }

    @Test
    public void should_forbid_update_tag_without_sharding_tags_feature() {
        when(license.isFeatureEnabled("apim-sharding-tags")).thenReturn(false);
        when(permissionService.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
        UpdateTagEntity tag = new UpdateTagEntity();
        tag.setName("tag-name");
        Response response = orgTarget("tag-id").request().put(json(tag));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_allow_update_tag_with_sharding_tags_feature() {
        when(license.isFeatureEnabled("apim-sharding-tags")).thenReturn(true);
        when(permissionService.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
        when(tagService.update(any(), any(UpdateTagEntity.class), anyString(), any())).thenAnswer(i -> {
            final TagEntity tagEntity = new TagEntity();
            tagEntity.setName(i.<UpdateTagEntity>getArgument(1).getName());
            return tagEntity;
        });

        UpdateTagEntity tag = new UpdateTagEntity();
        tag.setName("tag-name");
        Response response = orgTarget("tag-id").request().put(json(tag));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        TagEntity tagEntity = response.readEntity(TagEntity.class);
        assertEquals(tag.getName(), tagEntity.getName());
    }

    @Test
    public void should_forbid_delete_tag_without_sharding_tags_feature() {
        when(license.isFeatureEnabled("apim-sharding-tags")).thenReturn(false);
        when(permissionService.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
        Response response = orgTarget().path("tag-id").request().delete();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_allow_delete_tag_with_sharding_tags_feature() {
        when(license.isFeatureEnabled("apim-sharding-tags")).thenReturn(true);
        when(permissionService.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
        Response response = orgTarget().path("tag-id").request().delete();
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
    }
}
