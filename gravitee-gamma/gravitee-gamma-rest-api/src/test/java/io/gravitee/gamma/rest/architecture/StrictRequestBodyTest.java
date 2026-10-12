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

import static io.gravitee.gamma.rest.architecture.AbstractGammaArchitectureTest.BASE_PACKAGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.gravitee.gamma.rest.resources.RejectUnknownProperties;
import io.gravitee.gamma.rest.resources.observability.dashboards.dto.DashboardTimeRangeDto;
import io.gravitee.gamma.rest.resources.observability.dashboards.dto.SaveDashboardRequestDto;
import io.gravitee.gamma.rest.resources.tracing.dto.FilterConditionDto;
import io.gravitee.gamma.rest.resources.tracing.dto.SearchTracesRequestDto;
import jakarta.ws.rs.HttpMethod;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Pins the request types that refuse unknown keys (OBS-127). Every type reachable from a request body of a
 * host resource is strict, except where its root says otherwise:
 * <ul>
 *   <li>the dashboard save root, because GET → edit → PUT clients echo server-owned fields back (OBS-16);</li>
 *   <li>the dashboard time range and the whole trace search body, which OBS-127 left out of its scope.</li>
 * </ul>
 * Tolerance is granted per root, so a strict body reusing a tolerant type makes that type strict.
 */
class StrictRequestBodyTest {

    private static final Map<Class<?>, Set<Class<?>>> TOLERANT_BY_ROOT = Map.of(
        SaveDashboardRequestDto.class,
        Set.of(SaveDashboardRequestDto.class, DashboardTimeRangeDto.class),
        SearchTracesRequestDto.class,
        Set.of(SearchTracesRequestDto.class, SearchTracesRequestDto.TimeRangeDto.class, FilterConditionDto.class)
    );

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(BASE_PACKAGE);

    @Test
    void should_reject_unknown_keys_on_every_request_type_not_tolerant_by_contract() {
        Set<Class<?>> roots = CLASSES.stream()
            .filter(javaClass -> javaClass.getPackageName().startsWith(BASE_PACKAGE + ".resources"))
            .flatMap(javaClass -> Arrays.stream(javaClass.reflect().getDeclaredMethods()))
            .filter(StrictRequestBodyTest::isResourceMethod)
            .flatMap(StrictRequestBodyTest::entityParameters)
            .map(Parameter::getType)
            .collect(Collectors.toSet());

        Set<Class<?>> expectedStrict = new HashSet<>();
        for (Class<?> root : roots) {
            assertThat(isGammaRecord(root)).as("request body %s is a Gamma record the scan can walk", root.getName()).isTrue();
            Set<Class<?>> reachable = new HashSet<>();
            collectGammaTypes(root, reachable);
            reachable.removeAll(TOLERANT_BY_ROOT.getOrDefault(root, Set.of()));
            expectedStrict.addAll(reachable);
        }

        Set<Class<?>> strictTypes = CLASSES.stream()
            .filter(javaClass -> javaClass.isAnnotatedWith(RejectUnknownProperties.class))
            .map(JavaClass::reflect)
            .collect(Collectors.toSet());

        assertThat(roots).as("every tolerant root is still a request body").containsAll(TOLERANT_BY_ROOT.keySet());
        assertThat(strictTypes).containsExactlyInAnyOrderElementsOf(expectedStrict);
    }

    private static boolean isResourceMethod(Method method) {
        return Arrays.stream(method.getAnnotations()).anyMatch(annotation ->
            annotation.annotationType().isAnnotationPresent(HttpMethod.class)
        );
    }

    private static Stream<Parameter> entityParameters(Method method) {
        return Arrays.stream(method.getParameters()).filter(parameter ->
            Arrays.stream(parameter.getAnnotations()).noneMatch(annotation ->
                annotation.annotationType().getPackageName().startsWith("jakarta.ws.rs")
            )
        );
    }

    private static boolean isGammaRecord(Class<?> type) {
        return type.getName().startsWith(BASE_PACKAGE) && type.isRecord();
    }

    private static void collectGammaTypes(Type type, Set<Class<?>> collected) {
        switch (type) {
            case ParameterizedType parameterized -> Arrays.stream(parameterized.getActualTypeArguments()).forEach(argument ->
                collectGammaTypes(argument, collected)
            );
            case WildcardType wildcard -> Arrays.stream(wildcard.getUpperBounds()).forEach(bound -> collectGammaTypes(bound, collected));
            case GenericArrayType array -> collectGammaTypes(array.getGenericComponentType(), collected);
            case Class<?> clazz when clazz.isArray() -> collectGammaTypes(clazz.getComponentType(), collected);
            case Class<?> clazz when clazz.isEnum() -> {}
            case Class<?> clazz when clazz.getName().startsWith(BASE_PACKAGE) && collected.add(clazz) -> {
                assertThat(clazz.isRecord()).as("%s is a record, so its components are its JSON properties", clazz.getName()).isTrue();
                Arrays.stream(clazz.getRecordComponents())
                    .map(RecordComponent::getGenericType)
                    .forEach(component -> collectGammaTypes(component, collected));
            }
            default -> {}
        }
    }
}
