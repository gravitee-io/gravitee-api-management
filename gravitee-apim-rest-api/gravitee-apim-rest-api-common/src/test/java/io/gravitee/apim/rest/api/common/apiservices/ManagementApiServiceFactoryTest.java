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
package io.gravitee.apim.rest.api.common.apiservices;

import static io.gravitee.apim.rest.api.common.apiservices.ManagementApiServiceFactory.DEPLOYMENT_CONTEXT_MESSAGE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.reactivex.rxjava3.core.Completable;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ManagementApiServiceFactoryTest {

    @Test
    void should_throw_when_creating_service_with_DeploymentContext() {
        assertThatThrownBy(() ->
            new TestingManagementApiServiceFactory().createService(
                new DeploymentContext() {
                    @Override
                    public <T> T getComponent(Class<T> componentClass) {
                        return null;
                    }

                    @Override
                    public TemplateEngine getTemplateEngine() {
                        return null;
                    }
                }
            )
        )
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage(DEPLOYMENT_CONTEXT_MESSAGE);
    }

    static class TestingManagementApiServiceFactory implements ManagementApiServiceFactory<TestingManagementApiService> {

        @Override
        public TestingManagementApiService createService(DefaultManagementDeploymentContext deploymentContext) {
            return new TestingManagementApiService();
        }
    }

    static class TestingManagementApiService implements ManagementApiService {

        @Override
        public Completable update(AbstractApi api) {
            return Completable.complete();
        }

        @Override
        public String id() {
            return "testing-mgt-api-service";
        }

        @Override
        public String kind() {
            return "test";
        }

        @Override
        public Completable start() {
            return Completable.complete();
        }

        @Override
        public Completable stop() {
            return Completable.complete();
        }
    }
}
