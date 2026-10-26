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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.initializer.Initializer;
import io.gravitee.node.api.upgrader.Upgrader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

class MongoUpgraderConfigurationTest {

    private static final String UPGRADER_BEAN = "anUpgrader";
    private static final String INITIALIZER_BEAN = "anInitializer";

    private final GenericApplicationContext parent = new GenericApplicationContext();

    private final AnnotationConfigApplicationContext plugin = new AnnotationConfigApplicationContext();

    @AfterEach
    void closeContexts() {
        plugin.close();
        parent.close();
    }

    @Test
    void should_register_upgraders_and_initializers_in_parent_when_upgrade_mode_is_unset() {
        startPlugin(new MockEnvironment());

        assertThat(parent.getBeansOfType(Upgrader.class)).containsOnlyKeys(UPGRADER_BEAN);
        assertThat(parent.getBeansOfType(Initializer.class)).containsOnlyKeys(INITIALIZER_BEAN);
    }

    @Test
    void should_register_only_upgraders_in_parent_when_upgrade_mode_is_true() {
        startPlugin(new MockEnvironment().withProperty("upgrade.mode", "true"));

        assertThat(parent.getBeansOfType(Upgrader.class)).containsOnlyKeys(UPGRADER_BEAN);
        assertThat(parent.getBeansOfType(Initializer.class)).isEmpty();
    }

    @Test
    void should_register_only_initializers_in_parent_when_upgrade_mode_is_false() {
        startPlugin(new MockEnvironment().withProperty("upgrade.mode", "false"));

        assertThat(parent.getBeansOfType(Upgrader.class)).isEmpty();
        assertThat(parent.getBeansOfType(Initializer.class)).containsOnlyKeys(INITIALIZER_BEAN);
    }

    private void startPlugin(MockEnvironment environment) {
        parent.refresh();
        plugin.setParent(parent);
        plugin.setEnvironment(environment);
        plugin.registerBean(UPGRADER_BEAN, Upgrader.class, () -> new AnUpgrader());
        plugin.registerBean(INITIALIZER_BEAN, Initializer.class, () -> new AnInitializer());
        plugin.register(MongoUpgraderConfiguration.class);
        plugin.refresh();
    }

    private static class AnUpgrader implements Upgrader {

        @Override
        public boolean upgrade() {
            return true;
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }

    private static class AnInitializer implements Initializer {

        @Override
        public boolean initialize() {
            return true;
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }
}
