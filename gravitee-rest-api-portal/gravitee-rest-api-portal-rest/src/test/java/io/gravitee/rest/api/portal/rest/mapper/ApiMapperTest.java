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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.gravitee.definition.model.Proxy;
import io.gravitee.rest.api.model.EntrypointEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.RatingSummaryEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.ApiLinks;
import io.gravitee.rest.api.portal.rest.model.RatingSummary;
import io.gravitee.rest.api.service.EntrypointService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.SubscriptionService;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiMapperTest {

    private static final String API = "my-api";

    private ApiEntity apiEntity;

    @Mock
    private RatingService ratingService;
    
    @Mock
    private SubscriptionService subscriptionService;
    
    @Mock
    private EntrypointService entrypointService;
    
    @Mock
    private ParameterService parameterService;
    
    @InjectMocks
    private ApiMapper apiMapper;

    @Test
    public void testConvert() {
        //init
        apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setDescription(API);
        apiEntity.setName(API);
        apiEntity.setLabels(new ArrayList<>(Arrays.asList(API)));
        apiEntity.setTags(new HashSet<>(Arrays.asList(API)));
        apiEntity.setViews(new HashSet<>(Arrays.asList(API)));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("meta", API);
        apiEntity.setMetadata(metadata);
        
        apiEntity.setVersion(API);
        
        UserEntity ownerEntity = new UserEntity();
        ownerEntity.setId(API);
        apiEntity.setPrimaryOwner(new PrimaryOwnerEntity(ownerEntity));
        
        RatingSummaryEntity ratingSummaryEntity = new RatingSummaryEntity();
        ratingSummaryEntity.setAverageRate(Double.valueOf(4.2));
        ratingSummaryEntity.setNumberOfRatings(10);
        doReturn(true).when(ratingService).isEnabled();
        doReturn(ratingSummaryEntity).when(ratingService).findSummaryByApi(API);
        
        Proxy proxy = new Proxy();
        proxy.setContextPath("/foo");
        apiEntity.setProxy(proxy);
        apiEntity.setVisibility(Visibility.PUBLIC);
        apiEntity.setUpdatedAt(new Date());
        
        SubscriptionEntity mockSubscription = new SubscriptionEntity();
        mockSubscription.setApi(API);
        mockSubscription.setStatus(SubscriptionStatus.ACCEPTED);
        Collection<SubscriptionEntity> subscriptions = Arrays.asList(mockSubscription);
        doReturn(subscriptions).when(subscriptionService).search(any());
        
        EntrypointEntity mockEntrypoint = new EntrypointEntity();
        mockEntrypoint.setValue("http://foo.bar");
        doReturn(Arrays.asList(mockEntrypoint)).when(entrypointService).findAll();
        
        doReturn(Arrays.asList(API)).when(parameterService).findAll(Key.PORTAL_ENTRYPOINT);
        
        
        //Test
        Api responseApi = apiMapper.convert(apiEntity);
        assertNotNull(responseApi);
        
        assertNull(responseApi.getPages());
        assertNull(responseApi.getPlans());
        
        assertEquals(API, responseApi.getDescription());
        assertEquals(API, responseApi.getId());
        assertEquals(API, responseApi.getName());
        assertEquals(API, responseApi.getVersion());
        assertTrue(responseApi.getSubscribed());
        
        List<String> entrypoints = responseApi.getEntrypoints();
        assertNotNull(API, entrypoints);
        assertEquals(2, entrypoints.size());
        assertEquals("http://foo.bar/foo", entrypoints.get(0));
        assertEquals(API+"/foo", entrypoints.get(1));
        
        List<String> labels = responseApi.getLabels();
        assertNotNull(API, labels);
        assertTrue(labels.contains(API));
        
        List<String> tags = responseApi.getTags();
        assertNotNull(API, tags);
        assertTrue(tags.contains(API));
        
        List<String> views = responseApi.getViews();
        assertNotNull(API, views);
        assertTrue(views.contains(API));
        
        RatingSummary ratingSummary = responseApi.getRatingSummary();
        assertNotNull(ratingSummary);
        assertEquals(Double.valueOf(4.2), ratingSummary.getAverage());
        assertEquals(BigDecimal.valueOf(10), ratingSummary.getCount());
        
    }
 
    @Test
    public void testMinimalConvert() {
        //init
        apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setDescription(API);
        
        doReturn(false).when(ratingService).isEnabled();
        
        apiEntity.setVisibility(Visibility.PUBLIC);
        
        doReturn(Arrays.asList()).when(subscriptionService).search(any());
        
        EntrypointEntity mockEntrypoint = new EntrypointEntity();
        mockEntrypoint.setValue("http://foo.bar");
        doReturn(Arrays.asList(mockEntrypoint)).when(entrypointService).findAll();
        
        doReturn(Arrays.asList(API)).when(parameterService).findAll(Key.PORTAL_ENTRYPOINT);
        
        
        //Test
        Api responseApi = apiMapper.convert(apiEntity);
        assertNotNull(responseApi);
        
        assertNull(responseApi.getPages());
        assertNull(responseApi.getPlans());
        
        assertEquals(API, responseApi.getDescription());
        assertEquals(API, responseApi.getId());
        assertNull(responseApi.getName());
        assertNull(responseApi.getVersion());
        assertFalse(responseApi.getSubscribed());
        
        List<String> entrypoints = responseApi.getEntrypoints();
        assertNotNull(API, entrypoints);
        assertEquals(2, entrypoints.size());
        assertEquals("http://foo.bar", entrypoints.get(0));
        assertEquals(API, entrypoints.get(1));
        
        List<String> labels = responseApi.getLabels();
        assertNotNull(labels);
        assertEquals(0, labels.size());
        
        List<String> tags = responseApi.getTags();
        assertNotNull(tags);
        assertEquals(0, tags.size());
        
        List<String> views = responseApi.getViews();
        assertNotNull(views);
        assertEquals(0, views.size());
        
        RatingSummary ratingSummary = responseApi.getRatingSummary();
        assertNull(ratingSummary);
        
    }
    
    @Test
    public void testNoSubscription() {
        apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setDescription(API);
        apiEntity.setVisibility(Visibility.PUBLIC);
        
        doReturn(false).when(ratingService).isEnabled();
        
        //Empty list of subscription
        Collection<SubscriptionEntity> noSubscriptions = Arrays.asList();
        doReturn(noSubscriptions).when(subscriptionService).search(any());
        
        Api responseApi = apiMapper.convert(apiEntity);
        assertNotNull(responseApi);
        assertFalse(responseApi.getSubscribed());
        
        //Null list of subscription
        doReturn(null).when(subscriptionService).search(any());
        responseApi = apiMapper.convert(apiEntity);
        assertNotNull(responseApi);
        assertFalse(responseApi.getSubscribed());
    }
    
    @Test
    public void testNoDefaultEntrypointInParam() {
        apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setDescription(API);
        apiEntity.setVisibility(Visibility.PUBLIC);
        
        doReturn(false).when(ratingService).isEnabled();
        doReturn(Arrays.asList()).when(entrypointService).findAll();
        
        //Empty list of subscription
        doReturn(Arrays.asList()).when(parameterService).findAll(Key.PORTAL_ENTRYPOINT);
        
        Api responseApi = apiMapper.convert(apiEntity);
        assertNotNull(responseApi);
        assertEquals(0, responseApi.getEntrypoints().size());
        
        //Null list of subscription
        doReturn(null).when(parameterService).findAll(Key.PORTAL_ENTRYPOINT);
        responseApi = apiMapper.convert(apiEntity);
        assertNotNull(responseApi);
        assertEquals(0, responseApi.getEntrypoints().size());
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
