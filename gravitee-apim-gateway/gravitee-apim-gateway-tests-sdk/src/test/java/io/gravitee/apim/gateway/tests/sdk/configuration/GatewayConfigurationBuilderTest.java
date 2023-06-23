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
package io.gravitee.apim.gateway.tests.sdk.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GatewayConfigurationBuilderTest {

    @Test
    void should_build_config_from_scratch() {
        GatewayConfigurationBuilder cut = new GatewayConfigurationBuilder(new Properties());

        cut.setSystemProperty("foo", "bar");
        cut.setYamlProperty("toto", "tata");
        cut.set("alias", "sys");

        GatewayConfigurationBuilder.GatewayConfiguration conf = cut.build();
        assertThat(conf.systemProperties()).hasSize(2).containsEntry("foo", "bar").containsEntry("alias", "sys");
        assertThat(conf.yamlProperties()).hasSize(1).containsEntry("toto", "tata");
    }

    @Test
    void should_build_config_from_config() {
        var sys = new Properties();
        sys.setProperty("foo", "bar");
        var yaml = new Properties();
        yaml.setProperty("y_foo", "y_bar");
        GatewayConfigurationBuilder cut = new GatewayConfigurationBuilder(sys, yaml);

        GatewayConfigurationBuilder.GatewayConfiguration conf = cut.build();
        assertThat(conf.systemProperties()).hasSize(1).containsEntry("foo", "bar");
        assertThat(conf.yamlProperties()).hasSize(1).containsEntry("y_foo", "y_bar");
    }

    @Test
    void should_leave_conf_untouched_after_update() {
        GatewayConfigurationBuilder cut = new GatewayConfigurationBuilder(new Properties());

        cut.setSystemProperty("foo", "bar");
        cut.setYamlProperty("toto", "tata");

        GatewayConfigurationBuilder.GatewayConfiguration conf = cut.build();
        assertThat(conf.systemProperties()).hasSize(1).containsEntry("foo", "bar");
        assertThat(conf.yamlProperties()).hasSize(1).containsEntry("toto", "tata");

        cut.setSystemProperty("foo", "bar2");
        cut.setSystemProperty("foo2", "bar");
        cut.setYamlProperty("toto2", "tata");
        cut.setYamlProperty("toto", "tata2");

        // none has changed
        assertThat(conf.systemProperties()).hasSize(1).containsEntry("foo", "bar");
        assertThat(conf.yamlProperties()).hasSize(1).containsEntry("toto", "tata");

        conf = cut.build();
        assertThat(conf.systemProperties()).hasSize(2).containsEntry("foo", "bar2").containsEntry("foo2", "bar");
        assertThat(conf.yamlProperties()).hasSize(2).containsEntry("toto2", "tata").containsEntry("toto", "tata2");
    }
}
