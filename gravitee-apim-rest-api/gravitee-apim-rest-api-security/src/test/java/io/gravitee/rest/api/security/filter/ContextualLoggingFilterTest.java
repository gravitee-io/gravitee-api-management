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
package io.gravitee.rest.api.security.filter;

import static io.gravitee.rest.api.security.filter.ContextualLoggingFilter.APIS_SEGMENT;
import static io.gravitee.rest.api.security.filter.ContextualLoggingFilter.API_ID_KEY;
import static io.gravitee.rest.api.security.filter.ContextualLoggingFilter.APPLICATIONS_SEGMENT;
import static io.gravitee.rest.api.security.filter.ContextualLoggingFilter.APP_ID_KEY;
import static io.gravitee.rest.api.security.filter.ContextualLoggingFilter.CORRELATION_ID_HEADER;
import static io.gravitee.rest.api.security.filter.ContextualLoggingFilter.CORRELATION_ID_KEY;
import static io.gravitee.rest.api.security.filter.ContextualLoggingFilter.ENV_ID_KEY;
import static io.gravitee.rest.api.security.filter.ContextualLoggingFilter.ORG_ID_KEY;
import static io.gravitee.rest.api.security.filter.ContextualLoggingFilter.TRACE_PARENT_HEADER;
import static io.gravitee.rest.api.security.filter.ContextualLoggingFilter.TRACE_PARENT_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ContextualLoggingFilterTest {

    private final ContextualLoggingFilter filter = new ContextualLoggingFilter();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        GraviteeContext.setCurrentOrganization("my-org");
        GraviteeContext.setCurrentEnvironment("my-env");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        GraviteeContext.cleanContext();
    }

    @Nested
    class OrganizationAndEnvironment {

        @Test
        void should_put_org_and_env_in_mdc() throws Exception {
            // When
            filter.doFilter(request, response, (req, res) -> {
                // Then
                assertThat(MDC.get(ORG_ID_KEY)).isEqualTo("my-org");
                assertThat(MDC.get(ENV_ID_KEY)).isEqualTo("my-env");
            });
        }

        @Test
        void should_not_put_org_in_mdc_when_null() throws Exception {
            // Given
            GraviteeContext.cleanContext();
            GraviteeContext.setCurrentOrganization(null);
            GraviteeContext.setCurrentEnvironment(null);

            // When
            filter.doFilter(request, response, (req, res) -> {
                // Then
                assertThat(MDC.get(ORG_ID_KEY)).isNull();
                assertThat(MDC.get(ENV_ID_KEY)).isNull();
            });
        }
    }

    @Nested
    class Headers {

        @Test
        void should_put_correlation_id_in_mdc() throws Exception {
            // Given
            request.addHeader(CORRELATION_ID_HEADER, "corr-123");

            // When
            filter.doFilter(request, response, (req, res) -> {
                // Then
                assertThat(MDC.get(CORRELATION_ID_KEY)).isEqualTo("corr-123");
            });
        }

        @Test
        void should_put_traceparent_in_mdc() throws Exception {
            // Given
            request.addHeader(TRACE_PARENT_HEADER, "00-trace-id-span-id-01");

            // When
            filter.doFilter(request, response, (req, res) -> {
                // Then
                assertThat(MDC.get(TRACE_PARENT_KEY)).isEqualTo("00-trace-id-span-id-01");
            });
        }

        @Test
        void should_not_put_headers_in_mdc_when_absent() throws Exception {
            // When
            filter.doFilter(request, response, (req, res) -> {
                // Then
                assertThat(MDC.get(CORRELATION_ID_KEY)).isNull();
                assertThat(MDC.get(TRACE_PARENT_KEY)).isNull();
            });
        }
    }

    @Nested
    class PathParams {

        @Test
        void should_extract_api_id_from_path() throws Exception {
            // Given
            request.setPathInfo("/" + APIS_SEGMENT + "/api-123/subscribers");

            // When
            filter.doFilter(request, response, (req, res) -> {
                // Then
                assertThat(MDC.get(API_ID_KEY)).isEqualTo("api-123");
            });
        }

        @Test
        void should_extract_application_id_from_path() throws Exception {
            // Given
            request.setPathInfo("/" + APPLICATIONS_SEGMENT + "/app-456/members");

            // When
            filter.doFilter(request, response, (req, res) -> {
                // Then
                assertThat(MDC.get(APP_ID_KEY)).isEqualTo("app-456");
            });
        }

        @Test
        void should_not_extract_api_id_when_segment_is_last() throws Exception {
            // Given
            request.setPathInfo("/" + APIS_SEGMENT);

            // When
            filter.doFilter(request, response, (req, res) -> {
                // Then
                assertThat(MDC.get(API_ID_KEY)).isNull();
            });
        }

        @Test
        void should_not_extract_ids_when_path_is_null() throws Exception {
            // Given
            request.setPathInfo(null);

            // When
            filter.doFilter(request, response, (req, res) -> {
                // Then
                assertThat(MDC.get(API_ID_KEY)).isNull();
                assertThat(MDC.get(APP_ID_KEY)).isNull();
            });
        }

        @Test
        void should_not_extract_ids_when_path_is_blank() throws Exception {
            // Given
            request.setPathInfo("   ");

            // When
            filter.doFilter(request, response, (req, res) -> {
                // Then
                assertThat(MDC.get(API_ID_KEY)).isNull();
                assertThat(MDC.get(APP_ID_KEY)).isNull();
            });
        }

        @Test
        void should_not_extract_api_id_when_segment_value_is_empty() throws Exception {
            // Given
            request.setPathInfo("/" + APIS_SEGMENT + "//subscribers");

            // When
            filter.doFilter(request, response, (req, res) -> {
                // Then
                assertThat(MDC.get(API_ID_KEY)).isNull();
            });
        }

        @Test
        void should_not_extract_application_id_when_segment_value_is_empty() throws Exception {
            // Given
            request.setPathInfo("/" + APPLICATIONS_SEGMENT + "//members");

            // When
            filter.doFilter(request, response, (req, res) -> {
                // Then
                assertThat(MDC.get(APP_ID_KEY)).isNull();
            });
        }
    }

    @Nested
    class NonHttpRequest {

        @Test
        void should_only_process_context_for_non_http_request() throws Exception {
            // Given
            ServletRequest nonHttpRequest = buildNonHttpServletRequest();
            ServletResponse nonHttpResponse = buildNonHttpServletResponse();

            // When
            filter.doFilter(nonHttpRequest, nonHttpResponse, (req, res) -> {
                // Then
                assertThat(MDC.get(ORG_ID_KEY)).isEqualTo("my-org");
                assertThat(MDC.get(CORRELATION_ID_KEY)).isNull();
                assertThat(MDC.get(API_ID_KEY)).isNull();
            });
        }
    }

    @Nested
    class MdcCleanup {

        @Test
        void should_clear_mdc_at_start_of_each_request() throws Exception {
            // Given
            MDC.put(ORG_ID_KEY, "stale-org");
            MDC.put(CORRELATION_ID_KEY, "stale-corr");

            // When
            filter.doFilter(request, response, (req, res) -> {
                // Then
                assertThat(MDC.get(ORG_ID_KEY)).isEqualTo("my-org");
                assertThat(MDC.get(CORRELATION_ID_KEY)).isNull();
            });
        }
    }

    private static ServletRequest buildNonHttpServletRequest() {
        return new ServletRequest() {
            public String getCharacterEncoding() {
                return null;
            }

            public void setCharacterEncoding(String env) {}

            public int getContentLength() {
                return 0;
            }

            public long getContentLengthLong() {
                return 0;
            }

            public String getContentType() {
                return null;
            }

            public ServletInputStream getInputStream() {
                return null;
            }

            public String getParameter(String name) {
                return null;
            }

            public Enumeration<String> getParameterNames() {
                return null;
            }

            public String[] getParameterValues(String name) {
                return null;
            }

            public Map<String, String[]> getParameterMap() {
                return null;
            }

            public String getProtocol() {
                return null;
            }

            public String getScheme() {
                return null;
            }

            public String getServerName() {
                return null;
            }

            public int getServerPort() {
                return 0;
            }

            public BufferedReader getReader() {
                return null;
            }

            public String getRemoteAddr() {
                return null;
            }

            public String getRemoteHost() {
                return null;
            }

            public void setAttribute(String name, Object o) {}

            public void removeAttribute(String name) {}

            public Locale getLocale() {
                return null;
            }

            public Enumeration<Locale> getLocales() {
                return null;
            }

            public boolean isSecure() {
                return false;
            }

            public RequestDispatcher getRequestDispatcher(String path) {
                return null;
            }

            public int getRemotePort() {
                return 0;
            }

            public String getLocalName() {
                return null;
            }

            public String getLocalAddr() {
                return null;
            }

            public int getLocalPort() {
                return 0;
            }

            public ServletContext getServletContext() {
                return null;
            }

            public AsyncContext startAsync() {
                return null;
            }

            public AsyncContext startAsync(ServletRequest req, ServletResponse res) {
                return null;
            }

            public boolean isAsyncStarted() {
                return false;
            }

            public boolean isAsyncSupported() {
                return false;
            }

            public AsyncContext getAsyncContext() {
                return null;
            }

            public DispatcherType getDispatcherType() {
                return null;
            }

            public String getRequestId() {
                return null;
            }

            public String getProtocolRequestId() {
                return null;
            }

            public ServletConnection getServletConnection() {
                return null;
            }

            public Object getAttribute(String name) {
                return null;
            }

            public Enumeration<String> getAttributeNames() {
                return null;
            }
        };
    }

    private static ServletResponse buildNonHttpServletResponse() {
        return new ServletResponse() {
            public String getCharacterEncoding() {
                return null;
            }

            public String getContentType() {
                return null;
            }

            public ServletOutputStream getOutputStream() {
                return null;
            }

            public PrintWriter getWriter() {
                return null;
            }

            public void setCharacterEncoding(String charset) {}

            public void setContentLength(int len) {}

            public void setContentLengthLong(long len) {}

            public void setContentType(String type) {}

            public void setBufferSize(int size) {}

            public int getBufferSize() {
                return 0;
            }

            public void flushBuffer() {}

            public void resetBuffer() {}

            public boolean isCommitted() {
                return false;
            }

            public void reset() {}

            public void setLocale(Locale loc) {}

            public Locale getLocale() {
                return null;
            }
        };
    }
}
