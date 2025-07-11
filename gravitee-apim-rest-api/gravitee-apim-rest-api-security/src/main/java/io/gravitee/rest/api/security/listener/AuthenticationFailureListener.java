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
package io.gravitee.rest.api.security.listener;

import io.gravitee.rest.api.security.authentication.GraviteeAuthenticationDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;

@Slf4j
public class AuthenticationFailureListener implements ApplicationListener<AbstractAuthenticationFailureEvent> {

    @Override
    public void onApplicationEvent(AbstractAuthenticationFailureEvent e) {
        GraviteeAuthenticationDetails auth = (GraviteeAuthenticationDetails) e.getAuthentication().getDetails();
        log.warn("Authentication failed event for: {} - IP: {}", e.getAuthentication().getPrincipal(), auth.getRemoteAddress());
    }
}
