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
package io.gravitee.gateway.core.policy.impl;

import io.gravitee.gateway.core.policy.ClassLoaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ClassLoaderFactoryImpl implements ClassLoaderFactory {

    protected final Logger LOGGER = LoggerFactory.getLogger(ClassLoaderFactoryImpl.class);

    private final Map<String, ClassLoader> policyClassLoaderCache = new HashMap<>();

    @Override
    public ClassLoader createPolicyClassLoader(String policyId, URL[] urls) {
        ClassLoader cl;

        try {
            cl = policyClassLoaderCache.get(policyId);
            if (null == cl)  {
                cl = new URLClassLoader(urls, ClassLoaderFactoryImpl.class.getClassLoader());
                policyClassLoaderCache.put(policyId, cl);
            }

            LOGGER.debug("Created policy ClassLoader for {} with class path {}",
                    policyId, urls);

            return cl;
        }
        catch (Throwable t) {
            LOGGER.error("Unexpected error while creating policy classloader", t);
            return null;
        }
    }

    @Override
    public ClassLoader getPolicyClassLoader(String policyId) {
        return policyClassLoaderCache.get(policyId);
    }

    /**
     * Private method to convert a List into a URL array.
     *
     * @param paths list of String elements representing paths and JAR file
     * names
     * @return java.net.URL[] array representing the URLs corresponding to the
     * paths, or null if the list is null or if there is an exception creating
     * the array.
     * @throws Exception If the array creation is unsuccessful.
     */
    private URL[] list2URLArray (List<URL> paths) throws Exception {
        return paths.toArray(new URL[paths.size()]);
    }
}
