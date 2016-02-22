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
package io.gravitee.gateway.services.ratelimit;

import io.gravitee.common.service.AbstractService;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import net.sf.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class AsyncRateLimitService extends AbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncRateLimitService.class);

    @Value("${services.ratelimit.enabled:true}")
    private boolean enabled;

    @Autowired
    private Cache cache;

    private AsyncRateLimitRepository asyncRateLimitRepository;

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();

            LOGGER.info("Overriding Rate-limit repository implementation with rate-limit proxy repository");
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) ((ConfigurableApplicationContext) applicationContext.getParent()).getBeanFactory();

            RateLimitRepository rateLimitRepository = beanFactory.getBean(RateLimitRepository.class);
            LOGGER.debug("Current rate-limit repository implementation is {}", rateLimitRepository.getClass().getName());

            String[] beanNames = beanFactory.getBeanNamesForType(RateLimitRepository.class);
            String oldBeanName = beanNames[0];

            beanFactory.destroySingleton(oldBeanName);
            beanFactory.removeBeanDefinition(oldBeanName);
            LOGGER.debug("Rate-limit repository implementation {} has been removed from context", oldBeanName);

            LOGGER.debug("Register rate-limit repository implementation {}", AsyncRateLimitRepository.class.getName());
            asyncRateLimitRepository = new AsyncRateLimitRepository();
            beanFactory.autowireBean(asyncRateLimitRepository);
            asyncRateLimitRepository.setCachedRateLimitRepository(new RateLimitCacheRepository(cache));
            asyncRateLimitRepository.setDelegateRateLimitRepository(rateLimitRepository);

            beanFactory.registerSingleton(RateLimitRepository.class.getName(), asyncRateLimitRepository);

            asyncRateLimitRepository.start();
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (enabled) {
            super.doStop();

            if (asyncRateLimitRepository != null) {
                asyncRateLimitRepository.stop();
            }
        }
    }

    @Override
    protected String name() {
        return "Rate Limit proxy";
    }
}
