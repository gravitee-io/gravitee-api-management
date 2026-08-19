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
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The automation ports Gamma modules registered, looked up by module id.
 *
 * <p>Ports are Spring beans of each module's plugin context that the module handler copies into the root
 * context once the plugin is loaded — which happens after the host contexts exist. The lookup is therefore
 * done on every call rather than captured at construction, so a module loaded later, or not at all, is
 * reflected accurately.
 */
public class GammaAutomationPorts {

    private final ObjectProvider<GammaAutomationPort> ports;

    public GammaAutomationPorts(ObjectProvider<GammaAutomationPort> ports) {
        this.ports = ports;
    }

    /**
     * @return the port answering on {@code module}, or empty when no loaded module claims that id
     * @throws IllegalStateException when more than one port claims the module id — a packaging error that
     *     must not be resolved silently
     */
    public Optional<GammaAutomationPort> module(String module) {
        var candidates = ports
            .stream()
            .filter(port -> module.equals(port.module()))
            .toList();
        if (candidates.size() > 1) {
            throw new IllegalStateException("Several Gamma modules register an automation port for module [" + module + "]");
        }
        return candidates.stream().findFirst();
    }

    public List<GammaAutomationPort> all() {
        return ports.stream().toList();
    }
}
