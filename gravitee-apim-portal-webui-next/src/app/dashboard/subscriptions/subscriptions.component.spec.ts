/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import SubscriptionsComponent from './subscriptions.component';
import { SubscriptionsComponentHarness } from './subscriptions.component.harness';
import { ApplicationsResponse } from '../../../entities/application/application';
import { fakeSubscriptionResponse } from '../../../entities/subscription/subscription.fixture';
import { SubscriptionsResponse } from '../../../entities/subscription/subscriptions-response';

describe('SubscriptionsComponent', () => {
  let fixture: ComponentFixture<SubscriptionsComponent>;
  let harness: SubscriptionsComponentHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionsComponentHarness);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create', async () => {
    expectApplicationsList();
    expectSubscriptionsList({ data: [], links: { self: '' }, metadata: {} });
    fixture.detectChanges();

    expect(await harness.host()).toBeTruthy();
  });

  it('should display empty state when no subscriptions', async () => {
    expectApplicationsList();
    expectSubscriptionsList({ data: [], links: { self: '' }, metadata: {} });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(await harness.isEmptyStateDisplayed()).toBeTruthy();
  });

  it('should display table when subscriptions exist', async () => {
    expectApplicationsList();
    expectSubscriptionsList(fakeSubscriptionResponse());
    fixture.detectChanges();
    await fixture.whenStable();

    expect(await harness.isEmptyStateDisplayed()).toBeFalsy();
  });

  function expectSubscriptionsList(response: SubscriptionsResponse) {
    httpTestingController.expectOne(req => req.url.includes('/subscriptions')).flush(response);
  }

  function expectApplicationsList() {
    const applicationsResponse: ApplicationsResponse = { data: [] };
    httpTestingController.expectOne(req => req.url.includes('/applications')).flush(applicationsResponse);
  }
});
