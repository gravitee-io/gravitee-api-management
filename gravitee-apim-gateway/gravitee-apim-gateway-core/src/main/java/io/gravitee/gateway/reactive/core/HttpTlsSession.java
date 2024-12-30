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
package io.gravitee.gateway.reactive.core;

import io.gravitee.common.security.CertificateUtils;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.context.TlsSession;
import java.security.Principal;
import java.security.cert.Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link TlsSession} which acts as a wrapper for {@link SSLSession} and provides a mechanism to extract peer certificate from {@link HttpHeaders}
 */
@RequiredArgsConstructor
@Slf4j
public class HttpTlsSession implements TlsSession {

    private final SSLSession delegate;
    private final HttpHeaders headers;
    private final String clientAuthCertHeaderName;

    @Override
    public byte[] getId() {
        return delegate.getId();
    }

    @Override
    public SSLSessionContext getSessionContext() {
        return delegate.getSessionContext();
    }

    @Override
    public long getCreationTime() {
        return delegate.getCreationTime();
    }

    @Override
    public long getLastAccessedTime() {
        return delegate.getLastAccessedTime();
    }

    @Override
    public void invalidate() {
        delegate.invalidate();
    }

    @Override
    public boolean isValid() {
        return delegate.isValid();
    }

    @Override
    public void putValue(String name, Object value) {
        delegate.putValue(name, value);
    }

    @Override
    public Object getValue(String name) {
        return delegate.getValue(name);
    }

    @Override
    public void removeValue(String name) {
        delegate.removeValue(name);
    }

    @Override
    public String[] getValueNames() {
        return delegate.getValueNames();
    }

    @Override
    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        if (!canExtractClientCertFromHeader() && isSSLConnection()) {
            return delegate.getPeerCertificates();
        }

        if (!isSSLConnection()) {
            return extractPeerCertificatesFromHeader();
        }

        try {
            final Certificate[] peerCertificates = delegate.getPeerCertificates();
            if (peerCertificates == null || peerCertificates.length == 0) {
                log.debug("Unable to retrieve peer certificate from request, extracting from header {}", clientAuthCertHeaderName);
                return extractPeerCertificatesFromHeader();
            }
            return peerCertificates;
        } catch (SSLPeerUnverifiedException e) {
            log.debug("Unable to retrieve peer certificate from request, extracting from header {}", clientAuthCertHeaderName, e);
            return extractPeerCertificatesFromHeader();
        }
    }

    private boolean canExtractClientCertFromHeader() {
        return StringUtils.hasText(clientAuthCertHeaderName);
    }

    @Override
    public Certificate[] getLocalCertificates() {
        return delegate.getLocalCertificates();
    }

    @Override
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return delegate.getPeerPrincipal();
    }

    @Override
    public Principal getLocalPrincipal() {
        return delegate.getLocalPrincipal();
    }

    @Override
    public String getCipherSuite() {
        return delegate.getCipherSuite();
    }

    @Override
    public String getProtocol() {
        return delegate.getProtocol();
    }

    @Override
    public String getPeerHost() {
        return delegate.getPeerHost();
    }

    @Override
    public int getPeerPort() {
        return delegate.getPeerPort();
    }

    @Override
    public int getPacketBufferSize() {
        return delegate.getPacketBufferSize();
    }

    @Override
    public int getApplicationBufferSize() {
        return delegate.getApplicationBufferSize();
    }

    @Override
    public boolean isSSLConnection() {
        return delegate != null;
    }

    private Certificate[] extractPeerCertificatesFromHeader() {
        return CertificateUtils
            .extractCertificate(headers, clientAuthCertHeaderName)
            .map(certificate -> new Certificate[] { certificate })
            .orElse(new Certificate[0]);
    }
}
