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
package io.gravitee.gateway.standalone.junit.rules;

import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.junit.stmt.ApiDeployerStatement;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;

/**
 * JUnit 5 extension deploying the API described by the test class {@link ApiDescriptor} annotation on a fresh
 * embedded gateway before each test and tearing it down afterwards. Register it after the WireMock extension so
 * the backend mock is started before the API endpoints are wired.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiDeployer implements BeforeEachCallback, AfterEachCallback {

    private final Object target;
    private ApiDeployerStatement deployment;

    public ApiDeployer(Object target) {
        this.target = target;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (hasAnnotation(context)) {
            deployment = new ApiDeployerStatement(target);
            try {
                deployment.deploy();
            } catch (Exception e) {
                throw e;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (deployment != null) {
            try {
                deployment.undeploy();
            } finally {
                deployment = null;
            }
        }
    }

    public Supplier<ApplicationContext> getGatewayApplicationContext() {
        return deployment != null ? deployment.getApplicationContext() : () -> null;
    }

    private boolean hasAnnotation(ExtensionContext context) {
        return context.getRequiredTestClass().getAnnotation(ApiDescriptor.class) != null;
    }
}
