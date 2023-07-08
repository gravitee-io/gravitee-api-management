package io.gravitee.rest.api.service.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.rest.api.model.api.ApiEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * @author GraviteeSource Team
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class ApiSynchronizationProcessorTest {
    private final ApiSynchronizationProcessor apiSynchronizationProcessor = new ApiSynchronizationProcessor();

    @Mock
    private ObjectMapper objectMapper;

    @ParameterizedTest
    @MethodSource("provideApiEntityCrossIds")
    @DisplayName("GIVEN the currently deployed API state and the API state to be potentially deployed\nWHEN the API states have been processed\nTHEN the API states should have equal crossIds")
    void thenCrossIdsShouldBeEqual(String deployedApiCrossId, String apiToDeployCrossId) throws JsonProcessingException {
        ApiEntity deployedApi = new ApiEntity();
        ApiEntity apiToDeploy = new ApiEntity();

        deployedApi.setCrossId(deployedApiCrossId);
        apiToDeploy.setCrossId(apiToDeployCrossId);

        apiSynchronizationProcessor.setDeployedApi(deployedApi);
        apiSynchronizationProcessor.setApiToDeploy(apiToDeploy);
        apiSynchronizationProcessor.ignoreCrossIds();


        boolean crossIdsAreEqual = Objects.equals(apiSynchronizationProcessor.getDeployedApi().getCrossId(), apiSynchronizationProcessor.getApiToDeploy().getCrossId());

        assertThat(crossIdsAreEqual, is(true));
    }

    /**
     * Provide ApiEntity CrossIds for testing
     *
     * @return Stream of CrossId values to test
     */
    private Stream<Arguments> provideApiEntityCrossIds() {
        return Stream.of(
                Arguments.of(null, "c38d779e-6e7e-472b-8d77-9e6e7e172b78"),
                Arguments.of("db770e80-2547-4d48-b70e-8025477d4880", "c38d779e-6e7e-472b-8d77-9e6e7e172b78"),
                Arguments.of("", null)
        );
    }

}
