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
package io.gravitee.common.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class AbstractLifecycleComponent<T> implements LifecycleComponent<T> {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected final Lifecycle lifecycle = new Lifecycle();

  @Override
  public Lifecycle.State lifecycleState() {
    return this.lifecycle.state();
  }

  @Override
  public T start() throws Exception {
    doStart();
    lifecycle.moveToStarted();

    return (T) this;
  }

  @Override
  public T stop() throws Exception {
    lifecycle.moveToStopped();
    doStop();

    return (T) this;
  }

  protected abstract void doStart() throws Exception;

  protected abstract void doStop() throws Exception;
}
