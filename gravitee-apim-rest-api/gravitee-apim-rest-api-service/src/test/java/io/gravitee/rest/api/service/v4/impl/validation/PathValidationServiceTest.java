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
package io.gravitee.rest.api.service.v4.impl.validation;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.exception.InvalidHostException;
import io.gravitee.rest.api.service.v4.exception.PathAlreadyExistsException;
import io.gravitee.rest.api.service.v4.validation.PathValidationService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PathValidationServiceTest {

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private EnvironmentService environmentService;

    private PathValidationService pathValidationService;

    @Before
    public void init() {
        GraviteeContext.setCurrentEnvironment("DEFAULT");
        when(environmentService.findById(any())).thenReturn(mock(EnvironmentEntity.class));
        pathValidationService = new PathValidationServiceImpl(apiRepository, objectMapper, environmentService);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldSucceed_create_noApi() {
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
        )
            .thenReturn(Stream.empty());

        List<Path> paths = pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(new Path("/context"))
        );

        assertEquals(1, paths.size());
        assertEquals(new Path(null, "/context/", false), paths.get(0));
    }

    @Test
    public void shouldSucceed_create_noMatchingPath() {
        Api api1 = createMock("mock1", "/existing");
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
        )
            .thenReturn(Stream.of(api1));

        List<Path> paths = pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(new Path("/context"))
        );

        assertEquals(1, paths.size());
        assertEquals(new Path(null, "/context/", false), paths.get(0));
    }

    @Test
    public void shouldSucceed_create_withOverrideAccess() {
        Api api1 = createMock("mock1", "/existing");
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
        )
            .thenReturn(Stream.of(api1));

        List<Path> paths = pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(new Path("host", "path", true))
        );

        assertEquals(1, paths.size());
        assertEquals(new Path("host", "/path/", true), paths.get(0));
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void shouldFail_create_existingPath() {
        Api api1 = createMock("mock1", "/context");
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
        )
            .thenReturn(Stream.of(api1));

        pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(new Path("/context"))
        );
    }

    @Test
    public void shouldFail_create_sameBasePath() {
        Api api1 = createMock("mock1", "/context2");
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
        )
            .thenReturn(Stream.of(api1));

        List<Path> paths = pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(new Path("/context"))
        );

        assertEquals(1, paths.size());
        assertEquals(new Path(null, "/context/", false), paths.get(0));
    }

    @Test
    public void shouldFail_create_sameBasePath2() {
        Api api1 = createMock("mock1", "/context");

        List<Path> paths = pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(new Path("/context2"))
        );

        assertEquals(1, paths.size());
        assertEquals(new Path(null, "/context2/", false), paths.get(0));
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void shouldFail_create_existingPath_trailingSlash() {
        Api api1 = createMock("mock1", "/context");
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
        )
            .thenReturn(Stream.of(api1));

        pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(new Path("/context/"))
        );
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void shouldFail_create_existingPath_trailingSlash2() {
        Api api1 = createMock("mock1", "/context/");
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
        )
            .thenReturn(Stream.of(api1));

        pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(new Path("/context"))
        );
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void shouldFail_create_existingSubPath() {
        Api api1 = createMock("mock1", "/context/subpath");
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
        )
            .thenReturn(Stream.of(api1));

        pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(new Path("/context"))
        );
    }

    @Test
    public void shouldSucceed_create_virtualHostWithSamePath() {
        Api api1 = createMock("mock1", "/context", "api.gravitee.io");
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
        )
            .thenReturn(Stream.of(api1));

        List<Path> paths = pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(new Path("/context"))
        );

        assertEquals(1, paths.size());
        assertEquals(new Path(null, "/context/", false), paths.get(0));
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void shouldSucceed_create_sameVirtualHostAndSamePath() {
        Api api1 = createMock("mock1", "/context", "api.gravitee.io");
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
        )
            .thenReturn(Stream.of(api1));

        List<Path> paths = pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(new Path("api.gravitee.io", "/context"))
        );

        assertEquals(1, paths.size());
        assertEquals(new Path("api.gravitee.io", "/context/", false), paths.get(0));
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void shouldSucceed_create_sameVirtualHostAndSameSubPath() {
        Api api1 = createMock("mock1", "/context", "api.gravitee.io");
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
        )
            .thenReturn(Stream.of(api1));

        pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(new Path("api.gravitee.io", "/context/subpath"))
        );
    }

    @Test
    public void validate_hostEqualsToDomainConstraint() {
        Path path = getValidPath();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setDomainRestrictions(Collections.singletonList(path.getHost()));
        when(environmentService.findById(any())).thenReturn(environmentEntity);

        List<Path> paths = pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(path)
        );

        assertEquals(1, paths.size());
        assertEquals(new Path("valid.host.gravitee.io", "/validPath/", false), paths.get(0));
    }

    @Test
    public void validate_hostSubDomainOfDomainConstraint() {
        Path path = getValidPath();
        String domainConstraint = path.getHost();
        path.setHost("level2.level1." + domainConstraint);

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setDomainRestrictions(Collections.singletonList(domainConstraint));
        when(environmentService.findById(any())).thenReturn(environmentEntity);

        List<Path> paths = pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(path)
        );
        assertEquals(1, paths.size());
        assertEquals(new Path("level2.level1.valid.host.gravitee.io", "/validPath/", false), paths.get(0));
    }

    @Test
    public void validate_hostSubDomainOfOneOfDomainConstraints() {
        Path path = getValidPath();
        String domainConstraint = path.getHost();
        path.setHost("level2.level1." + domainConstraint);

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setDomainRestrictions(Arrays.asList("test.gravitee.io", "other.gravitee.io", domainConstraint));
        when(environmentService.findById(any())).thenReturn(environmentEntity);

        List<Path> paths = pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(path)
        );
        assertEquals(1, paths.size());
        assertEquals(new Path("level2.level1.valid.host.gravitee.io", "/validPath/", false), paths.get(0));
    }

    @Test(expected = InvalidHostException.class)
    public void validate_notASubDomain() {
        Path path = getValidPath();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setDomainRestrictions(Arrays.asList("test.gravitee.io", "other.gravitee.io"));
        when(environmentService.findById(any())).thenReturn(environmentEntity);

        pathValidationService.validateAndSanitizePaths(GraviteeContext.getExecutionContext(), null, Collections.singletonList(path));
    }

    @Test
    public void validate_allowNullHostInVirtualHost() {
        final Path path1 = new Path("host1", "/path_1");
        final Path path2 = new Path(null, "/path_2");
        final Path path3 = new Path("host3", "/path_3");

        List<Path> paths = pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            List.of(path1, path2, path3)
        );

        assertEquals(3, paths.size());
        assertEquals(new Path("host1", "/path_1/", false), paths.get(0));
        assertEquals(new Path(null, "/path_2/", false), paths.get(1));
        assertEquals(new Path("host3", "/path_3/", false), paths.get(2));

        // Check that even with multiple path, apiRepository is called only once
        verify(apiRepository, times(1)).search(any(), isNull(), any());
    }

    @Test
    public void validate_nullIsNotASubDomain() {
        Path path = new Path();
        path.setHost(null);
        path.setPath("/validPath");

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setDomainRestrictions(Arrays.asList("test.gravitee.io", "other.gravitee.io"));
        when(environmentService.findById(any())).thenReturn(environmentEntity);

        final List<Path> paths = pathValidationService.validateAndSanitizePaths(
            GraviteeContext.getExecutionContext(),
            null,
            Collections.singletonList(path)
        );
        assertEquals(1, paths.size());
        assertEquals(new Path("test.gravitee.io", "/validPath/", false), paths.get(0));
    }

    private Api createMock(String api, String path) {
        return createMock(api, path, null);
    }

    private Api createMock(String api, String path, String host) {
        Api api1 = mock(Api.class);
        when(api1.getId()).thenReturn(api);
        if (host == null) {
            when(api1.getDefinition())
                .thenReturn("{\"id\": \"" + api + "\",\"name\": \"API 1\",\"proxy\": {\"context_path\": \"" + path + "\"}}");
        } else {
            when(api1.getDefinition())
                .thenReturn(
                    "{\"id\": \"" +
                    api +
                    "\",\"name\": \"API 1\",\"proxy\": {\"virtual_hosts\": [{\"host\": \"api.gravitee.io\", \"path\": \"" +
                    path +
                    "\"}]}}"
                );
        }

        return api1;
    }

    private Path getValidPath() {
        Path path = new Path();
        path.setHost("valid.host.gravitee.io");
        path.setPath("/validPath");
        return path;
    }
}
