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
package io.gravitee.apim.integration.tests.fake;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.GenericExecutionContext;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.policy.cache.CacheResponse;
import io.gravitee.policy.cache.configuration.SerializationMode;
import io.gravitee.policy.cache.mapper.CacheResponseMapper;
import io.gravitee.policy.cache.resource.CacheElement;
import io.gravitee.resource.cache.api.Cache;
import io.gravitee.resource.cache.api.CacheResource;
import io.gravitee.resource.cache.api.Element;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;

public class DummyCacheResource extends CacheResource {

    private static Cache instance;
    private static CacheResponseMapper mapper = new CacheResponseMapper();

    static {
        mapper.setSerializationMode(SerializationMode.TEXT);
    }

    @Override
    public Cache getCache(ExecutionContext executionContext) {
        return getDummyCacheInstance();
    }

    private static Cache getDummyCacheInstance() {
        if (instance == null) {
            instance = new DummyCache();
        }
        return instance;
    }

    public static void clearCache() {
        getDummyCacheInstance().clear();
    }

    public static void checkNumberOfCacheEntries(int expectedSize) {
        try {
            Thread.sleep(2000);
        } catch (Exception e) {}
        Assertions.assertEquals(expectedSize, ((Map) getDummyCacheInstance().getNativeCache()).size());
    }

    public static CacheResponse getFirstEntry() throws JsonProcessingException {
        CacheElement cacheElement = (CacheElement) ((Map) getDummyCacheInstance().getNativeCache()).values().iterator().next();
        return mapper.readValue(cacheElement.value().toString(), CacheResponse.class);
    }

    @Override
    public Cache getCache(GenericExecutionContext genericExecutionContext) {
        return getDummyCacheInstance();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    @Override
    public String name() {
        return "Dummy Resource";
    }

    public String getResourceName() {
        return name();
    }

    @Override
    public Cache getCache(BaseExecutionContext ctx) {
        return null;
    }

    static class DummyCache implements Cache {

        private final Map<Object, Element> map = new HashMap<>();

        @Override
        public String getName() {
            return "cache-name";
        }

        @Override
        public Object getNativeCache() {
            return map;
        }

        @Override
        public Element get(Object o) {
            return map.get(o);
        }

        @Override
        public void put(Element element) {
            map.put(element.key(), element);
        }

        @Override
        public void evict(Object o) {
            map.remove(o);
        }

        @Override
        public void clear() {
            map.clear();
        }
    }
}
