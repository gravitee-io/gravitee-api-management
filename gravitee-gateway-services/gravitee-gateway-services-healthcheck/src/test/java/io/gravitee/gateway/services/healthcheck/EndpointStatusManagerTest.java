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
package io.gravitee.gateway.services.healthcheck;

import io.gravitee.definition.model.Endpoint;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class EndpointStatusManagerTest {

    @Test
    public void testEndpointStatus_noHealthCheckDone() {
        Endpoint endpoint = createEndpoint();
        Assert.assertEquals(Endpoint.Status.UP, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_up() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusManager manager = new EndpointStatusManager();

        manager.update(endpoint, true);
        Assert.assertEquals(Endpoint.Status.UP, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_transitionallyDown() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusManager manager = new EndpointStatusManager();

        manager.update(endpoint, true);
        manager.update(endpoint, false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_upAfterTransitionallyDown() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusManager manager = new EndpointStatusManager();

        manager.update(endpoint, true);
        manager.update(endpoint, false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());

        manager.update(endpoint, true);
        Assert.assertEquals(Endpoint.Status.UP, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_upAfterTransitionallyDownAndFinallyDown() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusManager manager = new EndpointStatusManager();

        manager.update(endpoint, false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());

        manager.update(endpoint, false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());

        manager.update(endpoint, false);
        Assert.assertEquals(Endpoint.Status.DOWN, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_down() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusManager manager = new EndpointStatusManager();

        manager.update(endpoint, false);
        manager.update(endpoint, false);
        manager.update(endpoint, false);
        Assert.assertEquals(Endpoint.Status.DOWN, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_downAfterTransitionallyUp() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusManager manager = new EndpointStatusManager();

        manager.update(endpoint, false);
        manager.update(endpoint, false);
        manager.update(endpoint, false);
        Assert.assertEquals(Endpoint.Status.DOWN, endpoint.getStatus());

        manager.update(endpoint, true);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_UP, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_downAfterTransitionallyUpAndFinallyUp() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusManager manager = new EndpointStatusManager();

        manager.update(endpoint, false);
        manager.update(endpoint, false);
        manager.update(endpoint, false);
        Assert.assertEquals(Endpoint.Status.DOWN, endpoint.getStatus());

        manager.update(endpoint, true);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_UP, endpoint.getStatus());

        manager.update(endpoint, true);
        Assert.assertEquals(Endpoint.Status.UP, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_scenario() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusManager manager = new EndpointStatusManager();

        Assert.assertEquals(Endpoint.Status.UP, endpoint.getStatus());

        manager.update(endpoint, false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());

        manager.update(endpoint, false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());

        manager.update(endpoint, false);
        Assert.assertEquals(Endpoint.Status.DOWN, endpoint.getStatus());

        manager.update(endpoint, true);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_UP, endpoint.getStatus());

        manager.update(endpoint, false);
        Assert.assertEquals(Endpoint.Status.DOWN, endpoint.getStatus());

        manager.update(endpoint, true);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_UP, endpoint.getStatus());

        manager.update(endpoint, true);
        Assert.assertEquals(Endpoint.Status.UP, endpoint.getStatus());

        manager.update(endpoint, false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());
    }

    private Endpoint createEndpoint() {
        Endpoint endpoint = new Endpoint("http://localhost:9099");
        endpoint.setName("default");

        return endpoint;
    }
}
