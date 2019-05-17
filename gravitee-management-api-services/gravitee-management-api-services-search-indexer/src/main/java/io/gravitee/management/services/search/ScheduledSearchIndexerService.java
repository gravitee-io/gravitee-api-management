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
package io.gravitee.management.services.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.service.AbstractService;
import io.gravitee.management.model.command.CommandEntity;
import io.gravitee.management.model.command.CommandQuery;
import io.gravitee.management.model.command.CommandSearchIndexerEntity;
import io.gravitee.management.model.command.CommandTags;
import io.gravitee.management.service.CommandService;
import io.gravitee.management.service.search.SearchEngineService;
import io.gravitee.repository.management.model.MessageRecipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ScheduledSearchIndexerService extends AbstractService implements Runnable {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ScheduledSearchIndexerService.class);

    @Autowired
    private TaskScheduler scheduler;

    @Value("${services.search_indexer.cron:*/5 * * * * *}")
    private String cronTrigger;

    @Value("${services.search_indexer.enabled:true}")
    private boolean enabled;

    private final AtomicLong counter = new AtomicLong(0);

    @Autowired
    private CommandService commandService;

    @Autowired
    private SearchEngineService searchEngineService;

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    protected String name() {
        return "Search Indexer Service";
    }

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();
            logger.info("Search Indexer service has been initialized with cron [{}]", cronTrigger);
            scheduler.schedule(this, new CronTrigger(cronTrigger));
        } else {
            logger.warn("Search Indexer service has been disabled");
        }
    }

    @Override
    public void run() {
        logger.debug("Search Indexer #{} started at {}", counter.incrementAndGet(), Instant.now());
        CommandQuery query = new CommandQuery();
        query.setTo(MessageRecipient.MANAGEMENT_APIS.name());
        query.setTags(Collections.singletonList(CommandTags.DATA_TO_INDEX));
        List<CommandEntity> messageEntities = commandService.search(query);
        messageEntities.forEach(commandEntity -> {
            commandService.ack(commandEntity.getId());
            try {
                searchEngineService.process(
                        mapper.readValue(commandEntity.getContent(), CommandSearchIndexerEntity.class));
            } catch (IOException e) {
                logger.error("Search Indexer has received a bad message.", e);
            }
        });

        logger.debug("Search Indexer #{} ended at {}", counter.get(), Instant.now());
    }
}
