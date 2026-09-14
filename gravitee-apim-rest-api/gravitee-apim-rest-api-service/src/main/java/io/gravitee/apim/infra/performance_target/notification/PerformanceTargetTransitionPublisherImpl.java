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
package io.gravitee.apim.infra.performance_target.notification;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetSubjectTransitions;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetTransitionEvent;
import io.gravitee.apim.core.performance_target.service_provider.PerformanceTargetTransitionPublisher;
import io.gravitee.common.event.EventManager;
import jakarta.annotation.PreDestroy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Takes the transitions of an evaluation run off the evaluating thread and, run by run on a single worker, groups
 * them per subject. A subject that is an API of its environment is reported to the API's notification recipients
 * through the {@link PerformanceTargetNotificationDispatcher}; every subject's changes are then published on the
 * event bus, where the modules owning subjects core cannot name (an agent) pick them up. The worker's queue is
 * bounded: past it a run's notifications are dropped and logged rather than held against the evaluation.
 */
@Component
@CustomLog
public class PerformanceTargetTransitionPublisherImpl implements PerformanceTargetTransitionPublisher {

    private final EnvironmentCrudService environmentCrudService;
    private final ApiCrudService apiCrudService;
    private final PerformanceTargetNotificationDispatcher dispatcher;
    private final EventManager eventManager;
    private final Executor worker;

    public PerformanceTargetTransitionPublisherImpl(
        EnvironmentCrudService environmentCrudService,
        ApiCrudService apiCrudService,
        PerformanceTargetNotificationDispatcher dispatcher,
        EventManager eventManager,
        @Value("${services.performance-targets.notifications.queue:1000}") int queueCapacity
    ) {
        this(environmentCrudService, apiCrudService, dispatcher, eventManager, singleWorker(queueCapacity));
    }

    PerformanceTargetTransitionPublisherImpl(
        EnvironmentCrudService environmentCrudService,
        ApiCrudService apiCrudService,
        PerformanceTargetNotificationDispatcher dispatcher,
        EventManager eventManager,
        Executor worker
    ) {
        this.environmentCrudService = environmentCrudService;
        this.apiCrudService = apiCrudService;
        this.dispatcher = dispatcher;
        this.eventManager = eventManager;
        this.worker = worker;
    }

    private static ExecutorService singleWorker(int queueCapacity) {
        return new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(Math.max(1, queueCapacity)),
            runnable -> {
                var thread = new Thread(runnable, "performance-targets-notifications");
                thread.setDaemon(true);
                return thread;
            },
            (runnable, executor) ->
                log.warn(
                    "Performance target notifications queue is full ({} runs waiting), the transitions of one evaluation run are dropped",
                    queueCapacity
                )
        );
    }

    @Override
    public void publish(List<PerformanceTargetRuleTransition> transitions) {
        if (transitions.isEmpty()) {
            return;
        }
        worker.execute(() -> dispatch(transitions));
    }

    /** One run's transitions, grouped per subject in the order they were detected. */
    void dispatch(List<PerformanceTargetRuleTransition> transitions) {
        var bySubject = transitions.stream().collect(Collectors.groupingBy(Subject::of, LinkedHashMap::new, Collectors.toList()));
        bySubject.forEach((subject, changes) -> {
            Environment environment;
            try {
                environment = environmentCrudService.get(subject.environmentId());
            } catch (Exception e) {
                log.error(
                    "Performance target transitions of subject [{}] in environment [{}] could not be published, {} change(s) are lost",
                    subject.reference(),
                    subject.environmentId(),
                    changes.size(),
                    e
                );
                return;
            }
            notifyApi(environment, subject, changes);
            publishToModules(environment, subject, changes);
        });
    }

    /** The reference names an API of the environment: its notification settings say who is told. */
    private void notifyApi(Environment environment, Subject subject, List<PerformanceTargetRuleTransition> changes) {
        try {
            apiCrudService
                .findById(subject.reference())
                .filter(api -> environment.getId().equals(api.getEnvironmentId()))
                .ifPresent(api -> dispatcher.notifyApiSubject(environment, api, changes));
        } catch (Exception e) {
            log.error(
                "Performance target notifications of API [{}] in environment [{}] failed, {} change(s) are not reported to its recipients",
                subject.reference(),
                subject.environmentId(),
                changes.size(),
                e
            );
        }
    }

    private void publishToModules(Environment environment, Subject subject, List<PerformanceTargetRuleTransition> changes) {
        try {
            eventManager.publishEvent(
                PerformanceTargetTransitionEvent.RULES_CHANGED,
                new PerformanceTargetSubjectTransitions(
                    environment.getOrganizationId(),
                    subject.environmentId(),
                    subject.reference(),
                    changes
                )
            );
        } catch (Exception e) {
            log.error(
                "Performance target transitions of subject [{}] in environment [{}] could not be published to the modules",
                subject.reference(),
                subject.environmentId(),
                e
            );
        }
    }

    @PreDestroy
    void shutdown() {
        if (worker instanceof ExecutorService executorService) {
            executorService.shutdownNow();
        }
    }

    private record Subject(String environmentId, String reference) {
        static Subject of(PerformanceTargetRuleTransition transition) {
            return new Subject(transition.environmentId(), transition.reference());
        }
    }
}
