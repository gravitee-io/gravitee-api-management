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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.PageConfigurationKeys;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.configuration.application.ApplicationGrantTypeEntity;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypeEntity;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypesEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.settings.ConsoleSettingsEntity;
import io.gravitee.rest.api.model.settings.PortalSettingsEntity;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Link.ResourceTypeEnum;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.*;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

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

        PortalSettingsEntity portalConfigEntity = new PortalSettingsEntity();
        ConsoleSettingsEntity consoleSettingsEntity = new ConsoleSettingsEntity();
        doReturn(portalConfigEntity).when(configService).getPortalSettings();
        doReturn(consoleSettingsEntity).when(configService).getConsoleSettings();
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(configMapper).convert(portalConfigEntity, consoleSettingsEntity);
        Mockito.verify(configService).getPortalSettings();
        Mockito.verify(configService).getConsoleSettings();
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
        linkSysFolder.setContent("LINK_RES_REF");
        linkSysFolder.setPublished(true);
        Map<String, String> linkConf = new HashMap<>();
        linkConf.put(PageConfigurationKeys.LINK_RESOURCE_TYPE, "external");
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

        PageEntity markdownTemplate = new PageEntity();
        markdownTemplate.setId("MARKDOWN_TEMPLATE");
        markdownTemplate.setParentId("SYS_FOLDER");
        markdownTemplate.setType("MARKDOWN_TEMPLATE");
        markdownTemplate.setName("MARKDOWN_TEMPLATE");
        markdownTemplate.setPublished(true);

        when(pageService.search(any(PageQuery.class), isNull(), eq(GraviteeContext.getCurrentEnvironment())))
            .thenAnswer(
                (Answer<List<PageEntity>>) invocation -> {
                    PageQuery pq = invocation.getArgument(0);
                    if (PageType.SYSTEM_FOLDER.equals(pq.getType())) {
                        return Arrays.asList(sysFolder);
                    } else if ("SYS_FOLDER".equals(pq.getParent())) {
                        return Arrays.asList(linkSysFolder, swaggerSysFolder, folderSysFolder, markdownTemplate);
                    } else if ("FOLDER_SYS_FOLDER".equals(pq.getParent())) {
                        return Arrays.asList(markdownFolderSysFolder);
                    }
                    return null;
                }
            );

        when(accessControlService.canAccessPageFromPortal(any())).thenReturn(true);

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

    @Test
    public void shouldGetApplicationTypes() throws TechnicalException {
        resetAllMocks();

        ApplicationTypesEntity typesEntity = new ApplicationTypesEntity();
        List<ApplicationTypeEntity> data = new ArrayList<>();

        ApplicationTypeEntity simple = new ApplicationTypeEntity();
        simple.setId("simple");
        simple.setAllowed_grant_types(new ArrayList<>());
        simple.setDefault_grant_types(new ArrayList<>());
        simple.setMandatory_grant_types(new ArrayList<>());
        simple.setName("Simple");
        simple.setDescription("Simple type");
        data.add(simple);

        ApplicationTypeEntity web = new ApplicationTypeEntity();
        web.setId("web");
        List<ApplicationGrantTypeEntity> grantTypes = new ArrayList<>();
        ApplicationGrantTypeEntity grantType = new ApplicationGrantTypeEntity();
        grantType.setName("name");
        List<String> responses_types = new ArrayList<>();
        responses_types.add("token");
        grantType.setResponse_types(responses_types);
        grantTypes.add(grantType);
        web.setAllowed_grant_types(grantTypes);
        web.setDefault_grant_types(new ArrayList<>());
        web.setMandatory_grant_types(new ArrayList<>());
        web.setName("Web");
        web.setDescription("Web type");
        data.add(web);

        typesEntity.setData(data);
        when(applicationTypeService.getEnabledApplicationTypes()).thenReturn(typesEntity);

        final Response response = target().path("applications").path("types").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final ConfigurationApplicationTypesResponse appTypes = response.readEntity(ConfigurationApplicationTypesResponse.class);
        assertNotNull(appTypes);
        @Valid
        List<ApplicationType> types = appTypes.getData();
        assertNotNull(types);
        assertEquals(2, types.size());

        assertEquals("web", types.get(1).getId());
        assertEquals(1, types.get(1).getAllowedGrantTypes().size());
    }

    @Test
    public void shouldGetApplicationRoles() throws TechnicalException {
        resetAllMocks();

        RoleEntity appRoleEntity = new RoleEntity();
        appRoleEntity.setDefaultRole(true);
        appRoleEntity.setName("appRole");
        appRoleEntity.setSystem(false);
        when(roleService.findByScope(RoleScope.APPLICATION)).thenReturn(Collections.singletonList(appRoleEntity));

        final Response response = target().path("applications").path("roles").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final ConfigurationApplicationRolesResponse appRoles = response.readEntity(ConfigurationApplicationRolesResponse.class);
        assertNotNull(appRoles);
        @Valid
        List<ApplicationRole> roles = appRoles.getData();
        assertNotNull(roles);
        assertEquals(1, roles.size());

        assertEquals("appRole", roles.get(0).getId());
        assertEquals(true, roles.get(0).getDefault());
        assertEquals("appRole", roles.get(0).getName());
        assertEquals(false, roles.get(0).getSystem());
    }
}
