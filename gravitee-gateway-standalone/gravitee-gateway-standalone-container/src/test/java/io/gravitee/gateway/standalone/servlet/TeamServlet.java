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
package io.gravitee.gateway.standalone.servlet;

import io.gravitee.gateway.standalone.utils.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class TeamServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String query = req.getQueryString();

        if (query != null && !query.isEmpty() && req.getParameter("q") != null) {
            resp.setHeader("Content-Type", "text/plain; charset=UTF-8");
            resp.getOutputStream().print(req.getParameter("q"));
        } else if (query != null && !query.isEmpty()) {
            resp.setHeader("Content-Type", "text/plain; charset=UTF-8");
            resp.getOutputStream().print(URLDecoder.decode(req.getQueryString()));
        } else {
            resp.getOutputStream().print("hello");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String reqCase = req.getParameter("case");
        String mode = req.getParameter("mode");

        if (mode == null || ! mode.isEmpty()) {
            mode = "chunk";
        }

        if (reqCase != null && ! reqCase.isEmpty()) {
            if (mode.equals("raw")) {
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(reqCase + "/response_content.json");
                String content = StringUtils.copy(is);
                resp.getOutputStream().print(content);
            } else {
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(reqCase + "/response_content.json");
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    resp.getOutputStream().write(buffer, 0, len);
                    // Do not flush since we are leaving this to the Jetty container
                    // resp.getOutputStream().flush();
                }
            }
        } else {
            String content = StringUtils.copy(req.getInputStream());
            resp.getOutputStream().print(content);
        }
    }

}
