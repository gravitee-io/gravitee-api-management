//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.gravitee.policy.fake;

import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.resource.api.ResourceManager;
import io.gravitee.resource.fake.api.FakeResource;
import io.reactivex.rxjava3.core.Completable;

public class FakePolicy implements Policy {

    public FakePolicy() {}

    public String id() {
        return "fake";
    }

    public Completable onRequest(HttpExecutionContext ctx) {
        FakeResource fakeResource = (FakeResource) ((ResourceManager) ctx.getComponent(ResourceManager.class)).getResource("my-fake");
        fakeResource.test();
        return Completable.complete();
    }
}
