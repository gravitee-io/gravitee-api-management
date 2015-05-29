package io.gravity.gateway.api;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface AsyncHandler<T> {

	void handle(T result);
}
