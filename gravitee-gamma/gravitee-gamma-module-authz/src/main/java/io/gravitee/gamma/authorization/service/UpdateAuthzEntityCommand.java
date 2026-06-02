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
package io.gravitee.gamma.authorization.service;

import java.util.List;
import java.util.Map;

/**
 * Patch for an existing authz entity. Only {@code attributes} and {@code parents} are mutable.
 *
 * <p>{@code kind}, {@code source}, and {@code entityType} are set once at upsert and immutable
 * on update — they form part of the entity's identity in the engine snapshot
 * ({@code <entityType>::"<entityId>"}). Changing them mid-life would silently break policies
 * that reference the old {@code Type::"id"}.
 */
public record UpdateAuthzEntityCommand(Map<String, Object> attributes, List<String> parents) {}
