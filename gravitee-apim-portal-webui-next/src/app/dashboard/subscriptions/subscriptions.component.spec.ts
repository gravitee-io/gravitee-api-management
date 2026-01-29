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
import { SubscriptionConsumerStatusEnum } from '../../../entities/subscription';
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
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  async function initHarness() {
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionsComponentHarness);
  }

  it('should create', async () => {
    fixture.detectChanges();
    expectApplicationsList();
    expectSubscriptionsList({ data: [], links: { self: '' }, metadata: {} });
    await initHarness();

    expect(fixture.componentInstance).toBeTruthy();
    expect(await harness.host()).toBeTruthy();
  });

  it('should display empty state when no subscriptions', async () => {
    fixture.detectChanges();
    expectApplicationsList();
    expectSubscriptionsList({ data: [], links: { self: '' }, metadata: {} });
    await fixture.whenStable();
    await initHarness();

    expect(await harness.isEmptyStateDisplayed()).toBeTruthy();
  });

  it('should display table when subscriptions exist', async () => {
    fixture.detectChanges();
    expectApplicationsList();
    expectSubscriptionsList(fakeSubscriptionResponse());
    await fixture.whenStable();
    await initHarness();

    expect(await harness.isEmptyStateDisplayed()).toBeFalsy();
  });

  it('should parse rows correctly from response', async () => {
    const response: SubscriptionsResponse = {
      data: [
        {
          id: 'sub-1',
          api: 'api-1',
          application: 'app-1',
          plan: 'plan-1',
          status: 'ACCEPTED',
          created_at: '2026-02-03T23:00:00Z',
          consumerStatus: SubscriptionConsumerStatusEnum.STARTED,
        },
      ],
      metadata: {
        'api-1': { name: 'API One', apiVersion: '1' },
        'app-1': { name: 'App One' },
        'plan-1': { name: 'Plan One' },
      },
      links: { self: '' },
    };

    fixture.detectChanges();
    expectApplicationsList();
    expectSubscriptionsList(response);
    await initHarness();

    expect(fixture.componentInstance.rows().length).toBe(1);
    expect(fixture.componentInstance.rows()[0]).toEqual({
      id: 'sub-1',
      api: 'API One',
      plan: 'Plan One',
      application: 'App One',
      created_at: '2026-02-03T23:00:00Z',
      status: 'Active',
    });
  });

  function expectSubscriptionsList(response: SubscriptionsResponse) {
    httpTestingController.expectOne(req => req.url.includes('/subscriptions')).flush(response);
  }

  function expectApplicationsList() {
    const applicationsResponse: ApplicationsResponse = { data: [] };
    httpTestingController.expectOne(req => req.url.includes('/applications')).flush(applicationsResponse);
  }
});
