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
package io.gravitee.rest.api.management.standalone;

import io.gravitee.node.container.spring.SpringBasedContainer;
import io.gravitee.rest.api.management.standalone.spring.StandaloneConfiguration;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ManagementContainer extends SpringBasedContainer {

    @Override
    protected List<Class<?>> annotatedClasses() {
        List<Class<?>> classes = super.annotatedClasses();
        classes.add(StandaloneConfiguration.class);
        return classes;
    }

    @Override
    protected String name() {
        return "Gravitee.io - Management REST API";
    }

    public static void main(String[] args) throws Exception {
        // If you want to run Gravitee standalone from your IDE, please do not forget
        // to specify -Dgravitee.home=/path/to/gravitee/home in order to make it works.
        ManagementContainer container = new ManagementContainer();
        container.start();
    }
}