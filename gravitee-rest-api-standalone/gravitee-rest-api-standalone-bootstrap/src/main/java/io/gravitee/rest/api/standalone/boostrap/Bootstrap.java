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
package io.gravitee.rest.api.standalone.boostrap;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Bootstrap {

    private static final String GRAVITEE_HOME_PROP = "gravitee.home";
    private static final String CONTAINER_CLASS = "io.gravitee.rest.api.standalone.GraviteeApisContainer";

    private static final String LIB_DIRECTORY = "lib";
    private static final String LIB_EXT_DIRECTORY = LIB_DIRECTORY + File.separatorChar + "ext";

    private ClassLoader graviteeClassLoader;
    private ClassLoader extensionClassLoader;

    /**
     * Daemon reference
     */
    private Object graviteeDaemon = null;

    private Bootstrap() {
    }

    public void init() throws Exception {
        setGraviteeHome();
        initClassLoaders();

        // Set the thread context classloader to the framework classloader
        Thread.currentThread().setContextClassLoader(graviteeClassLoader);

        Class<?> fwClass = graviteeClassLoader.loadClass(CONTAINER_CLASS);
        graviteeDaemon = fwClass.newInstance();
    }

    private void initClassLoaders() {
        createExtensionClassLoader();
        createGraviteeClassLoader();
    }

    private void createGraviteeClassLoader() {
        ArrayList<URL> cpList = new ArrayList<>();
        URL[] cpURLs = new URL[0];
        File libDir = new File(
                System.getProperty(GRAVITEE_HOME_PROP), LIB_DIRECTORY);

        // Everything in the lib directory goes into the classpath
        for (File lib : libDir.listFiles()) {
            try {
                cpList.add(lib.toURI().toURL());
            } catch (java.net.MalformedURLException urlEx) {
                urlEx.printStackTrace();
            }
        }

        cpURLs = cpList.toArray(cpURLs);
        graviteeClassLoader = new URLClassLoader(
                cpURLs, extensionClassLoader);
    }

    private void createExtensionClassLoader() {
        List<URL> cpList = new ArrayList<>();
        URL[] cpURLs = new URL[0];
        File libDir = new File(
                System.getProperty(GRAVITEE_HOME_PROP), LIB_EXT_DIRECTORY);

        if (libDir.exists() || libDir.isDirectory()) {
            try {
                // Add the top-level ext directory
                cpList.add(libDir.toURI().toURL());

                // Everything in the lib/ext directory goes into the classpath
                for (File lib : libDir.listFiles()) {
                    cpList.add(lib.toURI().toURL());
                }
            } catch (java.net.MalformedURLException urlEx) {
                urlEx.printStackTrace();
            }
        }

        cpURLs = cpList.toArray(cpURLs);
        extensionClassLoader = new URLClassLoader(
                cpURLs, getClass().getClassLoader());
    }

    /**
     * Set the
     * <code>gravitee.home</code> System property to the current working
     * directory if it has not been set.
     */
    private void setGraviteeHome() {
        String installPath = System.getProperty(GRAVITEE_HOME_PROP);
        if (installPath == null) {
            File installDir = new File(System.getProperty("user.dir"));

            if (LIB_DIRECTORY.equals(installDir.getName())) {
                installDir = installDir.getParentFile();
            }

            installPath = installDir.getAbsolutePath();
        }

        File graviteeHomeDir = new File(installPath);
        checkInstallRoot(graviteeHomeDir);

        // pass this information along to the core framework
        System.setProperty(GRAVITEE_HOME_PROP,
                graviteeHomeDir.getAbsolutePath());
    }

    private void checkInstallRoot(File graviteeHomeDir) {
        // quick sanity check on the install root
        if (!graviteeHomeDir.isDirectory()) {
            throw new RuntimeException("Invalid Gravitee.io Node home directory. Not a directory: "
                    + graviteeHomeDir.getAbsolutePath());
        }


        File graviteeLibDir = new File(graviteeHomeDir, LIB_DIRECTORY);

        File [] files = graviteeLibDir.listFiles(pathname ->
                pathname.getName().startsWith("gravitee-rest-api-standalone-bootstrap"));

        if (files == null || files.length == 0 || files.length > 1) {
            throw new RuntimeException("Invalid Gravitee.io Node home directory. No bootstrapable jar can be found in "
                    + graviteeLibDir.getAbsolutePath());
        }
    }

    /**
     * Start the Gravitee Standalone daemon.
     */
    public void start() throws Exception {
        if (graviteeDaemon == null) {
            init();
        }

        Method method = graviteeDaemon.getClass().getMethod("start", (Class[]) null);
        method.invoke(graviteeDaemon, (Object[]) null);
    }

    public static void main(String[] args) {
        Thread mainThread = Thread.currentThread();
        mainThread.setName("graviteeio-node");

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.start();
        } catch (Exception t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
}
