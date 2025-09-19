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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPagesResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";

    protected String contextPath() {
        return "apis/";
    }

    @Before
    public void init() throws IOException {
        resetAllMocks();

        ApiEntity mockApi = new ApiEntity();
        mockApi.setId(API);
        doReturn(mockApi).when(apiService).findById(GraviteeContext.getExecutionContext(), API);

        PageEntity markdownTemplate = new PageEntity();
        markdownTemplate.setType(PageType.MARKDOWN_TEMPLATE.name());
        doReturn(Arrays.asList(new PageEntity(), markdownTemplate))
            .when(pageService)
            .search(eq(GraviteeContext.getCurrentEnvironment()), any(), isNull());

        when(
            accessControlService.canAccessPageFromPortal(eq(GraviteeContext.getExecutionContext()), eq(API), any(PageEntity.class))
        ).thenAnswer(invocationOnMock -> {
            PageEntity page = invocationOnMock.getArgument(2);
            return !PageType.MARKDOWN_TEMPLATE.name().equals(page.getType());
        });

        doReturn(new Page()).when(pageMapper).convert(any());
        doReturn(new PageLinks()).when(pageMapper).computePageLinks(any(), any());
    }

    @Test
    public void shouldNotFoundWhileGettingApiPages() {
        //init
        ApiEntity userApi = new ApiEntity();
        userApi.setId("1");
        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(false);

        //test
        final Response response = target(API).path("pages").request().get();
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
    public void shouldGetApiPages() {
        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(true);

        final Response response = target(API).path("pages").request().get();
        assertEquals(OK_200, response.getStatus());

        PagesResponse pagesResponse = response.readEntity(PagesResponse.class);

        List<Page> pages = pagesResponse.getData();
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertNotNull(pages.get(0).getLinks());
    }

    @Test
    public void shouldGetNoApiPage() {
        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(true);
        when(accessControlService.canAccessPageFromPortal(eq(GraviteeContext.getExecutionContext()), eq(API), any())).thenReturn(false);

        final Builder request = target(API).path("pages").request();

        Response response = request.get();
        assertEquals(OK_200, response.getStatus());

        PagesResponse pagesResponse = response.readEntity(PagesResponse.class);
        List<Page> pages = pagesResponse.getData();
        assertNotNull(pages);
        assertEquals(0, pages.size());
    }

    @Test
    public void shouldNotReturnEmptyFolder() {
        PageEntity emptyFolder = new PageEntity();
        emptyFolder.setType("FOLDER");
        emptyFolder.setId("not-a-parent");

        PageEntity folderWithChild = new PageEntity();
        folderWithChild.setId("parent");
        emptyFolder.setType("FOLDER");

        PageEntity child = new PageEntity();
        child.setId("child");
        child.setType("MARKDOWN");
        child.setParentId("parent");

        doReturn(Arrays.asList(emptyFolder, folderWithChild, child))
            .when(pageService)
            .search(eq(GraviteeContext.getCurrentEnvironment()), any(), isNull());

        when(
            accessControlService.canAccessPageFromPortal(eq(GraviteeContext.getExecutionContext()), eq(API), any(PageEntity.class))
        ).thenAnswer(invocationOnMock -> true);

        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(true);

        when(pageService.folderHasPublishedChildren(eq(emptyFolder.getId()))).thenReturn(false);
        when(pageService.folderHasPublishedChildren(eq(folderWithChild.getId()))).thenReturn(true);

        var folderWithChildMapped = new Page();
        folderWithChildMapped.setId(folderWithChild.getId());
        when(pageMapper.convert(eq(folderWithChild))).thenReturn(folderWithChildMapped);

        var childMapped = new Page();
        childMapped.setId(child.getId());
        when(pageMapper.convert(eq(child))).thenReturn(childMapped);

        final Response response = target(API).path("pages").request().get();
        assertEquals(OK_200, response.getStatus());

        PagesResponse pagesResponse = response.readEntity(PagesResponse.class);

        List<Page> pages = pagesResponse.getData();
        assertNotNull(pages);
        assertEquals(2, pages.size());
        assertEquals(pages.get(0).getId(), "parent");
        assertEquals(pages.get(1).getId(), "child");
    }
}
