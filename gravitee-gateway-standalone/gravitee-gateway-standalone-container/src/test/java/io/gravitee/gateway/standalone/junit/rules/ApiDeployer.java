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
package io.gravitee.gateway.standalone.junit.rules;

import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.junit.stmt.ApiDeployerStatement;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiDeployer implements TestRule {

    private final Object target;

    public ApiDeployer(Object target) {
        this.target = target;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        Statement result;

        if(hasAnnotation(description)) {
            result = new ApiDeployerStatement(base, target);
        } else {
            result = base;
        }

        return result;
    }

    private boolean hasAnnotation(Description description) {
        return description.getTestClass().getAnnotation(ApiDescriptor.class) != null;
    }
}
