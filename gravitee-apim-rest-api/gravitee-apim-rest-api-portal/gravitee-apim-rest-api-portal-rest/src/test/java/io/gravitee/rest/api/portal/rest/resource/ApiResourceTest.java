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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.Link.ResourceTypeEnum;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Before
    public void init() {
        resetAllMocks();

        ApiEntity mockApi = new ApiEntity();
        mockApi.setId(API);
        doReturn(mockApi).when(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), API);

        doReturn(true).when(accessControlService).canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API);
        doReturn(Set.of(API))
            .when(apiAuthorizationService)
            .findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(false));

        Api api = new Api();
        api.setId(API);
        doReturn(api).when(apiMapper).convert(eq(GraviteeContext.getExecutionContext()), any());
        doReturn(new Page()).when(pageMapper).convert(any());
        doReturn(new Plan()).when(planMapper).convert(any(), any());
        doReturn(new Rating()).when(ratingMapper).convert(eq(GraviteeContext.getExecutionContext()), any(), any());
    }

    @Test
    public void shouldGetApiWithoutIncluded() {
        final Response response = target(API).request().get();

        assertEquals(OK_200, response.getStatus());

        final Api responseApi = response.readEntity(Api.class);
        assertNotNull(responseApi);

        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        Mockito.verify(apiMapper, Mockito.times(1)).computeApiLinks(ac.capture(), ArgumentCaptor.forClass(Date.class).capture());

        String expectedBasePath = target(API).getUriBuilder().build().toString();
        List<String> bastPathList = ac.getAllValues();
        assertTrue(bastPathList.contains(expectedBasePath));
    }

    @Test
    public void shouldGetApiWithIncluded() {
        // mock pages
        PageEntity pagePublished = new PageEntity();
        pagePublished.setPublished(true);
        pagePublished.setType("SWAGGER");
        pagePublished.setLastModificationDate(Date.from(Instant.now()));
        pagePublished.setContent("some page content");
        doReturn(Arrays.asList(pagePublished)).when(pageService).search(eq(GraviteeContext.getCurrentEnvironment()), any());

        // mock plans
        PlanEntity plan1 = new PlanEntity();
        plan1.setId("A");
        plan1.setSecurity(PlanSecurityType.API_KEY);
        plan1.setValidation(PlanValidationType.AUTO);
        plan1.setStatus(PlanStatus.PUBLISHED);

        PlanEntity plan2 = new PlanEntity();
        plan2.setId("B");
        plan2.setSecurity(PlanSecurityType.KEY_LESS);
        plan2.setValidation(PlanValidationType.MANUAL);
        plan2.setStatus(PlanStatus.PUBLISHED);

        PlanEntity plan3 = new PlanEntity();
        plan3.setId("C");
        plan3.setSecurity(PlanSecurityType.KEY_LESS);
        plan3.setValidation(PlanValidationType.MANUAL);
        plan3.setStatus(PlanStatus.CLOSED);

        doReturn(new HashSet<>(Arrays.asList(plan1, plan2, plan3)))
            .when(planSearchService)
            .findByApi(GraviteeContext.getExecutionContext(), API);

        // For pages
        doReturn(true).when(accessControlService).canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API);
        doReturn(true).when(accessControlService).canAccessPageFromPortal(GraviteeContext.getExecutionContext(), pagePublished);
        // For plans
        doReturn(true).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());

        // test
        final Response response = target(API).queryParam("include", "pages", "plans").request().get();

        assertEquals(OK_200, response.getStatus());

        final Api responseApi = response.readEntity(Api.class);
        assertNotNull(responseApi);

        List<Page> pages = responseApi.getPages();
        assertNotNull(pages);
        assertEquals(1, pages.size());

        final List<Plan> plans = responseApi.getPlans();
        assertNotNull(plans);
        assertEquals(2, plans.size());
    }

    @Test
    public void shouldHaveNotFoundWhileGettingApi() {
        // init
        ApiEntity userApi = new ApiEntity();
        userApi.setId("1");
        doReturn(false).when(accessControlService).canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API);

        // test
        final Response response = target(API).request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.api.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Api [" + API + "] cannot be found.", error.getMessage());
    }

    @Test
    public void shouldGetApiPicture() throws IOException, URISyntaxException {
        // init
        InlinePictureEntity mockImage = new InlinePictureEntity();
        byte[] apiLogoContent = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("media/logo.svg").toURI()));
        mockImage.setContent(apiLogoContent);
        mockImage.setType("image/svg");
        doReturn(mockImage).when(apiService).getPicture(GraviteeContext.getExecutionContext(), API);

        // test
        final Response response = target(API).path("picture").request().get();
        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void shouldHaveNotFoundWhileGettingApiPicture() {
        // init
        when(apiAuthorizationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(false)))
            .thenReturn(Set.of("1"));

        // test
        final Response response = target(API).path("picture").request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.api.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Api [" + API + "] cannot be found.", error.getMessage());
    }

    @Test
    public void shouldGetApiLinks() {
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

        when(pageService.search(eq(GraviteeContext.getCurrentEnvironment()), any(PageQuery.class), isNull()))
            .thenAnswer(
                (Answer<List<PageEntity>>) invocation -> {
                    PageQuery pq = invocation.getArgument(1);
                    if (PageType.SYSTEM_FOLDER.equals(pq.getType()) && API.equals(pq.getApi())) {
                        return Collections.singletonList(sysFolder);
                    } else if ("SYS_FOLDER".equals(pq.getParent()) && API.equals(pq.getApi())) {
                        return Arrays.asList(linkSysFolder, swaggerSysFolder, folderSysFolder, markdownTemplate);
                    } else if ("FOLDER_SYS_FOLDER".equals(pq.getParent()) && API.equals(pq.getApi())) {
                        return Collections.singletonList(markdownFolderSysFolder);
                    }
                    return null;
                }
            );

        when(accessControlService.canAccessPageFromPortal(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(true);

        final Response response = target(API).path("links").request().get();
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
