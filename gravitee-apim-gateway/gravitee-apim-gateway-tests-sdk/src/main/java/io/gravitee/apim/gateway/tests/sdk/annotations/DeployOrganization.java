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
 * {@code @DeployOrganization} is a type and method level annotation that is used to deploy an organization on the gateway thanks to an Organization definition json file.
 *
 * The path is relative to the folder {@code src/test/resources}
 *
 * For testing purposes, only one can organization can be deployed at a time.
 *
 * If you use {@code @DeployOrganization} at both class and method level, the method level one will take precedence over the other.
 *
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
public @interface DeployOrganization {
    String value();
}
