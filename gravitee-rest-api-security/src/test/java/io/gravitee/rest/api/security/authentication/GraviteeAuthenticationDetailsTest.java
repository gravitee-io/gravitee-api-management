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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import io.gravitee.rest.api.security.authentication.GraviteeAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GraviteeAuthenticationDetailsTest {

  @Mock
  private HttpServletRequest httpServletRequest;

  @Test
  public void shouldNotOverrideGetSession() {
    HttpSession sessionMock = mock(HttpSession.class);
    when(httpServletRequest.getSession(false)).thenReturn(sessionMock);
    when(sessionMock.getId()).thenReturn("sessionId");
    GraviteeAuthenticationDetails authenticationDetails = new GraviteeAuthenticationDetails(httpServletRequest);
    assertEquals("sessionId", authenticationDetails.getSessionId());
  }

  @Test
  public void shouldGetCorrectIPWhenProxyRequest() {
    when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("CORRECT_IP,PROXY_IP");
    when(httpServletRequest.getRemoteAddr()).thenReturn("PROXY_IP");
    GraviteeAuthenticationDetails authenticationDetails = new GraviteeAuthenticationDetails(httpServletRequest);
    assertEquals("CORRECT_IP", authenticationDetails.getRemoteAddress());
  }

  @Test
  public void shouldGetCorrectIPWithoutProxy() {
    when(httpServletRequest.getRemoteAddr()).thenReturn("CORRECT_IP");
    GraviteeAuthenticationDetails authenticationDetails = new GraviteeAuthenticationDetails(httpServletRequest);
    assertEquals("CORRECT_IP", authenticationDetails.getRemoteAddress());
  }

  @Test
  public void shouldObjectBeEqualsForSameRequest() {
    when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("CORRECT_IP,PROXY_IP");
    when(httpServletRequest.getRemoteAddr()).thenReturn("PROXY_IP");
    GraviteeAuthenticationDetails authenticationDetails = new GraviteeAuthenticationDetails(httpServletRequest);
    assertEquals(authenticationDetails, new GraviteeAuthenticationDetails(httpServletRequest));
  }

  @Test
  public void shouldNotBeEqualsWhenCompareToParentType() {
    when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("CORRECT_IP,PROXY_IP");
    when(httpServletRequest.getRemoteAddr()).thenReturn("PROXY_IP");
    GraviteeAuthenticationDetails authenticationDetails = new GraviteeAuthenticationDetails(httpServletRequest);
    assertNotEquals(authenticationDetails, new WebAuthenticationDetails(httpServletRequest));
  }

  @Test
  public void shouldHashCodeBeEqualsForSameValues() {
    when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("CORRECT_IP,PROXY_IP");
    when(httpServletRequest.getRemoteAddr()).thenReturn("PROXY_IP");
    GraviteeAuthenticationDetails authenticationDetails = new GraviteeAuthenticationDetails(httpServletRequest);
    Map<GraviteeAuthenticationDetails, String> map = new HashMap<>();
    map.put(authenticationDetails, "found");
    assertEquals("found", map.get(authenticationDetails));
  }

}