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
package io.gravitee.rest.api.services.dynamicproperties.spring;

import io.reactivex.annotations.NonNull;
import java.util.concurrent.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Alexandre FARIA (lusoalex on github.com)
 */
@Configuration
public class DynamicPropertiesConfiguration {

    @Bean(name = "dynamicPropertiesExecutor")
    public Executor dynamicPropertiesExecutor() {
        int maxPoolSize = Runtime.getRuntime().availableProcessors() * 2;

        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            0,
            maxPoolSize, // maximumPoolSize
            5, // keepAliveTime
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(),
            new ThreadFactory() {
                private int counter = 0;

                @Override
                public Thread newThread(@NonNull Runnable r) {
                    return new Thread(r, "gio.dynamic-properties-" + counter++);
                }
            }
        );

        threadPoolExecutor.allowCoreThreadTimeOut(true);

        return threadPoolExecutor;
    }
}
