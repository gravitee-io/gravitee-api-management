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

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.portal.rest.model.Metadata;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.Page.TypeEnum;
import io.gravitee.rest.api.portal.rest.model.PageConfiguration;
import io.gravitee.rest.api.portal.rest.model.PageLinks;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageMapperTest {

    private static final String PAGE_ID = "my-page-id";
    private static final String PAGE_CONFIGURATION = "my-page-configuration";
    private static final String PAGE_CONTRIBUTOR = "my-page-contributor";
    private static final String PAGE_NAME = "my-page-name";
    private static final String PAGE_PARENT = "my-page-parent";
    private static final String PAGE_TYPE = "SWAGGER";

    private PageEntity pageEntity;

    @InjectMocks
    private PageMapper pageMapper;
    
    @Test
    public void testConvert() {
        //init
        pageEntity = new PageEntity();
       
        pageEntity.setLastContributor(PAGE_CONTRIBUTOR);
        
        Map<String, String> configuration = new HashMap<>();
        configuration.put("config", PAGE_CONFIGURATION);
        pageEntity.setConfiguration(configuration);
        pageEntity.setId(PAGE_ID);
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("meta", PAGE_ID);
        pageEntity.setMetadata(metadata);
        
        pageEntity.setName(PAGE_NAME);
        pageEntity.setOrder(1);
        pageEntity.setParentId(PAGE_PARENT);
        pageEntity.setType(PAGE_TYPE);
        
        Instant now = Instant.now();
        pageEntity.setLastModificationDate(Date.from(now));
        
        
        //Test
        Page responsePage = pageMapper.convert(pageEntity);
        assertNotNull(responsePage);
        
        List<PageConfiguration> pageConfigurationList = responsePage.getConfiguraton(); 
        assertNotNull(pageConfigurationList);
        assertEquals(1, pageConfigurationList.size());
        PageConfiguration pg = pageConfigurationList.get(0);
        assertNotNull(pg);
        assertEquals("config", pg.getKey());
        assertEquals(PAGE_CONFIGURATION, pg.getValue());
        
        assertEquals(PAGE_ID, responsePage.getId());

        List<Metadata> metadatas = responsePage.getMetadata();
        assertNotNull(metadatas);
        assertEquals(1,metadatas.size());
        Metadata m = metadatas.get(0);
        assertEquals("0",  m.getOrder());
        assertEquals("meta", m.getName());
        assertEquals(PAGE_ID,  m.getValue());
        
        assertEquals(PAGE_NAME, responsePage.getName());
        assertEquals(Integer.valueOf(1), responsePage.getOrder());
        assertEquals(PAGE_PARENT, responsePage.getParent());
        
        assertEquals(TypeEnum.SWAGGER, responsePage.getType());
        
        assertEquals(now.toEpochMilli(), responsePage.getUpdatedAt().toInstant().toEpochMilli());
    }
 
    @Test
    public void testMinimalConvert() {
        //init
        pageEntity = new PageEntity();
        pageEntity.setType(PAGE_TYPE);
        
        Instant now = Instant.now();
        pageEntity.setLastModificationDate(Date.from(now));
        
        //Test
        Page responsePage = pageMapper.convert(pageEntity);
        assertNotNull(responsePage);
        
        List<PageConfiguration> pageConfigurationList = responsePage.getConfiguraton(); 
        assertNull(pageConfigurationList);
        
        List<Metadata> metadatas = responsePage.getMetadata();
        assertNull(metadatas);
        assertEquals(TypeEnum.SWAGGER, responsePage.getType());
        assertEquals(now.toEpochMilli(), responsePage.getUpdatedAt().toInstant().toEpochMilli());
    }

    @Test
    public void testApiLinks() {
        String basePath = "/"+PAGE_ID;
        String parentPath = "/"+PAGE_PARENT;
        
        PageLinks links = pageMapper.computePageLinks(basePath, parentPath);
        
        assertNotNull(links);
        
        assertEquals(basePath, links.getSelf());
        assertEquals(basePath+"/content", links.getContent());
        assertEquals(parentPath, links.getParent());
    }
}
