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
package io.gravitee.rest.api.service.impl;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertEventRepository;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.api.search.AlertEventCriteria;
import io.gravitee.repository.management.model.AlertEvent;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.AlertAnalyticsQuery;
import io.gravitee.rest.api.model.alert.AlertAnalyticsEntity;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.service.AlertAnalyticsService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AlertAnalyticsServiceImpl implements AlertAnalyticsService {

    private final Logger LOGGER = LoggerFactory.getLogger(AlertAnalyticsServiceImpl.class);

    private final AlertTriggerRepository alertTriggerRepository;

    private final AlertEventRepository alertEventRepository;

    public AlertAnalyticsServiceImpl(AlertTriggerRepository alertTriggerRepository, AlertEventRepository alertEventRepository) {
        this.alertTriggerRepository = alertTriggerRepository;
        this.alertEventRepository = alertEventRepository;
    }

    @Override
    public AlertAnalyticsEntity findByReference(AlertReferenceType referenceType, String referenceId, AlertAnalyticsQuery analyticsQuery) {
        try {
            Map<String, AlertTrigger> triggersById = alertTriggerRepository
                .findByReferenceAndReferenceId(referenceType.name(), referenceId)
                .stream()
                .collect(toMap(AlertTrigger::getId, trigger -> trigger));
            Map<AlertTrigger, HashSet<AlertEvent>> eventsByAlert = triggersById
                .values()
                .stream()
                .map(
                    trigger ->
                        alertEventRepository.search(
                            new AlertEventCriteria.Builder()
                                .alert(trigger.getId())
                                .from(analyticsQuery.getFrom())
                                .to(analyticsQuery.getTo())
                                .build(),
                            null
                        )
                )
                .filter(events -> events.getContent().size() > 0)
                .collect(
                    Collectors.toMap(
                        event -> triggersById.get(event.getContent().get(0).getAlert()),
                        event -> new HashSet<>(event.getContent())
                    )
                );

            // by severity
            Map<String, Integer> bySeverity = eventsByAlert
                .entrySet()
                .stream()
                .collect(
                    groupingBy(
                        entry -> entry.getKey().getSeverity(),
                        Collectors.mapping(Map.Entry::getValue, Collectors.summingInt(HashSet::size))
                    )
                );

            // alerts sorted by severity then by decreasing event count
            List<AlertAnalyticsEntity.AlertTriggerAnalytics> alerts = eventsByAlert
                .entrySet()
                .stream()
                .sorted(
                    Map.Entry
                        .<AlertTrigger, HashSet<AlertEvent>>comparingByKey(
                            (a1, a2) -> compareSeverity().compare(a1.getSeverity(), a2.getSeverity())
                        )
                        .thenComparing(Map.Entry.<AlertTrigger, HashSet<AlertEvent>>comparingByValue(comparing(Set::size)).reversed())
                )
                .map(
                    e -> {
                        AlertAnalyticsEntity.AlertTriggerAnalytics alertTriggerAnalytics = new AlertAnalyticsEntity.AlertTriggerAnalytics();
                        alertTriggerAnalytics.setId(e.getKey().getId());
                        alertTriggerAnalytics.setType(e.getKey().getType());
                        alertTriggerAnalytics.setSeverity(e.getKey().getSeverity());
                        alertTriggerAnalytics.setName(e.getKey().getName());
                        alertTriggerAnalytics.setEventsCount(e.getValue().size());
                        return alertTriggerAnalytics;
                    }
                )
                .collect(toList());

            return new AlertAnalyticsEntity(bySeverity, alerts);
        } catch (TechnicalException ex) {
            final String message =
                "An error occurs while trying to list alerts analytics by reference " + referenceType + '/' + referenceId;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    private Comparator<String> compareSeverity() {
        return comparing(s -> Severity.valueOf(s).getWeight());
    }

    private enum Severity {
        CRITICAL(1),
        WARNING(10),
        INFO(100);

        private final Integer weight;

        Severity(Integer weight) {
            this.weight = weight;
        }

        public Integer getWeight() {
            return weight;
        }
    }
}
