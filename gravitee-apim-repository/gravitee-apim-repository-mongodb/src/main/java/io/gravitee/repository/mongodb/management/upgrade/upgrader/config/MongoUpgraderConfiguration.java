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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.config;

import io.gravitee.node.api.upgrader.Upgrader;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author GraviteeSource Team
 */
@Configuration
public class MongoUpgraderConfiguration implements ApplicationContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(MongoUpgraderConfiguration.class);

    private boolean isUpgrade;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        if (isUpgrade) {
            Optional.ofNullable(context.getParent())
                .map(ConfigurableApplicationContext.class::cast)
                .map(ConfigurableApplicationContext::getBeanFactory)
                .ifPresent(factory -> registerBeans(context, factory));
        }
    }

    @Autowired
    public void setEnvironment(Environment environment) {
        Boolean upgradeMode = environment.getProperty("upgrade.mode", Boolean.class);
        // If upgrade.mode is not set, we consider that we are in upgrade mode to ensure backward compatibility
        this.isUpgrade = upgradeMode == null || upgradeMode;
    }

    private void registerBeans(ApplicationContext context, ConfigurableBeanFactory factory) {
        LOG.debug("Registering upgrader beans in parent application context");
        context.getBeansOfType(Upgrader.class).forEach(factory::registerSingleton);
    }
}
