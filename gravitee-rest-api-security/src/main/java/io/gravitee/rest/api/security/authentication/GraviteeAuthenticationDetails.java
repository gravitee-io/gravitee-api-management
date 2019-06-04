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
package io.gravitee.rest.api.security.authentication;

import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;

public class GraviteeAuthenticationDetails extends WebAuthenticationDetails {

  private final String remoteAddress;

  public GraviteeAuthenticationDetails(HttpServletRequest request) {
    super(request);
    final String xfHeader = request.getHeader("X-Forwarded-For");
    if (xfHeader == null) {
      this.remoteAddress = request.getRemoteAddr();
    } else {
      this.remoteAddress = xfHeader.split(",")[0];
    }
  }

  public boolean equals(Object obj) {
    if (obj instanceof GraviteeAuthenticationDetails) {
      GraviteeAuthenticationDetails rhs = (GraviteeAuthenticationDetails)obj;
      if (this.getRemoteAddress() == null && rhs.getRemoteAddress() != null) {
        return false;
      } else if (this.getRemoteAddress() != null && rhs.getRemoteAddress() == null) {
        return false;
      } else if (this.getRemoteAddress() != null && !this.getRemoteAddress().equals(rhs.getRemoteAddress())) {
        return false;
      } else if (this.getSessionId() == null && rhs.getSessionId() != null) {
        return false;
      } else if (this.getSessionId() != null && rhs.getSessionId() == null) {
        return false;
      } else {
        return this.getSessionId() == null || this.getSessionId().equals(rhs.getSessionId());
      }
    } else {
      return false;
    }
  }

  public int hashCode() {
    int code = 7654;
    if (this.getRemoteAddress() != null) {
      code *= this.getRemoteAddress().hashCode() % 7;
    }

    if (this.getSessionId() != null) {
      code *= this.getSessionId().hashCode() % 7;
    }

    return code;
  }

  @Override
  public String getRemoteAddress() {
    return this.remoteAddress;
  }
}
