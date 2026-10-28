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

import com.ibm.wsdl.util.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Set;
import javax.wsdl.xml.WSDLLocator;
import org.xml.sax.InputSource;

/**
 * Resolves nested WSDL/XSD imports like wsdl4j, then allow-lists the URL that
 * will actually be opened. Only {@code http} and {@code https} are permitted.
 * Relative locations are allowed only when they resolve against an http(s)
 * parent; with a null base (the production INLINE path), wsdl4j's File
 * fallback would yield {@code file:} and is rejected.
 */
public class SecureWSDLLocator implements WSDLLocator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final InputSource baseInputSource;
    private final String baseURI;
    private String latestImportURI;
    private InputStream lastImportStream;

    public SecureWSDLLocator(InputSource baseInputSource) {
        this(baseInputSource, null);
    }

    public SecureWSDLLocator(InputSource baseInputSource, String baseURI) {
        this.baseInputSource = baseInputSource;
        this.baseURI = baseURI;
    }

    @Override
    public InputSource getBaseInputSource() {
        return baseInputSource;
    }

    @Override
    public InputSource getImportInputSource(String parentLocation, String importLocation) {
        return open(resolve(parentLocation, importLocation));
    }

    /**
     * Resolve and allow-list the URL that would be opened. Does not fetch it.
     */
    static URL resolve(String parentLocation, String importLocation) {
        if (importLocation == null || importLocation.isBlank()) {
            throw new WsdlImportResolutionException("WSDL import location must not be empty.");
        }

        try {
            URL contextURL = null;
            if (parentLocation != null && !parentLocation.isBlank()) {
                contextURL = StringUtils.getURL(null, parentLocation);
            }
            URL url = StringUtils.getURL(contextURL, importLocation);
            rejectDisallowedResolvedUrl(url);
            return url;
        } catch (MalformedURLException e) {
            throw new WsdlImportResolutionException("Unable to resolve imported document at '" + importLocation + "'", e);
        }
    }

    InputSource open(URL url) {
        closeQuietly(lastImportStream);
        lastImportStream = null;
        try {
            lastImportStream = StringUtils.getContentAsInputStream(url);
            latestImportURI = url.toString();
            InputSource inputSource = new InputSource(lastImportStream);
            inputSource.setSystemId(latestImportURI);
            return inputSource;
        } catch (IOException e) {
            throw new WsdlImportResolutionException("Unable to resolve imported document at '" + url + "'", e);
        }
    }

    @Override
    public String getBaseURI() {
        return baseURI;
    }

    @Override
    public String getLatestImportURI() {
        return latestImportURI;
    }

    @Override
    public void close() {
        closeQuietly(lastImportStream);
        lastImportStream = null;
    }

    /**
     * Allow-list the scheme of the URL that will be opened (deny by default).
     */
    static void rejectDisallowedResolvedUrl(URL url) {
        if (url == null) {
            throw new WsdlImportResolutionException("Unable to resolve WSDL import location.");
        }
        String protocol = url.getProtocol();
        if (protocol == null || !ALLOWED_SCHEMES.contains(protocol.toLowerCase(Locale.ROOT))) {
            throw new WsdlImportResolutionException(
                "Unsupported WSDL import location scheme '" + protocol + "'. Only http and https are allowed."
            );
        }
    }

    private static void closeQuietly(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException ignored) {
            // best-effort
        }
    }
}
