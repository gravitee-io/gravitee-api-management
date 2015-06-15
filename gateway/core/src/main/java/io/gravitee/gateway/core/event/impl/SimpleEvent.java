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
package io.gravitee.gateway.core.event.impl;

import io.gravitee.gateway.core.event.Event;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class SimpleEvent<T extends Enum, S> implements Event<T, S> {

  private final T type;
  private final S content;

  public SimpleEvent(T type, S content) {
    this.type = type;
    this.content = content;
  }

  @Override public S content() {
    return this.content;
  }

  @Override public T type() {
    return this.type;
  }
}
