package io.gravity.gateway.http;

import java.util.Map;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface Request {

	String id();

	Map<String, ?> headers();
}
