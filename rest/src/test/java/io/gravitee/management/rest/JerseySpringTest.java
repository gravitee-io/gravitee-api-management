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
package io.gravitee.management.rest;

import io.gravitee.management.rest.resource.GraviteeApplication;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class JerseySpringTest {

    private JerseyTest _jerseyTest;

    public final WebTarget target(final String path)
    {
        return _jerseyTest.target(path);
    }

    @Before
    public void setup() throws Exception
    {
        _jerseyTest.setUp();
    }

    @After
    public void tearDown() throws Exception
    {
        _jerseyTest.tearDown();
    }

    @Autowired
    public void setApplicationContext(final ApplicationContext context)
    {
        _jerseyTest = new JerseyTest()
        {
            @Override
            protected Application configure()
            {
                ResourceConfig application = new GraviteeApplication();

                application.property("contextConfig", context);

                return application;
            }
        };
    }

//    protected abstract ResourceConfig configure();
}
