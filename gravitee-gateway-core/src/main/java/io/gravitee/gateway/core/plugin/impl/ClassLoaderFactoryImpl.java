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
package io.gravitee.gateway.core.plugin.impl;

import io.gravitee.gateway.core.plugin.ClassLoaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ClassLoaderFactoryImpl implements ClassLoaderFactory {

    protected final Logger LOGGER = LoggerFactory.getLogger(ClassLoaderFactoryImpl.class);

    private final Map<String, ClassLoader> pluginClassLoaderCache = new HashMap<>();

    @Override
    public ClassLoader createPluginClassLoader(String pluginId, URL[] urls) {
        ClassLoader cl;

        try {
            cl = pluginClassLoaderCache.get(pluginId);
            if (null == cl)  {
                cl = new URLClassLoader(urls, ClassLoaderFactoryImpl.class.getClassLoader());
                pluginClassLoaderCache.put(pluginId, cl);
            }

            LOGGER.debug("Created plugin ClassLoader for {} with class path {}",
                    pluginId, urls);

            return cl;
        }
        catch (Throwable t) {
            LOGGER.error("Unexpected error while creating plugin classloader", t);
            return null;
        }
    }

    @Override
    public ClassLoader getPluginClassLoader(String pluginId) {
        return pluginClassLoaderCache.get(pluginId);
    }

    @Override
    public void removePluginClassLoader(String pluginId) {
        pluginClassLoaderCache.remove(pluginId);
        LOGGER.debug("ClassLoader for plugin {} has been removed from cache",
                pluginId);
    }
}
