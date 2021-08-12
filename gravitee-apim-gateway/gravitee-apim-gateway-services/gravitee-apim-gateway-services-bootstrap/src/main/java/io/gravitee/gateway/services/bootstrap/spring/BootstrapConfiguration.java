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
package io.gravitee.gateway.services.bootstrap.spring;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.services.bootstrap.LocalBootstrapService;
import io.gravitee.repository.management.model.ApiKey;
import io.reactivex.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class BootstrapConfiguration {

    public static final int PARALLELISM = Runtime.getRuntime().availableProcessors() * 2;

    @Bean("syncExecutor")
    public ThreadPoolExecutor syncExecutor() {
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(PARALLELISM, PARALLELISM, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private int counter = 0;

                    @Override
                    public Thread newThread(@NonNull Runnable r) {
                        return new Thread(r, "gio.boot-" + counter++);
                    }
                });

        threadPoolExecutor.allowCoreThreadTimeOut(true);

        return threadPoolExecutor;
    }

    @Bean("apiMap")
    public IMap<String, Api> apiMap(HazelcastInstance hzInstance) {
        return hzInstance.getMap("apis");
    }

    @Bean("apiKeyMap")
    public IMap<String, ApiKey> apiKeyMap(HazelcastInstance hzInstance) {
        return hzInstance.getMap("apikeys");
    }

    @Bean("subscriptionMap")
    public IMap<String, Object> subscriptionMap(HazelcastInstance hzInstance) {
        return hzInstance.getMap("subscriptions");
    }

    @Bean("dictionaryMap")
    public IMap<String, Dictionary> dictionaryMap(HazelcastInstance hzInstance) {
        return hzInstance.getMap("dictionaries");
    }
}
