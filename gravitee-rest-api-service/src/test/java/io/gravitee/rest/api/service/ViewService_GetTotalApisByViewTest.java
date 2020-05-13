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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.impl.ViewServiceImpl;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ViewService_GetTotalApisByViewTest {

    @InjectMocks
    private ViewServiceImpl viewService = new ViewServiceImpl();

    private static Set<ApiEntity> apis;
    
    @BeforeClass
    public static void init() {
        ApiEntity apiA = new ApiEntity();
        apiA.setId("A");
        apiA.setViews(new HashSet<>(Arrays.asList("1", "")));
        ApiEntity apiB = new ApiEntity();
        apiB.setId("B");
        apiB.setViews(new HashSet<>(Arrays.asList("1", "2")));
        ApiEntity apiC = new ApiEntity();
        apiC.setId("C");
        apiC.setViews(new HashSet<>(Arrays.asList("2", "3")));
        ApiEntity apiD = new ApiEntity();
        apiD.setId("D");
        apiD.setViews(null);
        
        apis = new HashSet<>();
        apis.add(apiA);
        apis.add(apiB);
        apis.add(apiC);
        apis.add(apiD);
    }
    
    @Test
    public void testEnhanceForOneView() {
        ViewEntity v = new ViewEntity();
        v.setKey("1");
        
        long totalApisByView = viewService.getTotalApisByView(apis, v);
       
        assertEquals(2, totalApisByView);
    }
}
