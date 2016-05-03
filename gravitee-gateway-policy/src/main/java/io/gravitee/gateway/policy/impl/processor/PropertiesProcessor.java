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
package io.gravitee.gateway.policy.impl.processor;

import io.gravitee.gateway.policy.impl.PolicyContextProcessor;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.policy.api.PolicyContext;
import io.gravitee.policy.api.annotations.properties.Property;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class PropertiesProcessor implements PolicyContextProcessor {

    private final Logger LOGGER = LoggerFactory.getLogger(PropertiesProcessor.class);

    @Override
    public void process(PolicyContext policyContext, Reactable reactable) {
        LOGGER.debug("Looking for field annotated with @{}", Property.class.getSimpleName());

        Set<Field> injectableFields = ReflectionUtils.getAllFields(policyContext.getClass(),
                ReflectionUtils.withAnnotation(Property.class));

        for (Field field : injectableFields) {
            LOGGER.debug("Trying to handle property for field {}", field.getName());
            Property prop = field.getAnnotation(Property.class);

            String propertyName = prop.name();
            LOGGER.debug("Looking for property {} from reactable", propertyName);

            String propertyValue = (String) reactable.properties().getOrDefault(propertyName, prop.value());
            if (prop.required() && (propertyValue == null || Property.NULL.equals(propertyValue))) {
                throw new IllegalStateException("Property " + propertyName + " is missing.");
            } else if (! prop.required() && Property.NULL.equals(propertyValue)) {
                propertyValue = null;
            }

            boolean accessible = field.isAccessible();
            try {
                field.setAccessible(true);
                field.set(policyContext, propertyValue);
            } catch (IllegalAccessException iae) {
                LOGGER.error("An error occurs while injecting property value.", iae);
            } finally {
                field.setAccessible(accessible);
            }
        }
    }
}
