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
package io.gravitee.rest.api.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import java.util.List;
import org.junit.Test;

public class SubscriptionQueryTest {

    @Test
    public void matchesApi_should_return_true_when_no_apis_criteria_is_set() {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApis(null);

        assertTrue(query.matchesApi("my-api"));
    }

    @Test
    public void matchesApi_should_return_true_when_api_matches_one_criteria() {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApis(List.of("his-api", "my-api", "their-api"));

        assertTrue(query.matchesApi("my-api"));
    }

    @Test
    public void matchesApi_should_return_false_when_api_matches_no_criteria() {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApis(List.of("his-api", "my-api", "their-api"));

        assertFalse(query.matchesApi("your-api"));
    }

    @Test
    public void matchesPlan_should_return_true_when_no_plans_criteria_is_set() {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setPlans(null);

        assertTrue(query.matchesPlan("my-plan"));
    }

    @Test
    public void matchesPlan_should_return_true_when_plan_matches_one_criteria() {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setPlans(List.of("his-plan", "my-plan", "their-plan"));

        assertTrue(query.matchesPlan("my-plan"));
    }

    @Test
    public void matchesPlan_should_return_false_when_plan_matches_no_criteria() {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setPlans(List.of("his-plan", "my-plan", "their-plan"));

        assertFalse(query.matchesPlan("your-plan"));
    }

    @Test
    public void matchesApplication_should_return_true_when_no_applications_criteria_is_set() {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApplications(null);

        assertTrue(query.matchesApplication("my-application"));
    }

    @Test
    public void matchesApplication_should_return_true_when_application_matches_one_criteria() {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApplications(List.of("his-application", "my-application", "their-application"));

        assertTrue(query.matchesApplication("my-application"));
    }

    @Test
    public void matchesApplication_should_return_false_when_application_matches_no_criteria() {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApplications(List.of("his-application", "my-application", "their-application"));

        assertFalse(query.matchesApplication("your-application"));
    }

    @Test
    public void matchesStatus_should_return_true_when_no_statuss_criteria_is_set() {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setStatuses(null);

        assertTrue(query.matchesStatus(SubscriptionStatus.PENDING));
    }

    @Test
    public void matchesStatus_should_return_true_when_status_matches_one_criteria() {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setStatuses(List.of(SubscriptionStatus.PENDING, SubscriptionStatus.ACCEPTED));

        assertTrue(query.matchesStatus(SubscriptionStatus.PENDING));
    }

    @Test
    public void matchesStatus_should_return_false_when_status_matches_no_criteria() {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setStatuses(List.of(SubscriptionStatus.PENDING, SubscriptionStatus.ACCEPTED));

        assertFalse(query.matchesStatus(SubscriptionStatus.REJECTED));
    }
}
