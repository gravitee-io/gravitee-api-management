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
import { ComponentFixture, TestBed } from '@angular/core/testing';

import SubscriptionsComponent from './subscriptions.component';
import { SubscriptionsComponentHarness } from './subscriptions.component.harness';

describe('SubscriptionsComponent', () => {
  let fixture: ComponentFixture<SubscriptionsComponent>;
  let harness: SubscriptionsComponentHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionsComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionsComponentHarness);
    fixture.detectChanges();
  });

  it('should create', async () => {
    expect(await harness.host()).toBeTruthy();
  });

  it('should display empty state', async () => {
    expect(await harness.isEmptyStateDisplayed()).toBeTruthy();
  });
});
