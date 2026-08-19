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
package io.gravitee.apim.rest.api.automation.spring;

import io.gravitee.apim.plugin.gamma.api.automation.GammaAutomationPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Automation API to the Gamma modules' automation ports. Imported by the standalone container's
 * root configuration: the ports are copied into the root context by the module handler, and the
 * {@code /automation} web context resolves its beans from that parent.
 */
@Configuration
public class GammaAutomationConfiguration {

    @Bean
    public GammaAutomationPorts gammaAutomationPorts(ObjectProvider<GammaAutomationPort> ports) {
        return new GammaAutomationPorts(ports);
    }
}
