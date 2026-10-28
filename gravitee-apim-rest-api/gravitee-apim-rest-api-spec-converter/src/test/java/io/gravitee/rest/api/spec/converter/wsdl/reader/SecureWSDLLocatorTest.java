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
package io.gravitee.rest.api.spec.converter.wsdl.reader;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.InputSource;

class SecureWSDLLocatorTest {

    @ParameterizedTest
    @ValueSource(strings = { "file:///etc/passwd", "jar:file:/tmp/x.jar!/a.wsdl", "ftp://example.com/a.wsdl" })
    void should_reject_disallowed_resolved_url_schemes(String location) throws Exception {
        assertThatThrownBy(() -> SecureWSDLLocator.rejectDisallowedResolvedUrl(new URL(location)))
            .isInstanceOf(WsdlImportResolutionException.class)
            .hasMessageContaining("Unsupported WSDL import location scheme");
    }

    @ParameterizedTest
    @ValueSource(strings = { "https://example.com/service.wsdl", "http://example.com/service.wsdl" })
    void should_allow_http_resolved_urls(String location) throws Exception {
        assertThatCode(() -> SecureWSDLLocator.rejectDisallowedResolvedUrl(new URL(location))).doesNotThrowAnyException();
    }

    @Test
    void should_reject_relative_import_when_no_http_parent_base() {
        SecureWSDLLocator locator = new SecureWSDLLocator(new InputSource());
        assertThatThrownBy(() -> locator.getImportInputSource(null, "child.wsdl"))
            .isInstanceOf(WsdlImportResolutionException.class)
            .hasMessageContaining("Unsupported WSDL import location scheme");
    }

    @Test
    void should_reject_blank_import_location() {
        SecureWSDLLocator locator = new SecureWSDLLocator(new InputSource());
        assertThatThrownBy(() -> locator.getImportInputSource(null, "  "))
            .isInstanceOf(WsdlImportResolutionException.class)
            .hasMessageContaining("must not be empty");
    }

    @Test
    void should_reject_a_null_resolved_url() {
        assertThatThrownBy(() -> SecureWSDLLocator.rejectDisallowedResolvedUrl(null))
            .isInstanceOf(WsdlImportResolutionException.class)
            .hasMessageContaining("Unable to resolve");
    }

    @Test
    void should_reject_an_unresolvable_import_location() {
        SecureWSDLLocator locator = new SecureWSDLLocator(new InputSource());
        assertThatThrownBy(() -> locator.getImportInputSource("https://example.com/parent.wsdl", "netdoc:/tmp/x.wsdl"))
            .isInstanceOf(WsdlImportResolutionException.class)
            .hasMessageContaining("Unable to resolve");
    }

    @Test
    void should_reject_an_http_import_that_cannot_be_opened() {
        SecureWSDLLocator locator = new SecureWSDLLocator(new InputSource());
        assertThatThrownBy(() -> locator.getImportInputSource(null, "http://127.0.0.1:1/missing.wsdl"))
            .isInstanceOf(WsdlImportResolutionException.class)
            .hasMessageContaining("Unable to resolve");
    }

    @Test
    void should_reject_file_scheme_even_when_parent_is_http() {
        SecureWSDLLocator locator = new SecureWSDLLocator(new InputSource());
        assertThatThrownBy(() -> locator.getImportInputSource("https://example.com/parent.wsdl", "file:///etc/passwd"))
            .isInstanceOf(WsdlImportResolutionException.class)
            .hasMessageContaining("Unsupported WSDL import location scheme");
    }
}
