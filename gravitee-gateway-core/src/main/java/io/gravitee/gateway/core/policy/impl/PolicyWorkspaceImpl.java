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

import io.gravitee.gateway.api.policy.Policy;
import io.gravitee.gateway.api.policy.PolicyConfiguration;
import io.gravitee.gateway.api.policy.annotations.OnRequest;
import io.gravitee.gateway.api.policy.annotations.OnResponse;
import io.gravitee.gateway.core.policy.*;
import io.gravitee.gateway.core.policy.impl.properties.PolicyDescriptorProperties;
import io.gravitee.gateway.core.policy.impl.properties.PropertiesBasedPolicyDescriptorValidator;
import io.gravitee.gateway.core.utils.FileUtils;
import io.gravitee.gateway.core.utils.GlobMatchingFileVisitor;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyWorkspaceImpl implements PolicyWorkspace {

    protected final Logger LOGGER = LoggerFactory.getLogger(PolicyWorkspaceImpl.class);

    private final static String DESCRIPTOR_PROPERTIES_FILE = "policy.properties";

    private final static String JAR_EXTENSION = ".jar";
    private final static String JAR_GLOB = '*' + JAR_EXTENSION;

    private final Map<String, PolicyDefinition> definitions = new HashMap<>();

    private boolean initialized = false;

    @Value("${policy.workspace}")
    private String workspacePath;

    @Autowired
    private ClassLoaderFactory classLoaderFactory;

    /**
     * Empty constructor is used to defined workspace directory from @Value annotation on workspacePath field.
     */
    public PolicyWorkspaceImpl() {

    }

    public PolicyWorkspaceImpl(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    @Override
    public void init() {
        if (! initialized) {
            LOGGER.info("Initializing policy workspace.");
            this.initializeFromWorkspace();
            LOGGER.info("Initializing policy workspace. DONE");
        } else {
            LOGGER.warn("Policy workspace has already been initialized.");
        }
    }

    @Override
    public Collection<PolicyDefinition> getPolicyDefinitions() {
        return definitions.values();
    }

    @Override
    public PolicyDefinition getPolicyDefinition(String id) {
        return definitions.get(id);
    }

    public void initializeFromWorkspace() {
        if (workspacePath == null || workspacePath.isEmpty()) {
            LOGGER.error("Policy workspace directory is not valid.");
            throw new RuntimeException("Policy workspace directory is not valid.");
        }

        File workspaceDir = new File(workspacePath);

        // quick sanity check on the install root
        if (! workspaceDir.isDirectory()) {
            LOGGER.error("Invalid workspace directory, {} is not a directory.", workspaceDir.getAbsolutePath());
            throw new RuntimeException("Invalid workspace directory. Not a directory: "
                    + workspaceDir.getAbsolutePath());
        }

        LOGGER.info("Loading policies from {}", workspaceDir.getAbsoluteFile());
        List<File> subdirectories = getChildren(workspaceDir.getAbsolutePath());

        LOGGER.info("\t{} policy directories have been found.", subdirectories.size());
        for(File policyDir: subdirectories) {
            loadPolicy(policyDir.getAbsolutePath());
        }

        initialized = true;
    }

    /**
     * Load a policy from file system.
     *
     * Policy structure in the workspace is as follow:
     *  my-policy-dir/
     *      my-policy.jar
     *      lib/
     *          dependency01.jar
     *          dependency02.jar
     *      schemas/
     *          policy-schema.json
     *
     * @param policyDir The directory containing the policy definition
     */
    private void loadPolicy(String policyDir) {
        Path policyDirPath = FileSystems.getDefault().getPath(policyDir);
        LOGGER.info("Trying to load policy from {}", policyDirPath);

        PolicyDescriptor descriptor = readPolicyDescriptor(policyDirPath);
        if (descriptor != null) {
            // Prepare policy classloader by reading *.jar
            try {
                GlobMatchingFileVisitor visitor = new GlobMatchingFileVisitor(JAR_GLOB);
                Files.walkFileTree(policyDirPath, visitor);
                List<Path> policyDependencies = visitor.getMatchedPaths();
                URL[] dependencies = listToArray(policyDependencies);
                classLoaderFactory.createPolicyClassLoader(descriptor.id(), dependencies);

                PolicyDefinition definition = getPolicyDefinition(descriptor);

                definitions.put(descriptor.id(), definition);
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while looking for the policy dependencies", ioe);
            }
        }
    }

    private PolicyDefinition getPolicyDefinition(PolicyDescriptor descriptor) {
        try {
            final Class<?> instanceClass =
                    ClassUtils.forName(descriptor.policy(),
                            classLoaderFactory.getPolicyClassLoader(descriptor.id()));

            final Method onRequestMethod = resolvePolicyMethod(instanceClass, OnRequest.class);
            final Method onResponseMethod = resolvePolicyMethod(instanceClass, OnResponse.class);

            if (onRequestMethod == null && onResponseMethod == null) {
                LOGGER.error("No method annotated with @OnRequest or @OnResponse found, skip policy registration for {}", instanceClass.getName());
            } else {

                return new PolicyDefinition() {
                    @Override
                    public String id() {
                        return descriptor.id();
                    }

                    @Override
                    public String name() {
                        return descriptor.name();
                    }

                    @Override
                    public String description() {
                        return descriptor.description();
                    }

                    @Override
                    public String version() {
                        return descriptor.version();
                    }

                    @Override
                    public Class<Policy> policy() {
                        return (Class<Policy>) instanceClass;
                    }

                    @Override
                    public Class<PolicyConfiguration> configuration() {
                        return null;
                    }

                    @Override
                    public List<URL> getClassPathElements() {
                        return null;
                    }

                    @Override
                    public Method onRequestMethod() {
                        return onRequestMethod;
                    }

                    @Override
                    public Method onResponseMethod() {
                        return onResponseMethod;
                    }
                };
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate Policy: " + descriptor.policy(), e);
        }

        return null;
    }

    /**
     *
     * @param policyPath
     * @return
     */
    private PolicyDescriptor readPolicyDescriptor(Path policyPath) {
        try {
            Iterator iterator = FileUtils.newDirectoryStream(policyPath, JAR_GLOB).iterator();
            if (! iterator.hasNext()) {
                LOGGER.debug("Unable to found a jar in the root directory: {}", policyPath);
                return null;
            }

            Path policyJarPath = (Path) iterator.next();
            LOGGER.debug("Found a jar in the root directory, looking for a policy descriptor in: {}", policyJarPath);

            Properties policyDescriptorProperties = loadPolicyDescriptor(policyJarPath.toString());
            if (policyDescriptorProperties == null) {
                LOGGER.error("No policy.properties can be found from {}", policyJarPath);
                return null;
            }

            LOGGER.info("A policy descriptor has been loaded from: {}", policyJarPath);

            PolicyDescriptorValidator validator = new PropertiesBasedPolicyDescriptorValidator(policyDescriptorProperties);
            if (! validator.validate()) {
                LOGGER.error("Policy descriptor not valid, skipping policy registration.");
                return null;
            }

            return create(policyDescriptorProperties);
        } catch (IOException ioe) {
            LOGGER.error("Unexpected error while trying to load policy descriptor", ioe);
            return null;
        }
    }

    private PolicyDescriptor create(Properties properties) {
        final String id = properties.getProperty(PolicyDescriptorProperties.DESCRIPTOR_ID_PROPERTY);
        final String description = properties.getProperty(PolicyDescriptorProperties.DESCRIPTOR_DESCRIPTION_PROPERTY);
        final String clazz = properties.getProperty(PolicyDescriptorProperties.DESCRIPTOR_CLASS_PROPERTY);
        final String name = properties.getProperty(PolicyDescriptorProperties.DESCRIPTOR_NAME_PROPERTY);
        final String version = properties.getProperty(PolicyDescriptorProperties.DESCRIPTOR_VERSION_PROPERTY);

        return new PolicyDescriptor() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public String version() {
                return version;
            }

            @Override
            public String policy() {
                return clazz;
            }
        };
    }

    private Properties loadPolicyDescriptor(String policyPath) {
        try (FileSystem zipFileSystem = FileUtils.createZipFileSystem(policyPath, false)){
            final Path root = zipFileSystem.getPath("/");

            // Walk the jar file tree and search for policy.properties file
            PolicyDescriptorVisitor visitor = new PolicyDescriptorVisitor();
            Files.walkFileTree(root, visitor);
            Path policyDescriptorPath = visitor.getPolicyDescriptor();

            if (policyDescriptorPath != null) {
                Properties properties = new Properties();
                properties.load(Files.newInputStream(policyDescriptorPath));

                return properties;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Method resolvePolicyMethod(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        Set<Method> methods = ReflectionUtils.getMethods(
                clazz,
                withModifier(Modifier.PUBLIC),
                withAnnotation(annotationClass));

        if (methods.isEmpty()) {
            return null;
        }

        return methods.iterator().next();
    }

    private List<File> getChildren(String directory) {
        DirectoryStream.Filter<Path> filter = file -> (Files.isDirectory(file));

        List<File> files = new ArrayList<>();
        Path dir = FileSystems.getDefault().getPath(directory);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir,
                filter)) {
            for (Path path : stream) {
                files.add(path.toFile());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return files;
    }

    private URL[] listToArray(List<Path> paths) {
        URL [] urls = new URL[paths.size()];
        int idx = 0;

        for(Path path: paths) {
            try {
                urls[idx++] = path.toUri().toURL();
            } catch (IOException ioe) {}
        }

        return urls;
    }

    class PolicyDescriptorVisitor extends SimpleFileVisitor<Path> {
        private Path policyDescriptor = null;

        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attrs) throws IOException {
            if (file.getFileName().toString().equals(DESCRIPTOR_PROPERTIES_FILE)) {
                policyDescriptor = file;
                return FileVisitResult.TERMINATE;
            }

            return super.visitFile(file, attrs);
        }

        public Path getPolicyDescriptor() {
            return policyDescriptor;
        }
    }

    public void setClassLoaderFactory(ClassLoaderFactory classLoaderFactory) {
        this.classLoaderFactory = classLoaderFactory;
    }
}
