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
package io.gravitee.gateway.core.service;

import io.gravitee.common.component.AbstractLifecycleComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class AbstractService<T extends io.gravitee.common.component.LifecycleComponent> extends AbstractLifecycleComponent<T> implements ApplicationContextAware, io.gravitee.common.component.LifecycleComponent<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractService.class);

    private ApplicationContext applicationContext;

    protected ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }

    protected String name() {
        return getClass().getName();
    }

    @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override protected void doStart() throws Exception {
        LOGGER.info("Initializing service {}", name());
    }

    @Override protected void doStop() throws Exception {
        LOGGER.info("Destroying service {}", name());
    }
}
