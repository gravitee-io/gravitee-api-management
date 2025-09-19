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
package io.gravitee.apim.plugin.apiservice.healthcheck.http.context;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.gravitee.apim.plugin.apiservice.healthcheck.http.HttpHealthCheckServiceConfiguration;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpMethod;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class HttpHealthCheckRequestTest {

    @Test
    public void should_be_initialize_using_configuration_without_body_and_headers() throws Exception {
        final var config = new HttpHealthCheckServiceConfiguration();
        config.setTarget("/relative/target/path");
        config.setMethod(HttpMethod.GET);
        final var cut = new HttpHealthCheckRequest(config);

        assertThat(cut.path()).isNotNull().isEqualTo(config.getTarget());
        assertThat(cut.method()).isNotNull().isEqualTo(config.getMethod());
        assertThat(cut.headers()).isNotNull();
        assertThat(cut.headers().size()).isEqualTo(0);
        assertThat(cut.body()).isNotNull();
        cut.body().test().await().assertNoValues();
    }

    @Test
    public void should_be_initialize_using_configuration_with_body() throws Exception {
        final var config = new HttpHealthCheckServiceConfiguration();
        config.setTarget("/relative/target/path");
        config.setMethod(HttpMethod.GET);
        config.setBody("Request Body");
        final var cut = new HttpHealthCheckRequest(config);

        assertThat(cut.path()).isNotNull().isEqualTo(config.getTarget());
        assertThat(cut.method()).isNotNull().isEqualTo(config.getMethod());
        assertThat(cut.headers()).isNotNull();
        assertThat(cut.headers().size()).isEqualTo(0);
        assertThat(cut.body()).isNotNull();
        cut
            .body()
            .test()
            .await()
            .assertValue(boby -> config.getBody().equals(boby.toString()));
    }

    @Test
    public void should_be_initialize_using_configuration_with_headers() throws Exception {
        final var config = new HttpHealthCheckServiceConfiguration();
        config.setTarget("/relative/target/path");
        config.setMethod(HttpMethod.GET);

        config.setHeaders(List.of(new HttpHeader("header1", "value1"), new HttpHeader("header2", "value2")));

        final var cut = new HttpHealthCheckRequest(config);

        assertThat(cut.path()).isNotNull().isEqualTo(config.getTarget());
        assertThat(cut.method()).isNotNull().isEqualTo(config.getMethod());
        assertThat(cut.body()).isNotNull();
        cut.body().test().await().assertNoValues();
        assertThat(cut.headers()).isNotNull();
        assertThat(cut.headers().size()).isEqualTo(2);
    }
}
