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
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.ReCaptchaService;
import io.vertx.core.buffer.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ReCaptchaServiceImpl implements ReCaptchaService {

    private static Logger LOGGER = LoggerFactory.getLogger(ReCaptchaServiceImpl.class);

    @Value("${reCaptcha.enabled:false}")
    private boolean enabled;

    @Value("${reCaptcha.siteKey}")
    private String siteKey;

    @Value("${reCaptcha.secretKey}")
    private String secretKey;

    @Value("${reCaptcha.minScore:0.5}")
    private Double minScore;

    @Value("${reCaptcha.serviceUrl:https://www.google.com/recaptcha/api/siteverify}")
    private String serviceUrl;

    @Autowired
    private HttpClientService httpClientService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean isValid(String token) {

        if (!this.isEnabled()) {
            LOGGER.debug("ReCaptchaService is disabled");
            return true;
        }

        LOGGER.debug("ReCaptchaService is enabled");

        try {
            if (token == null || "".equals(token.trim())) {
                LOGGER.info("Recaptcha token is empty");
                return false;
            }

            Buffer response = httpClientService.request(HttpMethod.POST, serviceUrl, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED), "secret=" + secretKey + "&response=" + token, false);
            Map res = objectMapper.readValue(response.toString(), Map.class);

            Boolean success = (Boolean) res.getOrDefault("success", false);
            Double score = (Double) res.getOrDefault("score", 0.0d);

            LOGGER.debug(String.format("ReCaptchaService success: %s score: %s", success, score));

            // Result should be successful and score above 0.5.
            return (success && score >= minScore);
        } catch (IOException e) {
            LOGGER.error("An error occurred when trying to validate ReCaptcha token.", e);
            return false;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getSiteKey() {
        return siteKey;
    }

}
