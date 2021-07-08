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
package io.gravitee.repository.mongodb.management.mapper;

import com.github.dozermapper.core.util.DozerClassLoader;
import com.github.dozermapper.core.util.MappingUtils;
import com.github.dozermapper.core.util.ResourceLoader;
import java.net.URL;
import org.apache.commons.lang3.ClassUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RepositoryDozerClassLoader implements DozerClassLoader {

    private final ResourceLoader resourceLoader = new ResourceLoader(this.getClass().getClassLoader());
    private final ClassLoader classLoader = this.getClass().getClassLoader();

    public RepositoryDozerClassLoader() {}

    public Class<?> loadClass(String className) {
        Class result = null;

        try {
            result = ClassUtils.getClass(classLoader, className);
        } catch (ClassNotFoundException var4) {
            MappingUtils.throwMappingException(var4);
        }

        return result;
    }

    public URL loadResource(String uri) {
        return this.resourceLoader.getResource(uri);
    }
}
