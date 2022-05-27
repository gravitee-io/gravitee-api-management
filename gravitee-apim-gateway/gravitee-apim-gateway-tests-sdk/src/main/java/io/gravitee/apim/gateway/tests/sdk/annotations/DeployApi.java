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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 * {@code @DeployApi} is a type and method level annotation that is used to deploy apis on the gateway thanks to an API Definition json file.
 *
 * The path is relative to the folder {@code src/test/resources}
 *
 * For testing purposes, and to avoid state errors, an api can only be deployed once. The tests will be in error if you try to deploy two apis with the same api id.
 *
 * <h3>At class level</h3>
 *
 * The apis are deployed before all the tests, and undeployed after all the tests.
 * You cannot modify a deployed class level api from inside a test to avoid impacting other test cases.
 *
 *
 * <h3>At method level</h3>
 *
 * The apis are deployed before a test method, and undeployed after the test is executed.
 * You can modify a deployed method level api with {@link AbstractGatewayTest#deployedApis}.
 * For example, it can be useful if you want to modify a {@link io.gravitee.definition.model.Endpoint.Status} and check how the api behaves if the backend is down.
 *</pre>
 *
 * This annotation is flagged with {@link Inherited}. Subclasses do not have to reuse it explicitly if they want to use the same one from parent class.
 *
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface DeployApi {
    String[] value();
}
