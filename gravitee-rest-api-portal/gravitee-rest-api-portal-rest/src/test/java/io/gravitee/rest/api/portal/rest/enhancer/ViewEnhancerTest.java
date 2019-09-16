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
package io.gravitee.rest.api.portal.rest.enhancer;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.model.api.ApiEntity;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ViewEnhancerTest {

    private ViewEnhancer viewEnhancer = new ViewEnhancer();
    
    private Function<ViewEntity, ViewEntity> enhancer;
    
    @Before
    public void init() {
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
        
        Set<ApiEntity> apis = new HashSet<>();
        apis.add(apiA);
        apis.add(apiB);
        apis.add(apiC);
        apis.add(apiD);
        
        enhancer = viewEnhancer.enhance(apis);
    }
    
    @Test
    public void testEnhanceForOneView() {
        ViewEntity v = new ViewEntity();
        v.setId("1");
        
        v = enhancer.apply(v);
        
        assertEquals(2, v.getTotalApis());
    }
    
    @Test
    public void testEnhanceForAllView() {
        ViewEntity v = new ViewEntity();
        v.setId("all");
        
        v = enhancer.apply(v);
        
        assertEquals(4, v.getTotalApis());
    }
}
