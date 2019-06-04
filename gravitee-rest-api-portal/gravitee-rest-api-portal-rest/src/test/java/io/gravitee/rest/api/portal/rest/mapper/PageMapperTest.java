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
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.portal.rest.model.Metadata;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.Page.TypeEnum;
import io.gravitee.rest.api.portal.rest.model.PageConfiguration;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageMapperTest {

    private static final String PAGE = "my-page";

    private PageEntity pageEntity;

    @InjectMocks
    private PageMapper pageMapper;
    
    @Test
    public void testConvert() {
        //init
        pageEntity = new PageEntity();
       
        pageEntity.setLastContributor(PAGE);
        
        Map<String, String> configuration = new HashMap<>();
        configuration.put("config", PAGE);
        pageEntity.setConfiguration(configuration);
        pageEntity.setContent(PAGE);
        pageEntity.setId(PAGE);
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("meta", PAGE);
        pageEntity.setMetadata(metadata);
        
        pageEntity.setName(PAGE);
        pageEntity.setOrder(1);
        pageEntity.setParentId(PAGE);
        pageEntity.setType("SWAGGER");
        
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
        assertEquals(PAGE, pg.getValue());
        
        assertEquals(PAGE, responsePage.getContent());
        assertEquals(PAGE, responsePage.getId());

        List<Metadata> metadatas = responsePage.getMetadata();
        assertNotNull(metadatas);
        assertEquals(1,metadatas.size());
        Metadata m = metadatas.get(0);
        assertEquals("0",  m.getOrder());
        assertEquals("meta", m.getName());
        assertEquals(PAGE,  m.getValue());
        
        assertEquals(PAGE, responsePage.getName());
        assertEquals(Integer.valueOf(1), responsePage.getOrder());
        assertEquals(PAGE, responsePage.getParent());
        
        assertEquals(TypeEnum.SWAGGER, responsePage.getType());
        
        assertEquals(now.toEpochMilli(), responsePage.getUpdatedAt().toInstant().toEpochMilli());
        
        
    }
 
    @Test
    public void testMinimalConvert() {
        //init
        pageEntity = new PageEntity();
        
        pageEntity.setType("SWAGGER");
        
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
}
