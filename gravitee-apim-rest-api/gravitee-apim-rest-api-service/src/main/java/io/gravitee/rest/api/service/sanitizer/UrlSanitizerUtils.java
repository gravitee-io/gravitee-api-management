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
package io.gravitee.rest.api.service.sanitizer;

import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.UrlForbiddenException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UrlSanitizerUtils {

    public static void checkAllowed(String url, List<String> whitelist, boolean allowPrivate) {
        checkUrlForbiddenCharacters(url);
        checkUriSyntax(url);
        if (whitelist != null && !whitelist.isEmpty()) {
            if (
                whitelist
                    .stream()
                    .noneMatch(whitelistUrl ->
                        whitelistUrl.endsWith("/")
                            ? url.startsWith(whitelistUrl)
                            : (url.equals(whitelistUrl) || url.startsWith(whitelistUrl + '/'))
                    )
            ) {
                throw new UrlForbiddenException();
            }
        } else if (!allowPrivate && UrlSanitizerUtils.isPrivate(url)) {
            throw new UrlForbiddenException();
        }
    }

    public static boolean isPrivate(String url) {
        try {
            InetAddress inetAddress = Inet6Address.getByName(new URL(url).getHost());

            return (
                inetAddress.isSiteLocalAddress() ||
                inetAddress.isAnyLocalAddress() ||
                inetAddress.isLoopbackAddress() ||
                inetAddress.isLinkLocalAddress() ||
                inetAddress.isMulticastAddress() ||
                isPrivateOwasp(inetAddress.getHostAddress())
            );
        } catch (Exception e) {
            throw new InvalidDataException("Url [" + url + "] is invalid");
        }
    }

    public static void checkUriSyntax(String url) {
        try {
            new URI(url);
        } catch (Exception e) {
            throw new UrlForbiddenException();
        }
    }

    /**
     * Check if url contains characters that may be used to enforce HTTP header injection attack
     */
    public static void checkUrlForbiddenCharacters(String url) {
        String urlEncodedCarriageReturn = "%0D";
        String urlEncodedLineFeed = "%0A";
        if (url.contains(urlEncodedCarriageReturn) || url.contains(urlEncodedLineFeed)) {
            throw new UrlForbiddenException();
        }
    }

    /**
     * Check ip address is private using owasp algorithm.
     */
    private static boolean isPrivateOwasp(String ipAddress) {
        List<String> ipPrefixes = new ArrayList<>();

        // Add prefix for loopback addresses.
        ipPrefixes.add("127.");
        ipPrefixes.add("0.");

        // Add IP V4 prefix for private addresses.
        // See https://en.wikipedia.org/wiki/Private_network
        ipPrefixes.add("10.");
        ipPrefixes.add("172.16.");
        ipPrefixes.add("172.17.");
        ipPrefixes.add("172.18.");
        ipPrefixes.add("172.19.");
        ipPrefixes.add("172.20.");
        ipPrefixes.add("172.21.");
        ipPrefixes.add("172.22.");
        ipPrefixes.add("172.23.");
        ipPrefixes.add("172.24.");
        ipPrefixes.add("172.25.");
        ipPrefixes.add("172.26.");
        ipPrefixes.add("172.27.");
        ipPrefixes.add("172.28.");
        ipPrefixes.add("172.29.");
        ipPrefixes.add("172.30.");
        ipPrefixes.add("172.31.");
        ipPrefixes.add("192.168.");
        ipPrefixes.add("169.254.");

        // Add IP V6 prefix for private addresses.
        // See https://en.wikipedia.org/wiki/Unique_local_address
        // See https://en.wikipedia.org/wiki/Private_network
        // See https://simpledns.com/private-ipv6
        ipPrefixes.add("fc");
        ipPrefixes.add("fd");
        ipPrefixes.add("fe");
        ipPrefixes.add("ff");
        ipPrefixes.add("::1");
        ipPrefixes.add("0:0:0:0:0:0:0:0");

        // Verify the provided IP address
        // Remove whitespace characters from the beginning/end of the string and convert it to lower case
        // Lower case is for preventing any IPV6 case bypass using mixed case depending on the source used to get the IP address

        String ipToVerify = ipAddress.toLowerCase();
        // Perform the check against the list of prefix

        for (String prefix : ipPrefixes) {
            if (ipToVerify.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }
}
