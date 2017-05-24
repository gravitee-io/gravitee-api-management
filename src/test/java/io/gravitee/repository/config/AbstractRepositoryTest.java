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
package io.gravitee.repository.config;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.*;
import io.gravitee.repository.management.model.*;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Class.forName;
import static java.util.Arrays.asList;
import static org.springframework.util.StringUtils.capitalize;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        loader = AnnotationConfigContextLoader.class,
        initializers = {PropertySourceRepositoryInitializer.class})
@ActiveProfiles("test")
public abstract class AbstractRepositoryTest {

    private static final String MODEL_PACKAGE = "io.gravitee.repository.management.model.";
    private static final String JSON_EXTENSION = "json";

    @Inject
    private TestRepositoryInitializer testRepositoryInitializer;

    @Inject
    protected ApplicationRepository applicationRepository;
    @Inject
    protected ApiRepository apiRepository;
    @Inject
    protected UserRepository userRepository;
    @Inject
    protected ApiKeyRepository apiKeyRepository;
    @Inject
    protected EventRepository eventRepository;
    @Inject
    protected ViewRepository viewRepository;
    @Inject
    protected GroupRepository groupRepository;
	@Inject
    protected MembershipRepository membershipRepository;
    @Inject
    protected PlanRepository planRepository;
    @Inject
    protected TagRepository tagRepository;
    @Inject
    protected PageRepository pageRepository;
    @Inject
    protected SubscriptionRepository subscriptionRepository;
    @Inject
    protected TenantRepository tenantRepository;
    @Inject
    protected MetadataRepository metadataRepository;
    @Inject
    protected RoleRepository roleRepository;

    private ObjectMapper mapper = new ObjectMapper();

    protected abstract String getTestCasesPath();

    @Before
    public void setUp() throws Exception {
        testRepositoryInitializer.setUp();

        final File file = new File(AbstractRepositoryTest.class.getResource(getTestCasesPath()).toURI());

        final File[] collectionsDumps = file.listFiles(
                pathname -> pathname.isFile()
                        && JSON_EXTENSION.equalsIgnoreCase(FilenameUtils.getExtension(pathname.toString())));

        for (final File collectionsDump : asList(collectionsDumps).stream().sorted((o1, o2) -> o2.getName().compareTo(o1.getName())).collect(Collectors.toList())) {
            final Class c = getClassFromFileName(FilenameUtils.getBaseName(collectionsDump.getName()));
            for (final Object object : mapToModel(collectionsDump, c)) {
                createModel(object);
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        testRepositoryInitializer.tearDown();
    }

    private void createModel(Object object) throws TechnicalException {
        if (object instanceof Application) {
            applicationRepository.create((Application) object);
        }
        else if (object instanceof Api) {
            apiRepository.create((Api) object);
        }
        else if (object instanceof User) {
            userRepository.create((User) object);
        }
        else if (object instanceof Event) {
            eventRepository.create((Event) object);
        }
        else if (object instanceof ApiKey) {
            final ApiKey apiKey = (ApiKey) object;
            apiKeyRepository.create(apiKey);
        }
        else if (object instanceof View) {
            viewRepository.create((View) object);
        }
        else if (object instanceof Group) {
            groupRepository.create((Group)object);
        }
        else if (object instanceof Membership) {
            membershipRepository.create((Membership) object);
        }
        else if (object instanceof Plan) {
            planRepository.create((Plan) object);
        }
        else if (object instanceof Tag) {
            tagRepository.create((Tag) object);
        }
        else if (object instanceof Page) {
            pageRepository.create((Page) object);
        }
        else if (object instanceof Subscription) {
            subscriptionRepository.create((Subscription) object);
        }
        else if (object instanceof Tenant) {
            tenantRepository.create((Tenant) object);
        }
        else if (object instanceof Metadata) {
            metadataRepository.create((Metadata) object);
        }
        else if (object instanceof Role) {
            roleRepository.create((Role) object);
        }
    }

    private Class getClassFromFileName(final String baseName) {
        final String className = capitalize(baseName.substring(0, baseName.length() - 1));
        try {
            return forName(MODEL_PACKAGE + className);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("The entity can not be found for " + className, e);
        }
    }

    protected <T> List<T> mapToModel(final File file, final Class<T> clazz) throws Exception {
        return mapper.readValue(file, TypeFactory.collectionType(List.class, clazz));
    }

    @Configuration
    @ComponentScan(
            value = "io.gravitee.repository",
            includeFilters = @Filter(pattern = ".*Test.*", type = FilterType.REGEX),
            useDefaultFilters = false
    )
    static class ContextConfiguration {
    }
}
