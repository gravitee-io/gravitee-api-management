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
package io.gravitee.rest.api.service.cockpit.command.handler;

import static org.junit.Assert.assertEquals;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.gravitee.repository.management.api.AlertEventRepository;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.api.CrudRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.FindAllRepository;
import io.gravitee.repository.management.api.InstallationRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class DeleteCommandHandlerTest {

    private Set<Class> EXCLUDED_CLASSES = Set.of(
        CrudRepository.class,
        FindAllRepository.class,
        // Repositories that must not be removed
        EventRepository.class,
        EventLatestRepository.class,
        InstallationRepository.class,
        // We use the services for deletion
        OrganizationRepository.class,
        EnvironmentRepository.class,
        AlertTriggerRepository.class,
        AlertEventRepository.class
    );

    /**
     * If a new repository is added, the dev team may forget to add it in the delete commands.
     * The purpose of this test is to raise an alert.
     */
    @Test
    public void should_check_if_we_use_all_repositories_when_delete_an_organization() throws Exception {
        Set<Class> deleteCommandRepositories = Stream.concat(
            Arrays.stream(DeleteOrganizationCommandHandler.class.getDeclaredFields()),
            Arrays.stream(DeleteEnvironmentCommandHandler.class.getDeclaredFields())
        )
            .map(Field::getType)
            .collect(Collectors.toSet());

        Set<Class> repositoriesToAddToDeleteCommand = findAllClassesUsingClassLoader("io.gravitee.repository.management.api")
            .stream()
            .filter(c -> !EXCLUDED_CLASSES.contains(c))
            .filter(c -> !deleteCommandRepositories.contains(c))
            .collect(Collectors.toSet());

        assertEquals(repositoriesToAddToDeleteCommand, Set.of());
    }

    public static Set<Class> findAllClassesUsingClassLoader(String packageName) {
        ClassFileImporter classFileImporter = new ClassFileImporter();
        JavaClasses javaClasses = classFileImporter.importPackages(packageName);

        return javaClasses
            .stream()
            .filter(javaClass -> javaClass.getSimpleName().endsWith("Repository"))
            .map(javaClass -> {
                try {
                    return Class.forName(javaClass.getName());
                } catch (ClassNotFoundException e) {
                    return null;
                }
            })
            .filter(clazz -> clazz != null)
            .collect(Collectors.toSet());
    }
}
