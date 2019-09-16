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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Date;

import javax.ws.rs.core.UriBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.portal.rest.model.View;
import io.gravitee.rest.api.portal.rest.model.ViewLinks;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ViewMapperTest {

    private static final String VIEW_DESCRIPTION = "my-view-description";
    private static final String VIEW_ID = "my-view-id";
    private static final String VIEW_HIGHLIGHT_API = "my-view-highlight-api";
    private static final String VIEW_NAME = "my-view-name";
    private static final String VIEW_PICTURE = "my-view-picture";
    private static final String VIEW_PICTURE_URL = "my-view-picture-url";

    private static final String VIEW_BASE_URL = "http://foo/bar";

    @InjectMocks
    private ViewMapper viewMapper;
    
    @Test
    public void testConvert() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        ViewEntity viewEntity = new ViewEntity();
        viewEntity.setCreatedAt(nowDate);
        viewEntity.setDefaultView(true);
        viewEntity.setDescription(VIEW_DESCRIPTION);
        viewEntity.setHidden(true);
        viewEntity.setHighlightApi(VIEW_HIGHLIGHT_API);
        viewEntity.setId(VIEW_ID);
        viewEntity.setName(VIEW_NAME);
        viewEntity.setOrder(11);
        viewEntity.setPicture(VIEW_PICTURE);
        viewEntity.setPictureUrl(VIEW_PICTURE_URL);
        viewEntity.setTotalApis(42);
        viewEntity.setUpdatedAt(nowDate);
        
        
        //init
        View view = viewMapper.convert(viewEntity, UriBuilder.fromPath(VIEW_BASE_URL));
        assertTrue(view.getDefaultView());
        assertEquals(VIEW_DESCRIPTION, view.getDescription());
        assertEquals(VIEW_ID, view.getId());
        assertEquals(VIEW_NAME, view.getName());
        assertEquals(11, view.getOrder().intValue());
        assertEquals(42, view.getTotalApis().longValue());
        ViewLinks links = view.getLinks();
        assertNotNull(links);
        assertEquals(VIEW_BASE_URL+"/DEFAULT/apis/"+VIEW_HIGHLIGHT_API, links.getHighlightedApi());
        assertEquals(VIEW_BASE_URL+"/DEFAULT/views/"+VIEW_ID+"/picture", links.getPicture());
        assertEquals(VIEW_BASE_URL+"/DEFAULT/views/"+VIEW_ID, links.getSelf());
    }
}
