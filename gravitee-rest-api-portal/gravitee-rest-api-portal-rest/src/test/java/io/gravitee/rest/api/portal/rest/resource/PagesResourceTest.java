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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.portal.rest.model.Data;
import io.gravitee.rest.api.portal.rest.model.DatasResponse;
import io.gravitee.rest.api.portal.rest.model.Page;

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
        
        PageEntity publishedPage = new PageEntity();
        publishedPage.setPublished(true);
        
        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setPublished(false);
        
        doReturn(Arrays.asList(publishedPage, unpublishedPage)).when(pageService).search(any());
        
        doReturn(new Page()).when(pageMapper).convert(any());
        
    }

    
    @Test
    public void shouldGetPages() {
        doReturn(true).when(groupService).isUserAuthorizedToAccessPortalData(any(), any());

        final Response response = target().request().get();

        assertEquals(OK_200, response.getStatus());

        final DatasResponse pagesResponse = response.readEntity(DatasResponse.class);
        List<Data> pages = pagesResponse.getData();
        assertNotNull(pages);
        assertEquals(1, pages.size());
        
    }
    
    @Test
    public void shouldGetNoPage() {
        doReturn(false).when(groupService).isUserAuthorizedToAccessPortalData(any(), any());
        
        Response response = target().request().get();
        assertEquals(OK_200, response.getStatus());

        DatasResponse pagesResponse = response.readEntity(DatasResponse.class);

        List<Data> pages = pagesResponse.getData();
        assertNotNull(pages);
        assertEquals(0, pages.size());
        
    }
}
