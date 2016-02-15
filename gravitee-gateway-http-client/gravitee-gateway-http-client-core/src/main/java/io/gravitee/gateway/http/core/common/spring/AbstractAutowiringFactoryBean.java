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
package io.gravitee.gateway.http.core.common.spring;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractAutowiringFactoryBean<T> extends
        AbstractFactoryBean<T> implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(
            final ApplicationContext applicationContext){
        this.applicationContext = applicationContext;
    }

    @Override
    protected final T createInstance() throws Exception {
        final T instance = doCreateInstance();
        if(instance != null){
            applicationContext
                    .getAutowireCapableBeanFactory()
                    .autowireBean(instance);
        }
        return instance;
    }

    /**
     * Create the bean instance.
     *
     * @see #createInstance()
     */
    protected abstract T doCreateInstance() throws Exception;
}