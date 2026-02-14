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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    private final EventManager eventManager;
    private final ClusterManager clusterManager;

    public ScheduledCommandsRefresherServiceImpl(
        CommandService commandService,
        Node node,
        @Qualifier("commandTaskScheduler") TaskScheduler scheduler,
        @Value("${services.commands.cron:0/5 * * * * *}") String cronTrigger,
        EventManager eventManager,
        @Lazy ClusterManager clusterManager
    ) {
        this.commandService = commandService;
        this.node = node;
        this.scheduler = scheduler;
        this.cronTrigger = cronTrigger;
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

        // Search commands to process
        final List<CommandEntity> commands = searchCommands();
        processCastMode(commands);
        commands.forEach(command -> eventManager.publishEvent(CommandEvent.TO_PROCESS, command));
    }

    private List<CommandEntity> searchCommands() {
        CommandQuery commandQuery = new CommandQuery();
        commandQuery.setTo(MessageRecipient.MANAGEMENT_APIS.name());
        commandQuery.setNotAckBy(node.id());
        commandQuery.setTags(SUPPORTED_COMMAND_TAGS);

        return commandService
            .search(commandQuery)
            .stream()
            .filter(command -> !command.isExpired())
            .toList();
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
     * - {@link io.gravitee.rest.api.model.command.CommandTags.CommandCastMode#UNICAST}: with this cast mode, command should be treated only once. That means it is directly deleted to not be processed by other instances.
     * - {@link io.gravitee.rest.api.model.command.CommandTags.CommandCastMode#MULTICAST}: with this cast mode, command is meant to be processed by every instance targeted, so we only acknowledge it.
     * @param commands to process
     */
    private void processCastMode(List<CommandEntity> commands) {
        commands.forEach(command -> {
            if (command.getTags() != null && command.getTags().size() == 1 && command.getTags().get(0).isUnicast()) {
                commandService.delete(command.getId());
            } else {
                commandService.ack(command.getId());
            }
        });
    }
}
