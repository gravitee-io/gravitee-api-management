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
package io.gravitee.gateway.core.external;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet mock d'une api.
 */
@WebServlet(asyncSupported = true, loadOnStartup = 1, urlPatterns = "/*")
public class ApiServlet extends HttpServlet {

  /**
   * Serial id.
   */
  private static final long serialVersionUID = 7084816556439818855L;

  /**
   * Constructeur par défaut.
   */
  public ApiServlet() {
    super();
  }

  /**
   *
   */
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String query = req.getQueryString();

    if (query != null && !query.isEmpty()) {
      resp.getOutputStream().print(query);
    } else {
      resp.getOutputStream().print("hello");
    }
  }

}
