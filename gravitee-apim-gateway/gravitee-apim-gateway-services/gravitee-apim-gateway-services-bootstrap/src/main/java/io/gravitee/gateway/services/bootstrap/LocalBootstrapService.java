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
package io.gravitee.gateway.services.bootstrap;

import com.hazelcast.core.IMap;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.bootstrap.repository.ApiKeyRepositoryWrapper;
import io.gravitee.gateway.services.bootstrap.repository.SubscriptionRepositoryWrapper;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LocalBootstrapService extends AbstractService<LocalBootstrapService> implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(LocalBootstrapService.class);

    @Value("${services.sync.localBootstrap.path:${gravitee.home}/data}")
    protected String bootstrapPath;

    @Value("${services.sync.localBootstrap.enabled:false}")
    private boolean localBootstrap;

    @Value("${services.sync.localBootstrap.backupDelay:60000}")
    private int delay;

    @Autowired
    @Qualifier("apiMap")
    private IMap<String, Api> apis;

    @Autowired
    @Qualifier("apiKeyMap")
    private IMap<String, ApiKey> apiKeys;

    @Autowired
    @Qualifier("dictionaryMap")
    private IMap<String, Dictionary> dictionaries;

    @Autowired
    @Qualifier("subscriptionMap")
    // /!\ Note: subscription map contains Object not Subscription.
    private IMap<String, Object> subscriptions;

    @Autowired
    private ApiManager apiManager;

    @Autowired
    private DictionaryManager dictionaryManager;

    @Autowired
    @Qualifier("syncExecutor")
    private ThreadPoolExecutor executorService;

    @Autowired
    private ClusterManager clusterManager;

    @Autowired
    private ApplicationContext applicationContext;

    LocalBackupMap<String, Api> backupApis;
    LocalBackupMap<String, ApiKey> backupApiKeys;
    LocalBackupMap<String, Dictionary> backupDictionaries;
    LocalBackupMap<String, Object> backupSubscriptions;
    ThreadPoolTaskScheduler taskScheduler;

    @Override
    public void afterPropertiesSet() throws Exception {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) ((ConfigurableApplicationContext) applicationContext.getParent()).getBeanFactory();

        logger.debug("Register API key repository implementation {}", ApiKeyRepositoryWrapper.class.getName());

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ApiKeyRepositoryWrapper.class)
                .setPrimary(true)
                .addConstructorArgValue(applicationContext)
                .addConstructorArgValue(apiKeys);
        beanFactory.registerBeanDefinition(ApiKeyRepositoryWrapper.class.getName(), builder.getBeanDefinition());

        logger.debug("Register subscription repository implementation {}", ApiKeyRepositoryWrapper.class.getName());

        builder = BeanDefinitionBuilder.genericBeanDefinition(SubscriptionRepositoryWrapper.class)
                .setPrimary(true)
                .addConstructorArgValue(applicationContext)
                .addConstructorArgValue(subscriptions);
        beanFactory.registerBeanDefinition(SubscriptionRepositoryWrapper.class.getName(), builder.getBeanDefinition());

        if (localBootstrap) {
            // Start as soon as possible.
            this.start();
        }
    }

    @Override
    protected void doStart() throws Exception {
        if (localBootstrap && this.lifecycleState() != Lifecycle.State.STARTED) {
            super.doStart();

            initializeBackups();

            List<CompletableFuture<?>> futures = new ArrayList<>();
            apis.forEach((s, api) -> futures.add(execute(() -> apiManager.register(api))));
            dictionaries.forEach((s, dictionary) -> futures.add(execute(() -> dictionaryManager.deploy(dictionary))));

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        }
    }

    private void initializeBackups() {
        try {
            backupApis = new LocalBackupMap<>("apis", bootstrapPath, apis);
            backupApiKeys = new LocalBackupMap<>("api-keys", bootstrapPath, apiKeys);
            backupDictionaries = new LocalBackupMap<>("dictionaries", bootstrapPath, dictionaries);
            backupSubscriptions = new LocalBackupMap<>("subscriptions", bootstrapPath, subscriptions);

            // Load from local storage only if current instance is master. Other instances will retrieve all data from the cluster itself.
            CompletableFuture.allOf(execute(() -> backupApis.initialize(clusterManager.isMasterNode())),
                    execute(() -> backupApiKeys.initialize(clusterManager.isMasterNode())),
                    execute(() -> backupDictionaries.initialize(clusterManager.isMasterNode())),
                    execute(() -> backupSubscriptions.initialize(clusterManager.isMasterNode()))).get();

            taskScheduler = new ThreadPoolTaskScheduler();
            taskScheduler.setThreadNamePrefix("gio-bootstrap-");
            taskScheduler.setPoolSize(1);
            taskScheduler.initialize();

            taskScheduler.scheduleAtFixedRate(this::backup, Duration.ofMillis(delay));
        } catch (Exception e) {
            logger.error("An error occurred during initialization for local bootstrap feature.", e);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (localBootstrap) {
            if(taskScheduler != null) {
                taskScheduler.shutdown();
            }

            // Run a last backup before stopping.
            backup();

            // Then cleanup.
            cleanup();

            super.doStop();
        }
    }

    private void backup() {
        try {
            CompletableFuture.allOf(execute(() -> backupApis.backup()),
                    execute(() -> backupApiKeys.backup()),
                    execute(() -> backupSubscriptions.backup()),
                    execute(() -> backupDictionaries.backup())).get();
        } catch (Exception e) {
            logger.error("An error occurred during backup for local bootstrap feature.", e);
        }
    }

    private void cleanup() {
        try {
            CompletableFuture.allOf(execute(() -> backupApis.cleanup()),
                    execute(() -> backupApiKeys.cleanup()),
                    execute(() -> backupSubscriptions.cleanup()),
                    execute(() -> backupDictionaries.cleanup())).get();
        } catch (Exception e) {
            logger.error("An error occurred during cleanup for local bootstrap feature.", e);
        }
    }

    private CompletableFuture<Void> execute(Runnable task) {
        return runAsync(task, executorService);
    }
}
