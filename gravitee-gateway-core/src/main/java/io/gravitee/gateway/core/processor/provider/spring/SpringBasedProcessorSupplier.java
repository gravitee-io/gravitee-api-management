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
package io.gravitee.gateway.core.processor.provider.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.function.Supplier;

public class SpringBasedProcessorSupplier<T> implements Supplier<T> {
    private final Logger logger = LoggerFactory.getLogger(SpringBasedProcessorSupplier.class);

    private final ApplicationContext applicationContext;
    private final Class<T> beanClass;

    public SpringBasedProcessorSupplier(ApplicationContext applicationContext, Class<T> beanClass) {
        this.applicationContext = applicationContext;
        this.beanClass = beanClass;
    }

    @Override
    public T get() {
        T processor = null;

        try {
            processor = beanClass.newInstance();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(processor);
        } catch (Exception ex) {
            logger.error("Unexpected while creating a spring based processor", ex);
        }

        return processor;
    }
}
