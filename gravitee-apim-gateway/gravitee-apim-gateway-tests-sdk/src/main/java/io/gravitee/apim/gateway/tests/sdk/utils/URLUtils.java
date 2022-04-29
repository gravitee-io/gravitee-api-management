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
package io.gravitee.apim.gateway.tests.sdk.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility class to transform URLs
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class URLUtils {

    private URLUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String exchangePort(URL url, int port) {
        try {
            return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), port, url.getPath(), url.getQuery(), url.getRef())
                .toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static String exchangePort(String sUri, int port) {
        try {
            return exchangePort(new URL(sUri), port);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
