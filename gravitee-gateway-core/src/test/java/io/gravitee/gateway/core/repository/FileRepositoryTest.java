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
package io.gravitee.gateway.core.repository;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class FileRepositoryTest {

    @Test
    public void testConfigurationsFromFile() throws URISyntaxException {
        URL url = FileRepositoryTest.class.getResource("/registry/conf");

        FileRepository repository = new FileRepository(new File(url.getPath()));
        repository.init();

        Assert.assertTrue(repository.listAll().size()  == 1);
    }

    @Test
    public void testConfigurations() throws URISyntaxException {
        URL url = FileRepositoryTest.class.getResource("/registry/conf");
        Properties config = new Properties();
        config.put("repository.file.path", url.getPath());

        FileRepository repository = new FileRepository();
        repository.setConfiguration(config);
        repository.init();

        Assert.assertTrue(repository.listAll().size()  == 1);
    }
}
