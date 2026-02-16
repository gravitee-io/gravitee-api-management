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
package io.gravitee.plugin.console;

import jakarta.servlet.http.HttpServlet;
import org.springframework.context.ApplicationContext;

/**
 * Interface for console extensions that provide backend servlets.
 * Implementations create an {@link HttpServlet} and declare the servlet path
 * and Spring configuration classes for the child application context.
 *
 * @author GraviteeSource Team
 */
public interface ConsoleExtensionServletFactory {
    /**
     * Creates the HTTP servlet that handles requests for this extension.
     *
     * @param parentContext the parent Spring application context
     * @return the servlet instance
     */
    HttpServlet createServlet(ApplicationContext parentContext);

    /**
     * Returns the servlet context path (e.g. "/ai").
     */
    String getServletPath();

    /**
     * Returns the Spring configuration classes to register in the child application context.
     */
    Class<?>[] getSpringConfigClasses();
}
