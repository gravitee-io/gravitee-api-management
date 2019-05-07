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
package io.gravitee.gateway.handlers.api.processor.alert;

import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Supplier;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertProcessorSupplier implements Supplier<AlertProcessor> {

    @Autowired
    private AlertEngineService alertEngineService;

    @Autowired
    private Node node;

    @Value("${http.port:8082}")
    private String port;

    @Override
    public AlertProcessor get() {
        AlertProcessor processor = new AlertProcessor();
        processor.setPort(port);
        processor.setAlertEngineService(alertEngineService);
        processor.setNode(node);
        return processor;
    }
}
