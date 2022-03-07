package io.gravitee.gateway.policy.place;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.policy.api.PolicyChain;

public class PlacedPolicy implements Policy {

    private Policy delegate;
    private String place;

    public PlacedPolicy(Policy delegate, String place) {
        this.delegate = delegate;
        this.place = place;
    }


    @Override
    public String id() {
        return delegate.id();
    }

    @Override
    public void execute(PolicyChain chain, ExecutionContext context) throws PolicyException {
        delegate.execute(chain, context);
    }

    @Override
    public ReadWriteStream<Buffer> stream(PolicyChain chain, ExecutionContext context) throws PolicyException {
        return delegate.stream(chain, context);
    }

    @Override
    public boolean isStreamable() {
        return delegate.isStreamable();
    }

    @Override
    public boolean isRunnable() {
        return delegate.isRunnable();
    }

    @Override
    public String place() {
        return place;
    }
}
