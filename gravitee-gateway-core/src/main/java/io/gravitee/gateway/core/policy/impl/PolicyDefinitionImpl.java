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
import io.gravitee.gateway.core.policy.PolicyDefinition;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyDefinitionImpl implements PolicyDefinition {

    private String id;
    private String name;
    private String description;
    private String version;
    private Class<? extends Policy> policy;
    private Class<? extends PolicyConfiguration> configuration;
    private List<URL> classPathElements = new ArrayList<>();

    @Override
    public String id() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public String description() {
        return null;
    }

    @Override
    public String version() {
        return null;
    }

    @Override
    public Class<? extends Policy> policy() {
        return null;
    }

    @Override
    public Class<? extends PolicyConfiguration> configuration() {
        return null;
    }

    @Override
    public List<URL> getClassPathElements() {
        return null;
    }

    @Override
    public Method onRequestMethod() {
        return null;
    }

    @Override
    public Method onResponseMethod() {
        return null;
    }

    public void setConfiguration(Class<? extends PolicyConfiguration> configuration) {
        this.configuration = configuration;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPolicy(Class<? extends Policy> policy) {
        this.policy = policy;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setClassPathElements(List<URL> classPathElements) {
        this.classPathElements = classPathElements;
    }
}
