package io.gravitee.gateway.core;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface AsyncHandler<T> {

	void handle(T result);
}
