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
package io.gravitee.gateway.core.policy.impl.properties;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface PolicyDescriptorProperties {

    String DESCRIPTOR_ID_PROPERTY = "id";
    String DESCRIPTOR_NAME_PROPERTY = "name";
    String DESCRIPTOR_VERSION_PROPERTY = "version";
    String DESCRIPTOR_DESCRIPTION_PROPERTY = "description";
    String DESCRIPTOR_CLASS_PROPERTY = "class";
}
