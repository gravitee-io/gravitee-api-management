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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.gravitee.rest.api.service.ViewService;
import io.gravitee.rest.api.service.exceptions.ViewNotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.RatingSummaryEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiEntrypointEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.ApiLinks;
import io.gravitee.rest.api.portal.rest.model.RatingSummary;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.SubscriptionService;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiMapperTest {

    private static final String API = "my-api";
    private static final String API_ENTRYPOINT_1 = "http://foo.bar/foo";
    private static final String API_ID = "my-api-id";
    private static final String API_DESCRIPTION  = "my-api-description";
    private static final String API_LABEL  = "my-api-label";
    private static final String API_NAME  = "my-api-name";
    private static final String API_VERSION  = "my-api-version";
    private static final String API_OWNER_ID  = "my-api-owner-id";
    private static final String API_OWNER_EMAIL  = "my-api-ownber-email";
    private static final String API_OWNER_FIRSTNAME  = "my-api-owner-firstname";
    private static final String API_OWNER_LASTNAME  = "my-api-owner-lastname";
    private static final String API_VIEW  = "my-api-view";
    private static final String API_VIEW_HIDDEN  = "my-api-view-hidden";

    private ApiEntity apiEntity;

    @Mock
    private RatingService ratingService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ViewService viewService;

    @InjectMocks
    private ApiMapper apiMapper;

    @Test
    public void testConvert() {
        //init
        apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setDescription(API_DESCRIPTION);
        apiEntity.setName(API_NAME);
        apiEntity.setLabels(new ArrayList<>(Arrays.asList(API_LABEL)));
        doThrow(ViewNotFoundException.class).when(viewService).findNotHiddenById(API_VIEW_HIDDEN);

        apiEntity.setViews(new HashSet<>(Arrays.asList(API_VIEW, API_VIEW_HIDDEN)));

        apiEntity.setEntrypoints(Arrays.asList(new ApiEntrypointEntity(API_ENTRYPOINT_1), new ApiEntrypointEntity(API+"/foo")));
        
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("meta", API);
        apiEntity.setMetadata(metadata);
        
        apiEntity.setVersion(API_VERSION);
        
        UserEntity ownerEntity = new UserEntity();
        ownerEntity.setId(API_OWNER_ID);
        ownerEntity.setEmail(API_OWNER_EMAIL);
        ownerEntity.setFirstname(API_OWNER_FIRSTNAME);
        ownerEntity.setLastname(API_OWNER_LASTNAME);
        apiEntity.setPrimaryOwner(new PrimaryOwnerEntity(ownerEntity));
        
        RatingSummaryEntity ratingSummaryEntity = new RatingSummaryEntity();
        ratingSummaryEntity.setAverageRate(Double.valueOf(4.2));
        ratingSummaryEntity.setNumberOfRatings(10);
        doReturn(true).when(ratingService).isEnabled();
        doReturn(ratingSummaryEntity).when(ratingService).findSummaryByApi(API_ID);
        
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/foo")));
        apiEntity.setProxy(proxy);
        apiEntity.setLifecycleState(ApiLifecycleState.PUBLISHED);
        apiEntity.setUpdatedAt(new Date());
        
        
        //Test
        Api responseApi = apiMapper.convert(apiEntity);
        assertNotNull(responseApi);
        
        assertNull(responseApi.getPages());
        assertNull(responseApi.getPlans());
        
        assertEquals(API_DESCRIPTION, responseApi.getDescription());
        assertEquals(API_ID, responseApi.getId());
        assertEquals(API_NAME, responseApi.getName());
        assertEquals(API_VERSION, responseApi.getVersion());
        assertFalse(responseApi.getDraft());
        
        List<String> entrypoints = responseApi.getEntrypoints();
        assertNotNull(entrypoints);
        assertEquals(2, entrypoints.size());
        assertEquals(API_ENTRYPOINT_1, entrypoints.get(0));
        assertEquals(API+"/foo", entrypoints.get(1));
        
        List<String> labels = responseApi.getLabels();
        assertNotNull(labels);
        assertTrue(labels.contains(API_LABEL));
        
        User owner = responseApi.getOwner();
        assertNotNull(owner);
        assertEquals(API_OWNER_ID, owner.getId());
        assertEquals(API_OWNER_EMAIL, owner.getEmail());
        assertEquals(API_OWNER_FIRSTNAME+' '+API_OWNER_LASTNAME, owner.getDisplayName());
        
        List<String> views = responseApi.getViews();
        assertNotNull(views);
        assertTrue(views.contains(API_VIEW));
        
        RatingSummary ratingSummary = responseApi.getRatingSummary();
        assertNotNull(ratingSummary);
        assertEquals(Double.valueOf(4.2), ratingSummary.getAverage());
        assertEquals(BigDecimal.valueOf(10), ratingSummary.getCount());
        
    }
 
    @Test
    public void testMinimalConvert() {
        //init
        apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setDescription(API_DESCRIPTION);
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/foo")));
        apiEntity.setProxy(proxy);
        
        doReturn(false).when(ratingService).isEnabled();
        
        apiEntity.setLifecycleState(ApiLifecycleState.CREATED);
        
        //Test
        Api responseApi = apiMapper.convert(apiEntity);
        assertNotNull(responseApi);
        
        assertNull(responseApi.getPages());
        assertNull(responseApi.getPlans());
        
        assertEquals(API_DESCRIPTION, responseApi.getDescription());
        assertEquals(API_ID, responseApi.getId());
        assertNull(responseApi.getName());
        assertNull(responseApi.getVersion());
        assertTrue(responseApi.getDraft());
        
        List<String> entrypoints = responseApi.getEntrypoints();
        assertNull(entrypoints);
        
        List<String> labels = responseApi.getLabels();
        assertNotNull(labels);
        assertEquals(0, labels.size());
        
        assertNull(responseApi.getOwner());
        
        List<String> views = responseApi.getViews();
        assertNotNull(views);
        assertEquals(0, views.size());
        
        RatingSummary ratingSummary = responseApi.getRatingSummary();
        assertNull(ratingSummary);
        
    }
    
    @Test
    public void testApiLinks() {
        String basePath = "/"+API;
        
        ApiLinks links = apiMapper.computeApiLinks(basePath);
        
        assertNotNull(links);
        
        assertEquals(basePath, links.getSelf());
        assertEquals(basePath+"/pages", links.getPages());
        assertEquals(basePath+"/picture", links.getPicture());
        assertEquals(basePath+"/plans", links.getPlans());
        assertEquals(basePath+"/ratings", links.getRatings());
    }
}
