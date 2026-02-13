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
package io.gravitee.plugin.console.spring;

import io.gravitee.plugin.console.ConsoleExtensionManager;
import io.gravitee.plugin.console.internal.ConsoleExtensionService;
import io.gravitee.plugin.console.internal.DefaultConsoleExtensionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author GraviteeSource Team
 */
@Configuration
public class ConsoleExtensionConfiguration {

    @Bean
    public ConsoleExtensionManager consoleExtensionManager() {
        return DefaultConsoleExtensionManager.getInstance();
    }

    @Bean
    public ConsoleExtensionService consoleExtensionService(ConsoleExtensionManager consoleExtensionManager) {
        return new ConsoleExtensionService(consoleExtensionManager);
    }
}
