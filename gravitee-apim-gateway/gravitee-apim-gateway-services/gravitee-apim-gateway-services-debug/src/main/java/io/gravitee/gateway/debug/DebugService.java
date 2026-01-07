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
package io.gravitee.gateway.debug;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.util.ListReverser;
import io.gravitee.gateway.debug.vertx.VertxDebugService;
import io.gravitee.gateway.reactive.debug.DebugReactorEventListener;
import java.util.ArrayList;
import java.util.List;
import lombok.CustomLog;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class DebugService extends AbstractService {

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        List<Class<? extends LifecycleComponent>> components = this.components();

        for (Class<? extends LifecycleComponent> component : components) {
            Class<? extends LifecycleComponent> componentClass = component;
            log.info("Starting component: {}", componentClass.getSimpleName());

            try {
                LifecycleComponent lifecyclecomponent = this.applicationContext.getBean(componentClass);
                lifecyclecomponent.start();
            } catch (Exception exception) {
                log.error("An error occurs while starting component {}", componentClass.getSimpleName(), exception);
                throw exception;
            }
        }
    }

    private List<Class<? extends LifecycleComponent>> components() {
        List<Class<? extends LifecycleComponent>> components = new ArrayList<>();
        components.add(VertxDebugService.class);
        components.add(DebugReactorEventListener.class);
        return components;
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        log.info("{} is stopping", this.name());
        ListReverser<Class<? extends LifecycleComponent>> components = new ListReverser(this.components());

        for (Class<? extends LifecycleComponent> component : components) {
            Class componentClass = component;

            try {
                LifecycleComponent lifecyclecomponent = (LifecycleComponent) this.applicationContext.getBean(componentClass);
                if (lifecyclecomponent.lifecycleState() == Lifecycle.State.STARTED) {
                    log.info("Stopping component: {}", componentClass.getSimpleName());
                    lifecyclecomponent.stop();
                }
            } catch (Exception exception) {
                log.error("An error occurs while stopping component {}", componentClass.getSimpleName(), exception);
            }
        }

        log.info("{} stopped", this.name());
    }

    @Override
    public String name() {
        return "Gateway - Debug service";
    }
}
