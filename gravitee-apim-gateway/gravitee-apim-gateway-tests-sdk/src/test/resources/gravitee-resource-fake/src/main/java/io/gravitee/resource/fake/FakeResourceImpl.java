package io.gravitee.resource.fake;

import io.gravitee.resource.api.AbstractConfigurableResource;
import io.gravitee.resource.api.ResourceConfiguration;
import io.gravitee.resource.fake.api.FakeResource;

public class FakeResourceImpl extends AbstractConfigurableResource<ResourceConfiguration> implements FakeResource {

    public FakeResourceImpl() {}

    public void test() {
        System.out.println("This is a test from FakeResourceImpl");
    }
}
