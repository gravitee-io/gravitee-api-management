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

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.PageLinks;
import io.gravitee.rest.api.portal.rest.model.PagesResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PagesResourceTest extends AbstractResourceTest {

    protected String contextPath() {
        return "pages";
    }

    @Before
    public void init() throws IOException {
        resetAllMocks();

        doReturn(new Page()).when(pageMapper).convert(any());
        doReturn(new PageLinks()).when(pageMapper).computePageLinks(any(), any());
    }

    @Test
    public void shouldGetPagesIfAuthorizeAndPublishedPageAndNotSystemFolder() {
        PageEntity publishedPage = new PageEntity();
        publishedPage.setPublished(true);
        doReturn(singletonList(publishedPage)).when(pageService).search(any(), isNull());

        doReturn(true).when(groupService).isUserAuthorizedToAccessPortalData(any(), any());

        final Response response = target().request().get();

        assertEquals(OK_200, response.getStatus());

        final PagesResponse pagesResponse = response.readEntity(PagesResponse.class);
        List<Page> pages = pagesResponse.getData();
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertNotNull(pages.get(0).getLinks());
    }

    @Test
    public void shouldGetNoPageIfNotAuthorizeAndPublishedPageAndNotSystemFolder() {
        PageEntity publishedPage = new PageEntity();
        publishedPage.setPublished(true);
        doReturn(singletonList(publishedPage)).when(pageService).search(any(), isNull());

        doReturn(false).when(groupService).isUserAuthorizedToAccessPortalData(any(), any());

        Response response = target().request().get();
        assertEquals(OK_200, response.getStatus());

        PagesResponse pagesResponse = response.readEntity(PagesResponse.class);

        List<Page> pages = pagesResponse.getData();
        assertNotNull(pages);
        assertEquals(0, pages.size());
    }

    @Test
    public void shouldGetNoPageIfAuthorizeAndPublishedPageAndSystemFolder() {
        PageEntity publishedPage = new PageEntity();
        publishedPage.setPublished(true);
        publishedPage.setType("SYSTEM_FOLDER");
        doReturn(singletonList(publishedPage)).when(pageService).search(any(), isNull());

        doReturn(true).when(groupService).isUserAuthorizedToAccessPortalData(any(), any());

        Response response = target().request().get();
        assertEquals(OK_200, response.getStatus());

        PagesResponse pagesResponse = response.readEntity(PagesResponse.class);

        List<Page> pages = pagesResponse.getData();
        assertNotNull(pages);
        assertEquals(0, pages.size());
    }

    @Test
    public void shouldGetNoPageIfAuthorizeAndNotPublished() {
        doReturn(Collections.emptyList()).when(pageService).search(any(), isNull());

        Response response = target().request().get();
        assertEquals(OK_200, response.getStatus());

        PagesResponse pagesResponse = response.readEntity(PagesResponse.class);

        List<Page> pages = pagesResponse.getData();
        assertNotNull(pages);
        assertEquals(0, pages.size());
    }
}
