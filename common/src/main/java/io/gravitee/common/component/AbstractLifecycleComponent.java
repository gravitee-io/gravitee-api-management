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
