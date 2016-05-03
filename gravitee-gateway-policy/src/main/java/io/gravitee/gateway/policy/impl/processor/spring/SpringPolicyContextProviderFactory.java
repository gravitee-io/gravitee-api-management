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
package io.gravitee.gateway.policy.impl.processor.spring;

import io.gravitee.gateway.policy.PolicyContextProviderFactory;
import io.gravitee.policy.api.PolicyContext;
import io.gravitee.policy.api.PolicyContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class SpringPolicyContextProviderFactory implements PolicyContextProviderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringPolicyContextProviderFactory.class);

    @Override
    public boolean canHandle(PolicyContext policyContext) {
        return policyContext.getClass().getAnnotation(Import.class) != null;
    }

    public PolicyContextProvider create(PolicyContext policyContext) {
        Import importAnnotation = policyContext.getClass().getAnnotation(Import.class);

        List<Class<?>> importClasses = Arrays.asList(importAnnotation.value());

        LOGGER.info("Initializing a Spring context provider from @Import annotation: {}",
                String.join(",",
                        importClasses
                                .stream()
                                .map(Class::getName)
                                .collect(Collectors.toSet())));

        AnnotationConfigApplicationContext policyApplicationContext = new AnnotationConfigApplicationContext();
        policyApplicationContext.setClassLoader(policyContext.getClass().getClassLoader());
        importClasses.forEach(policyApplicationContext::register);

        // TODO: set the parent application context ?
        // pluginContext.setEnvironment(applicationContextParent.getEnvironment());
        // pluginContext.setParent(applicationContextParent);

        policyApplicationContext.refresh();
        return new SpringPolicyContextProvider(policyApplicationContext);
    }
}
