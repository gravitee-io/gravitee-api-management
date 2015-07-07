package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.Policy;
import io.gravitee.gateway.api.PolicyChain;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DummyPolicy implements Policy {

    @Override
    public void onRequest(Request request, Response response, PolicyChain handler) {
        // Do nothing
    }

    @Override
    public void onResponse(Request request, Response response, PolicyChain handler) {
        // Do nothing
    }
}
