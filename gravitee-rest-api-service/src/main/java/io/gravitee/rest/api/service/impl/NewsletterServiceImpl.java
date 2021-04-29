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

import static io.gravitee.common.http.HttpMethod.GET;
import static io.gravitee.common.http.HttpMethod.POST;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.service.EmailValidator;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.NewsletterService;
import io.gravitee.rest.api.service.exceptions.EmailFormatInvalidException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.vertx.core.buffer.Buffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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

    @Value("${newsletter.url:https://newsletter.gravitee.io/}")
    private String newsletterUrl;

    @Override
    @Async
    public void subscribe(final String email) {
        try {
            if (isEnabled()) {
                if (!EmailValidator.isValid(email)) {
                    throw new EmailFormatInvalidException(email);
                }
                final Map<String, Object> newsletterInfo = new HashMap<>();
                newsletterInfo.put("email", email);
                httpClientService.request(POST, newsletterUrl, emptyMap(), mapper.writeValueAsString(newsletterInfo), null);
            }
        } catch (final Exception e) {
            LOGGER.error("Error while subscribing to newsletters cause: {}", e.getMessage());
        }
    }

    @Override
    public List<String> getTaglines() {
        try {
            if (isEnabled()) {
                String taglinesPath = "taglines";
                if (newsletterUrl.lastIndexOf('/') != newsletterUrl.length() - 1) {
                    taglinesPath = "/" + taglinesPath;
                }
                final Buffer buffer = httpClientService.request(GET, newsletterUrl + taglinesPath, emptyMap(), null, null);
                return mapper.readValue(buffer.toString(), List.class);
            }
        } catch (Exception e) {
            LOGGER.debug("Error while reading the taglines response: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        return newsletterEnabled;
    }
}
