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
package io.gravitee.apim.gateway.tests.sdk.annotations;

import io.gravitee.gateway.api.genericevent.GenericEvent;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 * {@code @DeployGenericEvents} is a type and method level annotation that is used to deploy one or many generic events on the gateway.
 * </pre>
 *
 * Each resource is a JSON object with the {@code kind}, the {@code id} and the {@code payload} of one event (a JSON object, or a JSON string holding the payload as is), and optionally its {@code environmentId},
 * {@code organizationId} (both default to "DEFAULT") and {@code expiresAt}. The events of one annotation are delivered to the subscribers of their kind,
 * which are then committed once, as the gateway sync does. The events are removed when the test (or the test class) ends.
 *
 * This annotation is flagged with {@link Inherited}. Subclasses do not have to reuse it explicitly if they want to use the same one from parent class.
 *
 * @see GenericEvent as the model to deploy
 * @author GraviteeSource Team
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface DeployGenericEvents {
    String[] value();
}
