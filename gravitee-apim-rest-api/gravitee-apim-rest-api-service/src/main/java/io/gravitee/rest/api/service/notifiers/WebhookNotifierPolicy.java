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
package io.gravitee.rest.api.service.notifiers;

import io.gravitee.rest.api.service.exceptions.MessageUrlForbiddenException;
import io.gravitee.rest.api.service.exceptions.NotifierDisabledException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Applies {@code notifiers.webhook.enabled} and {@code notifiers.webhook.whitelist} to the URLs of webhook
 * notifications. An empty whitelist allows every URL.
 */
@Component
public class WebhookNotifierPolicy {

    private final boolean enabled;
    private final List<String> whitelist = new ArrayList<>();

    public WebhookNotifierPolicy(Environment environment) {
        this.enabled = environment.getProperty("notifiers.webhook.enabled", Boolean.class, true);
        String whitelistUrl;
        int i = 0;
        while ((whitelistUrl = environment.getProperty("notifiers.webhook.whitelist[" + i + "]")) != null) {
            whitelist.add(whitelistUrl);
            i++;
        }
    }

    public void assertAllowed(String url) {
        if (!enabled) {
            throw new NotifierDisabledException();
        }
        if (!isWhitelisted(url)) {
            throw new MessageUrlForbiddenException();
        }
    }

    public boolean isAllowed(String url) {
        return enabled && isWhitelisted(url);
    }

    private boolean isWhitelisted(String url) {
        if (whitelist.isEmpty()) {
            return true;
        }
        // A prefix only matches on a path boundary, so "https://allowed.example" does not admit "https://allowed.example.evil".
        return (
            url != null &&
            whitelist
                .stream()
                .anyMatch(whitelistUrl ->
                    whitelistUrl.endsWith("/")
                        ? url.startsWith(whitelistUrl)
                        : (url.equals(whitelistUrl) || url.startsWith(whitelistUrl + '/'))
                )
        );
    }
}
