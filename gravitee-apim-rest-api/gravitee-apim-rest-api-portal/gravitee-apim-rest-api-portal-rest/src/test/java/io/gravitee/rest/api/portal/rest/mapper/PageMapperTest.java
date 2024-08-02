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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.PageConfigurationKeys;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageMediaEntity;
import io.gravitee.rest.api.portal.rest.model.Metadata;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.Page.TypeEnum;
import io.gravitee.rest.api.portal.rest.model.PageConfiguration;
import io.gravitee.rest.api.portal.rest.model.PageConfiguration.DocExpansionEnum;
import io.gravitee.rest.api.portal.rest.model.PageConfiguration.ViewerEnum;
import io.gravitee.rest.api.portal.rest.model.PageLinks;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.UriBuilder;
import java.time.Instant;
import java.util.*;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageMapperTest {

    private static final String PAGE_ID = "my-page-id";
    private static final String PAGE_CONFIGURATION_DISPLAY_OPERATION_ID = "false";
    private static final String PAGE_CONFIGURATION_DOC_EXPANSION = "list";
    private static final String PAGE_CONFIGURATION_ENABLE_FILTERING = "true";
    private static final String PAGE_CONFIGURATION_MAX_DISPLAYED_TAGS = "42";
    private static final String PAGE_CONFIGURATION_SHOW_COMMON_EXTENSIONS = "false";
    private static final String PAGE_CONFIGURATION_SHOW_EXTENSIONS = "true";
    private static final String PAGE_CONFIGURATION_SHOW_URL = "false";
    private static final String PAGE_CONFIGURATION_TRY_IT = "true";
    private static final String PAGE_CONFIGURATION_TRY_IT_ANONYMOUS = "false";
    private static final String PAGE_CONFIGURATION_TRY_IT_URL = "http://try.it/url";
    private static final String PAGE_CONFIGURATION_VIEWER = "Redoc";
    private static final String PAGE_CONFIGURATION_USE_PKCE = "false";
    private static final String PAGE_CONTRIBUTOR = "my-page-contributor";
    private static final String PAGE_NAME = "my-page-name";
    private static final String PAGE_PARENT = "my-page-parent";
    private static final String PAGE_TYPE = "SWAGGER";
    private static final String API_ID = "api#1";

    @InjectMocks
    private PageMapper pageMapper;

    @Mock
    MediaService mediaService;

    public void setup() {}

    @Test
    public void should_convert_api_page() {
        //init
        PageEntity pageEntity = new PageEntity();

        pageEntity.setLastContributor(PAGE_CONTRIBUTOR);

        Map<String, String> configuration = new HashMap<>();
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_DISPLAY_OPERATION_ID, PAGE_CONFIGURATION_DISPLAY_OPERATION_ID);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_DOC_EXPANSION, PAGE_CONFIGURATION_DOC_EXPANSION);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_ENABLE_FILTERING, PAGE_CONFIGURATION_ENABLE_FILTERING);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_MAX_DISPLAYED_TAGS, PAGE_CONFIGURATION_MAX_DISPLAYED_TAGS);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_SHOW_COMMON_EXTENSIONS, PAGE_CONFIGURATION_SHOW_COMMON_EXTENSIONS);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_SHOW_EXTENSIONS, PAGE_CONFIGURATION_SHOW_EXTENSIONS);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_SHOW_URL, PAGE_CONFIGURATION_SHOW_URL);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_TRY_IT, PAGE_CONFIGURATION_TRY_IT);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_TRY_IT_ANONYMOUS, PAGE_CONFIGURATION_TRY_IT_ANONYMOUS);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_TRY_IT_URL, PAGE_CONFIGURATION_TRY_IT_URL);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_USE_PKCE, PAGE_CONFIGURATION_USE_PKCE);
        configuration.put(PageConfigurationKeys.SWAGGER_VIEWER, PAGE_CONFIGURATION_VIEWER);
        pageEntity.setConfiguration(configuration);
        pageEntity.setId(PAGE_ID);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("meta", PAGE_ID);
        pageEntity.setMetadata(metadata);

        pageEntity.setName(PAGE_NAME);
        pageEntity.setOrder(1);
        pageEntity.setParentId(PAGE_PARENT);
        pageEntity.setType(PAGE_TYPE);
        PageMediaEntity pme1 = new PageMediaEntity();
        pme1.setMediaHash("media_id_1");
        pme1.setMediaName("media_name_1");
        PageMediaEntity pme2 = new PageMediaEntity();
        pme2.setMediaHash("media_id_2");
        pme2.setMediaName("media_name_2");
        final List<PageMediaEntity> attachedMedia = Arrays.asList(pme1, pme2);
        pageEntity.setAttachedMedia(attachedMedia);
        Instant now = Instant.now();
        pageEntity.setLastModificationDate(Date.from(now));

        when(mediaService.findAllWithoutContent(attachedMedia, API_ID))
            .thenReturn(Arrays.asList(mock(MediaEntity.class), mock(MediaEntity.class)));

        //Test
        Page responsePage = pageMapper.convert(UriBuilder.fromPath("/"), API_ID, pageEntity);
        assertNotNull(responsePage);

        PageConfiguration pageConfiguration = responsePage.getConfiguration();
        assertNotNull(pageConfiguration);
        assertFalse(pageConfiguration.getDisplayOperationId());
        assertEquals(DocExpansionEnum.LIST, pageConfiguration.getDocExpansion());
        assertTrue(pageConfiguration.getEnableFiltering());
        assertEquals(42, pageConfiguration.getMaxDisplayedTags());
        assertFalse(pageConfiguration.getShowCommonExtensions());
        assertTrue(pageConfiguration.getShowExtensions());
        assertFalse(pageConfiguration.getShowUrl());
        assertTrue(pageConfiguration.getTryIt());
        assertFalse(pageConfiguration.getTryItAnonymous());
        assertFalse(pageConfiguration.getUsePkce());
        assertEquals(PAGE_CONFIGURATION_TRY_IT_URL, pageConfiguration.getTryItUrl());
        assertEquals(ViewerEnum.REDOC, pageConfiguration.getViewer());

        assertEquals(PAGE_ID, responsePage.getId());

        List<Metadata> metadatas = responsePage.getMetadata();
        assertNotNull(metadatas);
        assertEquals(1, metadatas.size());
        Metadata m = metadatas.get(0);
        assertEquals("0", m.getOrder());
        assertEquals("meta", m.getName());
        assertEquals(PAGE_ID, m.getValue());

        assertEquals(PAGE_NAME, responsePage.getName());
        assertEquals(Integer.valueOf(1), responsePage.getOrder());
        assertEquals(PAGE_PARENT, responsePage.getParent());

        assertNotNull(responsePage.getMedia());
        assertEquals(2, responsePage.getMedia().size());

        assertEquals(TypeEnum.SWAGGER, responsePage.getType());
        assertEquals(now.toEpochMilli(), responsePage.getUpdatedAt().toInstant().toEpochMilli());
    }

    @Test
    public void should_convert_org_page() {
        //init
        PageEntity pageEntity = new PageEntity();

        pageEntity.setLastContributor(PAGE_CONTRIBUTOR);

        Map<String, String> configuration = new HashMap<>();
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_DISPLAY_OPERATION_ID, PAGE_CONFIGURATION_DISPLAY_OPERATION_ID);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_DOC_EXPANSION, PAGE_CONFIGURATION_DOC_EXPANSION);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_ENABLE_FILTERING, PAGE_CONFIGURATION_ENABLE_FILTERING);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_MAX_DISPLAYED_TAGS, PAGE_CONFIGURATION_MAX_DISPLAYED_TAGS);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_SHOW_COMMON_EXTENSIONS, PAGE_CONFIGURATION_SHOW_COMMON_EXTENSIONS);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_SHOW_EXTENSIONS, PAGE_CONFIGURATION_SHOW_EXTENSIONS);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_SHOW_URL, PAGE_CONFIGURATION_SHOW_URL);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_TRY_IT, PAGE_CONFIGURATION_TRY_IT);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_TRY_IT_ANONYMOUS, PAGE_CONFIGURATION_TRY_IT_ANONYMOUS);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_TRY_IT_URL, PAGE_CONFIGURATION_TRY_IT_URL);
        configuration.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_USE_PKCE, PAGE_CONFIGURATION_USE_PKCE);
        configuration.put(PageConfigurationKeys.SWAGGER_VIEWER, PAGE_CONFIGURATION_VIEWER);
        pageEntity.setConfiguration(configuration);
        pageEntity.setId(PAGE_ID);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("meta", PAGE_ID);
        pageEntity.setMetadata(metadata);

        pageEntity.setName(PAGE_NAME);
        pageEntity.setOrder(1);
        pageEntity.setParentId(PAGE_PARENT);
        pageEntity.setType(PAGE_TYPE);
        PageMediaEntity pme1 = new PageMediaEntity();
        pme1.setMediaHash("media_id_1");
        pme1.setMediaName("media_name_1");
        PageMediaEntity pme2 = new PageMediaEntity();
        pme2.setMediaHash("media_id_2");
        pme2.setMediaName("media_name_2");
        final List<PageMediaEntity> attachedMedia = Arrays.asList(pme1, pme2);
        pageEntity.setAttachedMedia(attachedMedia);
        Instant now = Instant.now();
        pageEntity.setLastModificationDate(Date.from(now));

        when(mediaService.findAllWithoutContent(GraviteeContext.getExecutionContext(), attachedMedia))
            .thenReturn(Arrays.asList(mock(MediaEntity.class), mock(MediaEntity.class)));

        //Test
        Page responsePage = pageMapper.convert(UriBuilder.fromPath("/"), null, pageEntity);
        assertNotNull(responsePage);

        PageConfiguration pageConfiguration = responsePage.getConfiguration();
        assertNotNull(pageConfiguration);
        assertFalse(pageConfiguration.getDisplayOperationId());
        assertEquals(DocExpansionEnum.LIST, pageConfiguration.getDocExpansion());
        assertTrue(pageConfiguration.getEnableFiltering());
        assertEquals(42, pageConfiguration.getMaxDisplayedTags());
        assertFalse(pageConfiguration.getShowCommonExtensions());
        assertTrue(pageConfiguration.getShowExtensions());
        assertFalse(pageConfiguration.getShowUrl());
        assertTrue(pageConfiguration.getTryIt());
        assertFalse(pageConfiguration.getTryItAnonymous());
        assertFalse(pageConfiguration.getUsePkce());
        assertEquals(PAGE_CONFIGURATION_TRY_IT_URL, pageConfiguration.getTryItUrl());
        assertEquals(ViewerEnum.REDOC, pageConfiguration.getViewer());

        assertEquals(PAGE_ID, responsePage.getId());

        List<Metadata> metadatas = responsePage.getMetadata();
        assertNotNull(metadatas);
        assertEquals(1, metadatas.size());
        Metadata m = metadatas.get(0);
        assertEquals("0", m.getOrder());
        assertEquals("meta", m.getName());
        assertEquals(PAGE_ID, m.getValue());

        assertEquals(PAGE_NAME, responsePage.getName());
        assertEquals(Integer.valueOf(1), responsePage.getOrder());
        assertEquals(PAGE_PARENT, responsePage.getParent());

        assertNotNull(responsePage.getMedia());
        assertEquals(2, responsePage.getMedia().size());

        assertEquals(TypeEnum.SWAGGER, responsePage.getType());
        assertEquals(now.toEpochMilli(), responsePage.getUpdatedAt().toInstant().toEpochMilli());
    }

    @Test
    public void should_convert_minimal_page() {
        //init
        PageEntity minimalPageEntity = new PageEntity();
        minimalPageEntity.setType(PAGE_TYPE);

        Instant now = Instant.now();
        minimalPageEntity.setLastModificationDate(Date.from(now));

        //Test
        Page responsePage = pageMapper.convert(minimalPageEntity);
        assertNotNull(responsePage);

        PageConfiguration pageConfiguration = responsePage.getConfiguration();
        assertNull(pageConfiguration);

        List<Metadata> metadatas = responsePage.getMetadata();
        assertNull(metadatas);
        assertEquals(TypeEnum.SWAGGER, responsePage.getType());
        assertEquals(now.toEpochMilli(), responsePage.getUpdatedAt().toInstant().toEpochMilli());
    }

    @Test
    public void testPageLinks() {
        String basePath = "/" + PAGE_ID;
        String parentPath = "/" + PAGE_PARENT;

        PageLinks links = pageMapper.computePageLinks(basePath, parentPath);

        assertNotNull(links);

        assertEquals(basePath, links.getSelf());
        assertEquals(basePath + "/content", links.getContent());
        assertEquals(parentPath, links.getParent());
    }
}
