package io.gravitee.management.repository.proxy;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public abstract class AbstractProxy<T> {

    protected T target;

    public void setTarget(T target) {
        this.target = target;
    }
}
