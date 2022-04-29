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
package io.gravitee.apim.gateway.tests.sdk.annotations;

import io.gravitee.apim.gateway.tests.sdk.GatewayTestingExtension;
import io.vertx.junit5.VertxExtension;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

/**
 * <pre>
 * {@code @GatewayTest} is a type-level annotation which is used to:
 *   - configure a gateway and deploy apis on it thanks to {@link DeployApi}, see {@link GatewayTestingExtension}
 *   - inject {@link io.vertx.ext.web.client.WebClient} to be able to do call to the gateway.
 *
 * Usage of {@link TestInstance}:
 * A new test instance will be created once per test class. It allows to run only once the gateway.
 *
 * Usage of {@link ResourceLock}:
 * Avoid flaky tests by adding a lock identified by 'SYSTEM_PROPERTY'
 * It allows to avoid race condition of writing and then writing the same JVM System Property
 * </pre>
 * @see <a htref="https://junit.org/junit5/docs/snapshot/user-guide/#writing-tests-parallel-execution-synchronization">Junit5 Documentation - Synchronization</a>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({ VertxExtension.class, GatewayTestingExtension.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ_WRITE)
@Order(Integer.MAX_VALUE)
public @interface GatewayTest {
    /**
     * Define where to find the configuration folder of the gateway used by {@code gravitee.home}.
     * This folder should be in src/test/resources and contains a "config" folder with gravitee.yml file inside and a "plugins" folder
     * @return the config folder.
     */
    String configFolder() default "/gravitee-default";
}
