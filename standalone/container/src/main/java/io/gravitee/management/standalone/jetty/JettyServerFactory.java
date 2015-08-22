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
package io.gravitee.management.standalone.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JettyServerFactory implements FactoryBean<Server> {

    @Override
    public Server getObject() throws Exception {
        Resource fileserver_xml = Resource.newSystemResource("gravitee-management-jetty.xml");
        XmlConfiguration configuration = new XmlConfiguration(fileserver_xml.getInputStream());
        return (Server) configuration.configure();
    }

    @Override
    public Class<?> getObjectType() {
        return Server.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
