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
package io.gravitee.gateway.services.apikeyscache.spring;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class ApiKeysCacheConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeysCacheConfiguration.class);

    private static final String API_KEY_CACHE_NAME = "apikey";

    @Bean
    public Cache cache() {
        CacheManager cacheManager = cacheManager();
        Cache apiKeyCache = cacheManager.getCache(API_KEY_CACHE_NAME);
        if (apiKeyCache == null) {
            LOGGER.warn("EHCache cache for apikey not found. Fallback to default EHCache configuration");
            CacheConfiguration cacheConfiguration = new CacheConfiguration(API_KEY_CACHE_NAME, 100);
            cacheManager.addCache(new Cache(cacheConfiguration));
        }

        return cacheManager.getCache(API_KEY_CACHE_NAME);
    }

    @Bean
    public CacheManager cacheManager() {
        String graviteeHome = System.getProperty("gravitee.home");
        String ehCacheConfiguration = graviteeHome + File.separator + "config" + File.separator + "ehcache.xml";

        LOGGER.info("Loading EHCache configuration from {}", ehCacheConfiguration);
        File ehCacheConfigurationFile = new File(ehCacheConfiguration);
        if (ehCacheConfigurationFile.exists()) {
            return CacheManager.newInstance(ehCacheConfigurationFile.getAbsolutePath());
        }

        LOGGER.warn("No configuration file can be found for EHCache");
        throw new IllegalStateException("No configuration file can be found for EHCache");
    }
}
