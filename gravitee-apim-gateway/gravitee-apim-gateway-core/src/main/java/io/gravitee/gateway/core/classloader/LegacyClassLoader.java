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
package io.gravitee.gateway.core.classloader;

/**
 * A classloader that delegates first to the parent and then to a delegate loader
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LegacyClassLoader extends ClassLoader {

    private final ClassLoader[] delegates;
    private final ClassLoader mParentLoader;

    /**
     * Constructor for special classloader to give to proxy making code
     *
     * @param parent               the java-style classloader parent of this loader
     * @param classLoaderDelegates other classloaders to delegate to
     */
    public LegacyClassLoader(ClassLoader parent, ClassLoader... classLoaderDelegates) {
        super(parent);
        delegates = classLoaderDelegates;

        ClassLoader systemCL = getSystemClassLoader();
        mParentLoader = (parent != null) ? parent : systemCL;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class class2Load = findLoadedClass(name);

        if (class2Load != null) {
            if (resolve) {
                resolveClass(class2Load);
            }

            return class2Load;
        }

        // Loading it locally first
        // This means "search the shared classloaders first
        try {
            class2Load = findClass(name);
            if (class2Load != null) {
                if (resolve) {
                    resolveClass(class2Load);
                }

                return class2Load;
            }
        } catch (ClassNotFoundException cnfe) {
            // ignore
        }

        // Send it to the parent classloader
        try {
            class2Load = mParentLoader.loadClass(name);
            if (resolve) {
                resolveClass(class2Load);
            }
            return class2Load;
        } catch (ClassNotFoundException cnfe) {
            // ignore
        }

        //  All efforts are in vain - class was not found
        throw new ClassNotFoundException(name);
    }

    public Class findClass(String name) throws ClassNotFoundException {
        boolean found = false;
        Class sharedClass = null;

        for (ClassLoader classloader : delegates) {
            try {
                sharedClass = classloader.loadClass(name);

                // If we reach here, we've loaded the class
                // and can stop looking further.
                found = true;
                break;
            } catch (ClassNotFoundException cnfe) {
                continue;
            }
        }

        // if we reach here with found=false that means
        // that the requested class was not in the shared classpath
        if (!found) {
            throw new ClassNotFoundException(name);
        }

        return sharedClass;
    }

    @Override
    public String toString() {
        return "DelegatingClassLoader(" + getParent() + "," + delegates + "," + System.identityHashCode(this) + ")";
    }
}
