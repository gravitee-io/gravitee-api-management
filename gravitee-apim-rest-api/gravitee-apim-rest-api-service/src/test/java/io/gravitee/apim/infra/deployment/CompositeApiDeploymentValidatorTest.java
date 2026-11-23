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
package io.gravitee.apim.infra.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import fakes.spring.MutableObjectProvider;
import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.cluster.domain_service.ValidateApiClusterBindingService;
import io.gravitee.apim.core.deployment.service_provider.ApiDeploymentGate;
import io.gravitee.apim.core.deployment.service_provider.ApiDeploymentValidator;
import io.gravitee.apim.infra.spring.CoreServiceSpringConfiguration;
import io.gravitee.apim.infra.spring.InfraServiceSpringConfiguration;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CompositeApiDeploymentValidatorTest {

    private static final Api API = ApiFixtures.aProxyApiV4();

    /** The questions one test's validators were asked, per test, so the class carries no execution-order constraint. */
    private final Asked asked = new Asked();

    /** The two questions the gate carries. Every behaviour of the composite holds for both. */
    enum Question {
        DEPLOYABLE(ApiDeploymentGate::validateDeployable),
        INTERNAL_FLOW_DEPLOYABLE(ApiDeploymentGate::validateInternalFlowDeployable);

        private final BiConsumer<ApiDeploymentGate, Api> ask;

        Question(BiConsumer<ApiDeploymentGate, Api> ask) {
            this.ask = ask;
        }

        void askOf(ApiDeploymentGate gate) {
            ask.accept(gate, API);
        }

        String askedOf(String validator) {
            return validator + "." + name();
        }
    }

    /** What ran, as {@code <validator>.<question>}: the composite forwards each question as itself. */
    static final class Asked {

        private final List<String> entries = new ArrayList<>();

        private void record(String entry) {
            entries.add(entry);
        }

        List<String> entries() {
            return entries;
        }
    }

    /** A validator that answers both questions, recording which one it was asked in the test's own list. */
    private record Recording(String name, Asked asked) implements ApiDeploymentValidator {
        @Override
        public void validateDeployable(Api api) {
            answer(Question.DEPLOYABLE, api);
        }

        @Override
        public void validateInternalFlowDeployable(Api api) {
            answer(Question.INTERNAL_FLOW_DEPLOYABLE, api);
        }

        private void answer(Question question, Api api) {
            assertThat(api).as("API handed to a validator").isSameAs(API);
            asked.record(question.askedOf(name));
        }
    }

    /** A validator that refuses whatever it is asked, as a module whose Guardian is gone does. */
    private record Refusing(ApiNotDeployableException refusal) implements ApiDeploymentValidator {
        @Override
        public void validateDeployable(Api api) {
            throw refusal;
        }

        @Override
        public void validateInternalFlowDeployable(Api api) {
            throw refusal;
        }
    }

    /** Declared in the reverse of their {@link Order}, so running them in declaration order fails the order test. */
    @Configuration
    static class ValidatorsConfig {

        @Bean
        @Order(2)
        ApiDeploymentValidator guardianValidator(Asked asked) {
            return new Recording("guardian", asked);
        }

        @Bean
        @Order(1)
        ApiDeploymentValidator clusterValidator(Asked asked) {
            return new Recording("cluster", asked);
        }
    }

    private AnnotationConfigApplicationContext contextHoldingTheValidators() {
        var context = new AnnotationConfigApplicationContext();
        context.registerBean(Asked.class, () -> asked);
        context.register(CompositeApiDeploymentValidator.class, ValidatorsConfig.class);
        context.refresh();
        return context;
    }

    @Test
    void should_be_the_gate_an_injection_point_of_the_port_receives() {
        try (var context = contextHoldingTheValidators()) {
            assertThat(context.getBean(ApiDeploymentGate.class)).isInstanceOf(CompositeApiDeploymentValidator.class);
        }
    }

    /**
     * The candidates of the context the installation runs, which the validators declared above cannot attest: the
     * cluster binding check reaches every deployment and every start because its service is an
     * {@link ApiDeploymentValidator} the core scan registers, and nothing else would notice it stopping being one.
     * The Guardians validator of a Gamma module is a bean of the module's own context, which
     * {@code GammaGuardiansConfigurationTest} pins against this composite.
     */
    @Test
    void should_hold_the_validators_of_the_rest_api_wiring_among_its_candidates() {
        try (var context = theRestApiWiring()) {
            List<Class<?>> candidates = Stream.of(context.getBeanFactory().getBeanNamesForType(ApiDeploymentValidator.class, true, false))
                .<Class<?>>map(name -> context.getBeanFactory().getType(name, false))
                .toList();

            assertSoftly(softly -> {
                softly
                    .assertThat(candidates)
                    .as("the validator the cluster binding of a definition is checked by")
                    .contains(ValidateApiClusterBindingService.class);
                softly
                    .assertThat(candidates)
                    .as("the gate, which is no validator itself and so never asks itself")
                    .doesNotContain(CompositeApiDeploymentValidator.class);
            });
        }
    }

    /**
     * The scans the rest-api runs, bean definitions only: instantiating the core and the infra would ask for every
     * repository and every plugin of the installation, while what is asserted is which beans the provider reports.
     */
    private static AnnotationConfigApplicationContext theRestApiWiring() {
        var context = new AnnotationConfigApplicationContext();
        context.register(CoreServiceSpringConfiguration.class, InfraServiceSpringConfiguration.class);
        context.addBeanFactoryPostProcessor(factory ->
            Stream.of(factory.getBeanDefinitionNames()).forEach(name -> factory.getBeanDefinition(name).setLazyInit(true))
        );
        context.refresh();
        return context;
    }

    @ParameterizedTest
    @EnumSource(Question.class)
    void should_ask_every_validator_the_context_holds_in_order(Question question) {
        try (var context = contextHoldingTheValidators()) {
            question.askOf(context.getBean(ApiDeploymentGate.class));

            assertThat(asked.entries()).as("validators asked").containsExactly(question.askedOf("cluster"), question.askedOf("guardian"));
        }
    }

    @ParameterizedTest
    @EnumSource(Question.class)
    void should_ask_a_validator_the_context_gained_after_a_first_resolution(Question question) {
        try (var context = contextHoldingTheValidators()) {
            var composite = context.getBean(ApiDeploymentGate.class);
            question.askOf(composite);
            context.getBeanFactory().registerSingleton("lateValidator", new Recording("late", asked));

            question.askOf(composite);

            assertSoftly(softly -> {
                softly
                    .assertThat(asked.entries())
                    .as("validators asked")
                    .containsExactly(
                        question.askedOf("cluster"),
                        question.askedOf("guardian"),
                        question.askedOf("cluster"),
                        question.askedOf("guardian"),
                        question.askedOf("late")
                    );
                softly
                    .assertThat(context.getBean(ApiDeploymentGate.class))
                    .as("the gate an injection point receives, the context having gained a candidate")
                    .isSameAs(composite);
            });
        }
    }

    @ParameterizedTest
    @EnumSource(Question.class)
    void should_accept_an_api_no_validator_refuses(Question question) {
        var composite = new CompositeApiDeploymentValidator(new MutableObjectProvider<>());

        assertThatCode(() -> question.askOf(composite)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(Question.class)
    void should_raise_the_refusal_of_a_validator_and_ask_none_of_the_validators_after_it(Question question) {
        var refusal = new ApiNotDeployableException("Guardian 'guardian-1' is not deployed");
        var validators = new MutableObjectProvider<ApiDeploymentValidator>();
        var composite = new CompositeApiDeploymentValidator(validators);
        validators.add(new Recording("cluster", asked)).add(new Refusing(refusal)).add(new Recording("guardian", asked));

        var throwable = catchThrowable(() -> question.askOf(composite));

        assertSoftly(softly -> {
            softly.assertThat(throwable).as("refusal raised").isSameAs(refusal);
            softly.assertThat(asked.entries()).as("validators asked").containsExactly(question.askedOf("cluster"));
        });
    }
}
