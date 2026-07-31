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
package io.gravitee.repository.noop;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.Bean;

/**
 * Repository plugins run in their own Spring context. {@code RepositoryPluginHandler} copies a bean into the
 * host context only when its <b>name</b> ends with {@code Repository} (or {@code TransactionManager}), and a
 * bean declared with {@code @Bean} is named after its method. A repository whose method is named otherwise is
 * therefore built, never published, and only shows up as a "No qualifying bean" error the first time a page
 * needs it - which is how the v4 health check repository stayed invisible while analytics were disabled.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NoOpRepositoryBeanNamingTest {

    static Stream<Class<?>> configurations() {
        return Stream.of(
            NoOpAnalyticsRepositoryConfiguration.class,
            NoOpManagementRepositoryConfiguration.class,
            NoOpRateLimitRepositoryConfiguration.class
        );
    }

    @ParameterizedTest
    @MethodSource("configurations")
    void every_repository_bean_is_named_so_the_plugin_handler_publishes_it(Class<?> configuration) {
        List<String> beanNames = Arrays.stream(configuration.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Bean.class))
            .map(Method::getName)
            .toList();

        assertThat(beanNames).isNotEmpty();
        assertThat(beanNames).allSatisfy(name -> assertThat(name).endsWith("Repository"));
    }
}
