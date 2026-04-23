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
package io.gravitee.apim.infra.domain_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.rest.api.service.exceptions.UrlForbiddenException;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WsdlParserDomainServiceImplTest {

    private final ImportConfiguration importConfiguration = mock(ImportConfiguration.class);
    private final WsdlParserDomainServiceImpl service = new WsdlParserDomainServiceImpl(importConfiguration);

    @Test
    void should_parse_inline_wsdl_content() {
        when(importConfiguration.isAllowImportFromPrivate()).thenReturn(false);
        when(importConfiguration.getImportWhitelist()).thenReturn(List.of());

        var result = service.toOpenApiYaml(loadWsdl());

        assertThat(result).isNotNull().startsWith("openapi:");
    }

    @Test
    void should_throw_when_url_points_to_private_address() {
        when(importConfiguration.isAllowImportFromPrivate()).thenReturn(false);
        when(importConfiguration.getImportWhitelist()).thenReturn(List.of());

        var throwable = catchThrowable(() -> service.toOpenApiYaml("http://192.168.1.1/spec.wsdl"));

        assertThat(throwable).isInstanceOf(UrlForbiddenException.class);
    }

    @SneakyThrows
    private String loadWsdl() {
        return Resources.toString(Resources.getResource("wsdl/calculator.wsdl"), Charsets.UTF_8);
    }
}
