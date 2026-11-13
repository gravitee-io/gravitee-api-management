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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.apim.core.utils.CollectionUtils.isNotEmpty;
import static io.gravitee.apim.core.utils.CollectionUtils.stream;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.management.model.MessageRecipient;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandQuery;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.service.CommandService;
import io.gravitee.rest.api.service.ScheduledCommandService;
import io.gravitee.rest.api.service.event.CommandEvent;
import java.util.Arrays;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class ScheduledCommandsRefresherServiceImpl
    extends AbstractService<ScheduledCommandsRefresherServiceImpl>
    implements ScheduledCommandService<ScheduledCommandsRefresherServiceImpl>, Runnable {

    // We exclude the DATA_TO_INDEX tag because it is processed by another service
    public static final List<CommandTags> SUPPORTED_COMMAND_TAGS = Arrays.stream(CommandTags.values())
        .filter(commandTags -> commandTags != CommandTags.DATA_TO_INDEX)
        .toList();

    private final CommandService commandService;

    private final Node node;

    private final TaskScheduler scheduler;
    private final String cronTrigger;
    // MAX_BULK_ITEMS keeps the JDBC id list of a page under SQL Server's 2100-parameter limit; MAX_BULKS bounds a run.
    static final int MAX_BULK_ITEMS = 1_000;
    static final int MAX_BULKS = 100;

    private final int bulkItems;
    private final int maxBulks;
    private final EventManager eventManager;
    private final ClusterManager clusterManager;

    public ScheduledCommandsRefresherServiceImpl(
        CommandService commandService,
        Node node,
        @Qualifier("commandTaskScheduler") TaskScheduler scheduler,
        @Value("${services.commands.cron:0/5 * * * * *}") String cronTrigger,
        @Value("${services.commands.bulk_items:100}") int bulkItems,
        @Value("${services.commands.max_bulks:10}") int maxBulks,
        EventManager eventManager,
        @Lazy ClusterManager clusterManager
    ) {
        if (bulkItems < 1 || bulkItems > MAX_BULK_ITEMS || maxBulks < 1 || maxBulks > MAX_BULKS) {
            throw new IllegalArgumentException(
                "services.commands.bulk_items must be between 1 and " +
                    MAX_BULK_ITEMS +
                    " and services.commands.max_bulks between 1 and " +
                    MAX_BULKS +
                    ", got " +
                    bulkItems +
                    " and " +
                    maxBulks
            );
        }
        this.commandService = commandService;
        this.node = node;
        this.scheduler = scheduler;
        this.cronTrigger = cronTrigger;
        this.bulkItems = bulkItems;
        this.maxBulks = maxBulks;
        this.eventManager = eventManager;
        this.clusterManager = clusterManager;
    }

    @Override
    protected String name() {
        return "Scheduled Command Service";
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        scheduler.schedule(this, new CronTrigger(cronTrigger));
    }

    @Override
    public void run() {
        // Only the primary node should clean expired commands to avoid concurrent deletions
        if (clusterManager.self().primary()) {
            searchExpiredCommandsAndClean();
        }

        processPendingCommands();
    }

    /**
     * Reads the pending commands bulk by bulk, oldest first, each bulk starting strictly after the last command of the
     * previous one, so that commands still pending after being read (expired, or whose claim failed) do not hold back
     * newer ones. Each run starts again from the oldest command, so the ones left pending are retried on the next run.
     */
    private void processPendingCommands() {
        CommandQuery commandQuery = new CommandQuery();
        commandQuery.setTo(MessageRecipient.MANAGEMENT_APIS.name());
        commandQuery.setNotAckBy(node.id());
        commandQuery.setTags(SUPPORTED_COMMAND_TAGS);

        CommandEntity last = null;
        for (int bulk = 0; bulk < maxBulks; bulk++) {
            List<CommandEntity> commands = commandService.search(commandQuery, last, bulkItems);
            commands
                .stream()
                .filter(command -> !command.isExpired())
                .forEach(this::process);
            if (commands.size() < bulkItems) {
                return;
            }
            last = commands.get(commands.size() - 1);
            if (last.getCreatedAt() == null) {
                // The next bulk starts after this command's creation date. Without one, the commands after it are read
                // on the next runs, once this one is claimed and no longer read.
                log.debug("Command [{}] has no creation date, the commands after it are processed on the next runs", last.getId());
                return;
            }
        }
        log.debug("Read the maximum of {} bulks of commands, the remaining ones are processed on the next run", maxBulks);
    }

    private void process(CommandEntity command) {
        try {
            if (!claim(command)) {
                return;
            }
        } catch (RuntimeException e) {
            log.error("Failed to claim command [{}], it is not processed by this run", command.getId(), e);
            return;
        }
        try {
            eventManager.publishEvent(CommandEvent.TO_PROCESS, command);
        } catch (RuntimeException e) {
            log.error(
                "Failed to process command [{}] with tags {}, it was already claimed and is not retried",
                command.getId(),
                command.getTags(),
                e
            );
        }
    }

    private void searchExpiredCommandsAndClean() {
        var before = TimeProvider.instantNow();
        int deletedCount = commandService.deleteByExpiredAtBefore(before);
        if (deletedCount > 0) {
            log.debug("Deleted {} expired commands before {}", deletedCount, before);
        }
    }

    /**
     * A command can be applied on two different cast modes.
     * - {@link io.gravitee.rest.api.model.command.CommandTags.CommandCastMode#UNICAST}: with this cast mode, command should be treated only once. That means it is directly deleted to not be processed by other instances, and only the instance whose delete removed it processes it.
     * - {@link io.gravitee.rest.api.model.command.CommandTags.CommandCastMode#MULTICAST}: with this cast mode, command is meant to be processed by every instance targeted, so we only acknowledge it, and process it only once acknowledged.
     * @param command to process
     * @return whether this instance has to process the command
     */
    private boolean claim(CommandEntity command) {
        if (command.getTags() != null && command.getTags().size() == 1 && command.getTags().get(0).isUnicast()) {
            return commandService.deleteIfExists(command.getId());
        }
        return commandService.ackIfExists(command.getId());
    }
}
