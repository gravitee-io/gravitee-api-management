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
package io.gravitee.rest.api.services.performancetargets;

import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetSchedule;
import io.gravitee.apim.core.performance_target.use_case.EvaluateDuePerformanceTargetsUseCase;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.cluster.ClusterManager;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Evaluates the performance targets that are due against gateway telemetry, once per tick, on the primary node
 * only. Disabling it leaves evaluate-on-demand untouched: that path is a distinct use case behind the REST API.
 */
@CustomLog
public class ScheduledPerformanceTargetEvaluationService extends AbstractService implements Runnable {

    private final TaskScheduler scheduler;
    private final String cronTrigger;
    private final boolean enabled;
    private final PerformanceTargetSchedule schedule;
    private final EvaluateDuePerformanceTargetsUseCase evaluateDuePerformanceTargetsUseCase;
    private final ClusterManager clusterManager;
    private final AtomicLong counter = new AtomicLong(0);

    public ScheduledPerformanceTargetEvaluationService(
        @Qualifier("performanceTargetsTaskScheduler") TaskScheduler scheduler,
        @Value("${services.performance-targets.cron:0 * * * * *}") String cronTrigger,
        @Value("${services.performance-targets.enabled:true}") boolean enabled,
        @Value("${services.performance-targets.backoff.after:3}") int backoffAfter,
        @Value("${services.performance-targets.backoff.cap:60}") long backoffCapMinutes,
        @Value("${services.performance-targets.retention:288}") int retention,
        EvaluateDuePerformanceTargetsUseCase evaluateDuePerformanceTargetsUseCase,
        ClusterManager clusterManager
    ) {
        this.scheduler = scheduler;
        this.cronTrigger = cronTrigger;
        this.enabled = enabled;
        this.schedule = new PerformanceTargetSchedule(backoffAfter, Duration.ofMinutes(backoffCapMinutes), retention);
        this.evaluateDuePerformanceTargetsUseCase = evaluateDuePerformanceTargetsUseCase;
        this.clusterManager = clusterManager;
    }

    @Override
    protected String name() {
        return "Performance Targets Evaluation Service";
    }

    @Override
    protected void doStart() throws Exception {
        if (!enabled) {
            log.warn("Performance targets evaluation service has been disabled, targets are only evaluated on demand");
            return;
        }
        super.doStart();
        try {
            scheduler.schedule(this, new CronTrigger(cronTrigger));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid cron expression [%s] in property 'services.performance-targets.cron'".formatted(cronTrigger),
                e
            );
        }
        log.info(
            "Performance targets evaluation service has been initialized with cron [{}], backing off after {} empty windows up to {}",
            cronTrigger,
            schedule.backoffAfter(),
            schedule.backoffCap()
        );
    }

    /**
     * Only the primary node runs the job: every node holds its own schedule state, so several nodes would evaluate
     * the same targets on the same tick and multiply the analytics queries.
     */
    @Override
    public void run() {
        if (!clusterManager.self().primary()) {
            log.debug("Performance targets evaluation is not on the primary node, skipping execution");
            return;
        }
        var run = counter.incrementAndGet();
        try {
            var output = evaluateDuePerformanceTargetsUseCase.execute(new EvaluateDuePerformanceTargetsUseCase.Input(schedule));
            // Kept at debug when nothing was due: the job ticks every minute on every installation.
            if (output.evaluations().isEmpty()) {
                log.debug("Performance targets evaluation #{}: none of the {} target(s) was due", run, output.targets());
            } else {
                var missed = output
                    .evaluations()
                    .stream()
                    .filter(evaluation -> evaluation.status() == PerformanceTargetEvaluation.Status.BREACH)
                    .count();
                log.info(
                    "Performance targets evaluation #{}: {} of {} target(s) evaluated, {} missed",
                    run,
                    output.evaluations().size(),
                    output.targets(),
                    missed
                );
            }
        } catch (Exception e) {
            log.error("Performance targets evaluation #{} failed, the targets left unevaluated are retried at their next slot", run, e);
        }
    }
}
