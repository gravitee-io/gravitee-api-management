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
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.NewsletterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static io.gravitee.common.http.HttpMethod.POST;
import static java.util.Collections.emptyMap;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class NewsletterServiceImpl extends TransactionalService implements NewsletterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewsletterServiceImpl.class);

    @Autowired
    private HttpClientService httpClientService;
    @Autowired
    private ObjectMapper mapper;
    @Value("${newsletter.enabled:true}")
    private boolean newsletterEnabled;

    @Override
    @Async
    public void subscribe(final Object user) {
        try {
            if (isEnabled()) {
                httpClientService.request(POST, "https://download.gravitee.io/newsletter", emptyMap(),
                        mapper.writeValueAsString(user), null);
            }
        } catch (final Exception e) {
            LOGGER.error("Error while subscribing to newsletters cause: {}", e.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return newsletterEnabled;
    }
}
