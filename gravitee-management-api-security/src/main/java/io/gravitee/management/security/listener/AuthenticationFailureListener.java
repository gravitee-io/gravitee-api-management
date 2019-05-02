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
package io.gravitee.management.security.listener;

import io.gravitee.management.security.authentication.GraviteeAuthenticationDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;

public class AuthenticationFailureListener implements ApplicationListener<AbstractAuthenticationFailureEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFailureListener.class);

  @Override
  public void onApplicationEvent(AbstractAuthenticationFailureEvent e) {
    GraviteeAuthenticationDetails auth = (GraviteeAuthenticationDetails)  e.getAuthentication().getDetails();
    LOGGER.warn("Authentication failed event for: " + e.getAuthentication().getPrincipal() + " - IP: " + auth.getRemoteAddress());
  }
}
