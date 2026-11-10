/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.plugin.gamma.api.credential;

/**
 * One object that references a credential.
 *
 * @param type the kind of object, as the module names it, e.g. {@code LLM_PROXY} or {@code AI_WORKSPACE}
 * @param id the id of the object in its module
 * @param name the name a user knows the object by
 */
public record CredentialConsumer(String type, String id, String name) {}
