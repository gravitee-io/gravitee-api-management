package io.gravitee.common.component;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface LifecycleComponent<T> {

  Lifecycle.State lifecycleState();

  T start() throws Exception;

  T stop() throws Exception;
}
