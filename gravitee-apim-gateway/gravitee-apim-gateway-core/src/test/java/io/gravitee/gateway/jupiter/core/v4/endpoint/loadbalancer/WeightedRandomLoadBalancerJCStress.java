/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.core.v4.endpoint.loadbalancer;

import static org.mockito.Mockito.mock;

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.gateway.jupiter.core.v4.endpoint.ManagedEndpoint;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.openjdk.jcstress.Main;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IIII_Result;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WeightedRandomLoadBalancerJCStress {

    public static void main(String[] args) throws Exception {
        String[] jcstress = { "-v", "-t", "WeightedRandomLoadBalancerJCStressTest" };
        Main.main(jcstress);
    }

    @JCStressTest
    @Outcome(id = "1, 1, 1, 1", expect = Expect.ACCEPTABLE, desc = "Good")
    @Outcome(id = "1, 1, 2, 2", expect = Expect.ACCEPTABLE, desc = "Good")
    @Outcome(id = "2, 2, 1, 1", expect = Expect.ACCEPTABLE, desc = "Good")
    @Outcome(expect = Expect.FORBIDDEN, desc = "Oups")
    @State
    public static class ShouldRefreshBeCompliant {

        private final List<ManagedEndpoint> endpoints;
        private final WeightedRandomLoadBalancer loadBalancer;

        public ShouldRefreshBeCompliant() {
            this.endpoints = new CopyOnWriteArrayList<>();
            this.loadBalancer = new WeightedRandomLoadBalancer(endpoints);
        }

        @Actor
        public void actor1(IIII_Result r) {
            ManagedEndpoint managedEndpoint = new ManagedEndpoint(new Endpoint(), null, null);
            endpoints.add(managedEndpoint);
            loadBalancer.refresh();
            r.r1 = loadBalancer.weightDistributions.get().getWeightSum();
            r.r2 = loadBalancer.weightDistributions.get().getRemainingSum();
        }

        @Actor
        public void actor2(IIII_Result r) {
            ManagedEndpoint managedEndpoint = new ManagedEndpoint(new Endpoint(), null, null);
            endpoints.add(managedEndpoint);
            loadBalancer.refresh();
            r.r3 = loadBalancer.weightDistributions.get().getWeightSum();
            r.r4 = loadBalancer.weightDistributions.get().getRemainingSum();
        }
    }

    @JCStressTest
    @Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "Good")
    @Outcome(id = "0, 1", expect = Expect.ACCEPTABLE, desc = "Good")
    @Outcome(id = "1, 0", expect = Expect.ACCEPTABLE, desc = "Good")
    @Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "Good")
    @Outcome(expect = Expect.FORBIDDEN, desc = "Oups")
    @State
    public static class ShouldNextBeCompliant {

        private final List<ManagedEndpoint> endpoints;
        private final WeightedRandomLoadBalancer loadBalancer;

        public ShouldNextBeCompliant() {
            this.endpoints = new CopyOnWriteArrayList<>();
            ManagedEndpoint managedEndpoint1 = new ManagedEndpoint(new Endpoint(), null, null);
            endpoints.add(managedEndpoint1);
            ManagedEndpoint managedEndpoint2 = new ManagedEndpoint(new Endpoint(), null, null);
            endpoints.add(managedEndpoint2);
            this.loadBalancer = new WeightedRandomLoadBalancer(endpoints);
        }

        @Actor
        public void actor1(II_Result r) {
            loadBalancer.next();
            r.r1 = loadBalancer.weightDistributions.get().getRemainingSum();
        }

        @Actor
        public void actor2(II_Result r) {
            loadBalancer.next();
            r.r2 = loadBalancer.weightDistributions.get().getRemainingSum();
        }
    }
}
