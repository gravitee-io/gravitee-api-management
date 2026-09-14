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

import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
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
 * them per subject and publishes each subject's changes on the event bus, where the modules owning subjects core
 * cannot name (an agent) pick them up. The worker's queue is bounded: past it a run's notifications are dropped and
 * logged rather than held against the evaluation.
 */
@Component
@CustomLog
public class PerformanceTargetTransitionPublisherImpl implements PerformanceTargetTransitionPublisher {

    private final EnvironmentCrudService environmentCrudService;
    private final EventManager eventManager;
    private final Executor worker;

    public PerformanceTargetTransitionPublisherImpl(
        EnvironmentCrudService environmentCrudService,
        EventManager eventManager,
        @Value("${services.performance-targets.notifications.queue:1000}") int queueCapacity
    ) {
        this(environmentCrudService, eventManager, singleWorker(queueCapacity));
    }

    PerformanceTargetTransitionPublisherImpl(EnvironmentCrudService environmentCrudService, EventManager eventManager, Executor worker) {
        this.environmentCrudService = environmentCrudService;
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
            try {
                var organizationId = environmentCrudService.get(subject.environmentId()).getOrganizationId();
                eventManager.publishEvent(
                    PerformanceTargetTransitionEvent.RULES_CHANGED,
                    new PerformanceTargetSubjectTransitions(organizationId, subject.environmentId(), subject.reference(), changes)
                );
            } catch (Exception e) {
                log.error(
                    "Performance target transitions of subject [{}] in environment [{}] could not be published, {} change(s) are lost",
                    subject.reference(),
                    subject.environmentId(),
                    changes.size(),
                    e
                );
            }
        });
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
