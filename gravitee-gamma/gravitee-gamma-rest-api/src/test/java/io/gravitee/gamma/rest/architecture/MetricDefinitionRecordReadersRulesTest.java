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
package io.gravitee.gamma.rest.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static io.gravitee.gamma.rest.architecture.AbstractGammaArchitectureTest.BASE_PACKAGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.GetObservabilityAnalyticsDefinitionUseCase;
import io.gravitee.gamma.rest.core.observability.catalog.port.repository.AdditionalMetricDefinitionRepository;
import io.gravitee.gamma.rest.core.observability.filter.use_case.GetObservabilityFilterDefinitionsUseCase;
import io.gravitee.gamma.rest.infra.adapter.ObservabilityAnalyticsDataPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.ObservabilityFilterDataPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.ObservabilityLogsDataPortAdapter;
import io.gravitee.repository.management.api.GammaMetricDefinitionRepository;

/**
 * A metric definition record only describes a key to the pickers: {@code exposed: false} keeps the key out of the
 * filter and analytics definitions, never out of the data. So no query, values listing or log search may read records,
 * whether through the core port, the repository underneath, or the two definitions the pickers read.
 */
@AnalyzeClasses(packages = BASE_PACKAGE, importOptions = { ImportOption.DoNotIncludeTests.class })
class MetricDefinitionRecordReadersRulesTest {

    private static final String RANKED_SERIES_FILTER = BASE_PACKAGE + ".infra.adapter.RankedSeriesFilter";

    private static final Class<?>[] PICKER_DEFINITIONS = {
        GetObservabilityFilterDefinitionsUseCase.class,
        GetObservabilityAnalyticsDefinitionUseCase.class,
    };

    /** The core query classes of analytics, filters and logs, and the infra adapters answering their data ports (RankedSeriesFilter is package-private). */
    private static final DescribedPredicate<JavaClass> QUERY_CLASSES = resideInAnyPackage(
        BASE_PACKAGE + ".core.observability.analytics..",
        BASE_PACKAGE + ".core.observability.filter..",
        BASE_PACKAGE + ".core.observability.logs.."
    )
        .and(not(belongToAnyOf(PICKER_DEFINITIONS)))
        .or(
            belongToAnyOf(
                ObservabilityAnalyticsDataPortAdapter.class,
                ObservabilityFilterDataPortAdapter.class,
                ObservabilityLogsDataPortAdapter.class
            ).or(nameMatching(RANKED_SERIES_FILTER.replace(".", "\\.") + "(\\$.*)?"))
        )
        .as("observability queries, values listings and log searches");

    /** RankedSeriesFilter is matched by name: a rename or a move must fail here, not drop it from the rule silently. */
    @ArchTest
    static void ranked_series_filter_should_still_be_where_the_rule_names_it(JavaClasses classes) {
        assertThat(classes.contain(RANKED_SERIES_FILTER)).as(RANKED_SERIES_FILTER).isTrue();
    }

    @ArchTest
    static final ArchRule observability_queries_should_not_read_metric_definition_records = noClasses()
        .that(QUERY_CLASSES)
        .should()
        .transitivelyDependOnClassesThat(
            assignableTo(AdditionalMetricDefinitionRepository.class)
                .or(assignableTo(GammaMetricDefinitionRepository.class))
                .or(belongToAnyOf(PICKER_DEFINITIONS))
                .or(resideInAPackage(BASE_PACKAGE + ".core.observability.catalog.use_case.."))
                .as("metric definition records, the use cases reading them or the definitions built from them")
        )
        .because("hiding a key from the pickers is not a redaction: queries answer the same whether a key is exposed or not");
}
