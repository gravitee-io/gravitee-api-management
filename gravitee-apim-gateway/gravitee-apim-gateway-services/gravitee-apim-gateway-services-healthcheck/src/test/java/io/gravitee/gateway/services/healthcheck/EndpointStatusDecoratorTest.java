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
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointStatusDecoratorTest {

    @Test
    public void testEndpointStatus_noHealthCheckDone() {
        Endpoint endpoint = createEndpoint();
        Assert.assertEquals(Endpoint.Status.UP, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_up() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusDecorator manager = new EndpointStatusDecorator(endpoint);

        manager.updateStatus(true);
        Assert.assertEquals(Endpoint.Status.UP, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_transitionallyDown() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusDecorator manager = new EndpointStatusDecorator(endpoint);

        manager.updateStatus(true);
        manager.updateStatus(false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_upAfterTransitionallyDown() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusDecorator manager = new EndpointStatusDecorator(endpoint);

        manager.updateStatus(true);
        manager.updateStatus(false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());

        manager.updateStatus(true);
        Assert.assertEquals(Endpoint.Status.UP, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_upAfterTransitionallyDownAndFinallyDown() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusDecorator manager = new EndpointStatusDecorator(endpoint);

        manager.updateStatus(false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());

        manager.updateStatus(false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());

        manager.updateStatus(false);
        Assert.assertEquals(Endpoint.Status.DOWN, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_down() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusDecorator manager = new EndpointStatusDecorator(endpoint);

        manager.updateStatus(false);
        manager.updateStatus(false);
        manager.updateStatus(false);
        Assert.assertEquals(Endpoint.Status.DOWN, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_downAfterTransitionallyUp() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusDecorator manager = new EndpointStatusDecorator(endpoint);

        manager.updateStatus(false);
        manager.updateStatus(false);
        manager.updateStatus(false);
        Assert.assertEquals(Endpoint.Status.DOWN, endpoint.getStatus());

        manager.updateStatus(true);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_UP, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_downAfterTransitionallyUpAndFinallyUp() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusDecorator manager = new EndpointStatusDecorator(endpoint);

        manager.updateStatus(false);
        manager.updateStatus(false);
        manager.updateStatus(false);
        Assert.assertEquals(Endpoint.Status.DOWN, endpoint.getStatus());

        manager.updateStatus(true);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_UP, endpoint.getStatus());

        manager.updateStatus(true);
        Assert.assertEquals(Endpoint.Status.UP, endpoint.getStatus());
    }

    @Test
    public void testEndpointStatus_scenario() {
        Endpoint endpoint = createEndpoint();
        EndpointStatusDecorator manager = new EndpointStatusDecorator(endpoint);

        Assert.assertEquals(Endpoint.Status.UP, endpoint.getStatus());

        manager.updateStatus(false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());

        manager.updateStatus(false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());

        manager.updateStatus(false);
        Assert.assertEquals(Endpoint.Status.DOWN, endpoint.getStatus());

        manager.updateStatus(true);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_UP, endpoint.getStatus());

        manager.updateStatus(false);
        Assert.assertEquals(Endpoint.Status.DOWN, endpoint.getStatus());

        manager.updateStatus(true);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_UP, endpoint.getStatus());

        manager.updateStatus(true);
        Assert.assertEquals(Endpoint.Status.UP, endpoint.getStatus());

        manager.updateStatus(false);
        Assert.assertEquals(Endpoint.Status.TRANSITIONALLY_DOWN, endpoint.getStatus());
    }

    private Endpoint createEndpoint() {
        return new HttpEndpoint("default", "http://localhost:9099");
    }
}
