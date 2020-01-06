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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.PortalConfigEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.portal.rest.model.CategorizedLinks;
import io.gravitee.rest.api.portal.rest.model.Link;
import io.gravitee.rest.api.portal.rest.model.LinksResponse;
import io.gravitee.rest.api.portal.rest.model.Link.ResourceTypeEnum;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConfigurationResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "configuration";
    }
    
    @Test
    public void shouldGetConfiguration() {
        resetAllMocks();
        
        PortalConfigEntity configEntity = new PortalConfigEntity();
        doReturn(configEntity).when(configService).getPortalConfig();
        
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(configMapper).convert(configEntity);
        Mockito.verify(configService).getPortalConfig();
    }
    
    @Test
    public void shouldGetPortalLinks() {
        resetAllMocks();
        
        PageEntity sysFolder = new PageEntity();
        sysFolder.setId("SYS_FOLDER");
        sysFolder.setType("SYSTEM_FOLDER");
        sysFolder.setName("SYSTEM_FOLDER");
        sysFolder.setPublished(true);
        
        PageEntity linkSysFolder = new PageEntity();
        linkSysFolder.setId("LINK_SYS_FOLDER");
        linkSysFolder.setParentId("SYS_FOLDER");
        linkSysFolder.setType("LINK");
        linkSysFolder.setName("LINK");
        linkSysFolder.setPublished(true);
        Map<String, String> linkConf = new HashMap<>();
        linkConf.put("resourceRef", "LINK_RES_REF");
        linkConf.put("resourceType", "external");
        linkSysFolder.setConfiguration(linkConf);
        
        PageEntity swaggerSysFolder = new PageEntity();
        swaggerSysFolder.setId("SWAGGER_SYS_FOLDER");
        swaggerSysFolder.setParentId("SYS_FOLDER");
        swaggerSysFolder.setType("SWAGGER");
        swaggerSysFolder.setName("SWAGGER");
        swaggerSysFolder.setPublished(true);
        
        PageEntity folderSysFolder = new PageEntity();
        folderSysFolder.setId("FOLDER_SYS_FOLDER");
        folderSysFolder.setParentId("SYS_FOLDER");
        folderSysFolder.setType("FOLDER");
        folderSysFolder.setName("FOLDER");
        folderSysFolder.setPublished(true);
        
        PageEntity markdownFolderSysFolder = new PageEntity();
        markdownFolderSysFolder.setId("MARKDOWN_FOLDER_SYS_FOLDER");
        markdownFolderSysFolder.setParentId("FOLDER_SYS_FOLDER");
        markdownFolderSysFolder.setType("MARKDOWN");
        markdownFolderSysFolder.setName("MARKDOWN");
        markdownFolderSysFolder.setPublished(true);
        
        when(pageService.search(any(PageQuery.class))).thenAnswer(new Answer<List<PageEntity>>() {

            @Override
            public List<PageEntity> answer(InvocationOnMock invocation) throws Throwable {
                PageQuery pq = invocation.getArgument(0);
                if(PageType.SYSTEM_FOLDER.equals(pq.getType())) {
                    return Arrays.asList(sysFolder);
                } else if ("SYS_FOLDER".equals(pq.getParent())) {
                    return Arrays.asList(linkSysFolder, swaggerSysFolder, folderSysFolder);
                } else if ("FOLDER_SYS_FOLDER".equals(pq.getParent())) {
                    return Arrays.asList(markdownFolderSysFolder);
                }
                return null;
            }
        });
        
        final Response response = target("/links").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        final LinksResponse links = response.readEntity(LinksResponse.class);
        assertNotNull(links);
        Map<String, List<CategorizedLinks>> slots = links.getSlots();
        assertNotNull(slots);
        assertEquals(1, slots.size());
        
        List<CategorizedLinks> sysFolderList = slots.get("system_folder");
        assertNotNull(sysFolderList);
        assertEquals(2, sysFolderList.size());
        
        CategorizedLinks rootCat = sysFolderList.get(0);
        assertNotNull(rootCat);
        assertTrue(rootCat.getRoot());
        assertEquals("SYSTEM_FOLDER", rootCat.getCategory());
        List<Link> rootCatLinks = rootCat.getLinks();
        assertNotNull(rootCatLinks);
        assertEquals(2, rootCatLinks.size());
        Link rootCatLink = rootCatLinks.get(0);
        assertNotNull(rootCatLink);
        assertEquals("LINK", rootCatLink.getName());
        assertEquals("LINK_RES_REF", rootCatLink.getResourceRef());
        assertEquals(ResourceTypeEnum.EXTERNAL, rootCatLink.getResourceType());
        assertNull(rootCatLink.getFolder());

        Link rootCatSwagger = rootCatLinks.get(1);
        assertNotNull(rootCatSwagger);
        assertEquals("SWAGGER", rootCatSwagger.getName());
        assertEquals("SWAGGER_SYS_FOLDER", rootCatSwagger.getResourceRef());
        assertEquals(ResourceTypeEnum.PAGE, rootCatSwagger.getResourceType());
        
        CategorizedLinks folderCat = sysFolderList.get(1);
        assertNotNull(folderCat);
        assertFalse(folderCat.getRoot());
        assertEquals("FOLDER", folderCat.getCategory());
        List<Link> folderCatLinks = folderCat.getLinks();
        assertNotNull(folderCatLinks);
        assertEquals(1, folderCatLinks.size());
        Link folderCatMarkdown = folderCatLinks.get(0);
        assertNotNull(folderCatMarkdown);
        assertEquals("MARKDOWN", folderCatMarkdown.getName());
        assertEquals("MARKDOWN_FOLDER_SYS_FOLDER", folderCatMarkdown.getResourceRef());
        assertEquals(ResourceTypeEnum.PAGE, folderCatMarkdown.getResourceType());
    }
}
