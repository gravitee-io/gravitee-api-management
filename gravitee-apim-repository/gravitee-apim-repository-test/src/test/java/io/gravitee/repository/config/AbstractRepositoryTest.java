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
package io.gravitee.repository.config;

import static java.lang.Class.forName;
import static org.springframework.util.StringUtils.capitalize;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import jakarta.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * @author GraviteeSource Team
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, initializers = { PropertySourceRepositoryInitializer.class })
@ActiveProfiles("test")
public abstract class AbstractRepositoryTest {

    private static final String JSON_EXTENSION = "json";
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Inject
    private TestRepositoryInitializer testRepositoryInitializer;

    protected abstract String getTestCasesPath();

    protected abstract String getModelPackage();

    protected abstract void createModel(Object object) throws TechnicalException;

    @Before
    public void setUp() throws Exception {
        testRepositoryInitializer.setUp();

        if (getTestCasesPath() != null) {
            URL testCaseResource = AbstractRepositoryTest.class.getResource(getTestCasesPath());

            if (testCaseResource == null) {
                throw new IllegalStateException("No resource defined in " + getTestCasesPath());
            }

            final File directory = new File(testCaseResource.toURI());

            final File[] files = directory.listFiles(pathname ->
                pathname.isFile() && JSON_EXTENSION.equalsIgnoreCase(FilenameUtils.getExtension(pathname.toString()))
            );

            for (final File file : getSortedFilesList(files)) {
                createModels(file);
            }
        }
    }

    @After
    public void tearDown() {
        testRepositoryInitializer.tearDown();
    }

    protected Class<?> getClassFromFileName(final String baseName) {
        final String className = capitalize(baseName.substring(0, baseName.length() - 1));
        try {
            return forName(getModelPackage() + className);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("The entity cannot be found for " + className, e);
        }
    }

    protected <T> List<T> mapToModel(final File file, final Class<T> clazz) throws Exception {
        return MAPPER.readValue(file, MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    private List<File> getSortedFilesList(File[] files) {
        return Stream.of(files).sorted((o1, o2) -> o2.getName().compareTo(o1.getName())).collect(Collectors.toList());
    }

    private void createModels(File file) throws Exception {
        final Class<?> c = getClassFromFileName(FilenameUtils.getBaseName(file.getName()));

        for (final Object object : mapToModel(file, c)) {
            createModel(object);
        }
    }

    @Configuration
    @ComponentScan(
        value = "io.gravitee.repository",
        includeFilters = @ComponentScan.Filter(pattern = ".*TestRepository.*", type = FilterType.REGEX),
        useDefaultFilters = false
    )
    static class ContextConfiguration {}
}
