/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { SubscriptionInfoComponent } from './subscription-info.component';
import { SubscriptionInfoHarness } from './subscription-info.harness';
import { Subscription } from '../../entities/subscription';

describe('SubscriptionInfoComponent', () => {
  let component: SubscriptionInfoComponent;
  let fixture: ComponentFixture<SubscriptionInfoComponent>;
  let harness: SubscriptionInfoHarness;

  const init = async (subscription: Subscription) => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionInfoComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionInfoComponent);
    component = fixture.componentInstance;
    component.subscription = subscription;
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionInfoHarness);
    fixture.detectChanges();
  };

  it('should create', async () => {
    await init({ status: 'ACCEPTED' } as Subscription);
    expect(component).toBeTruthy();
  });

  describe('closeSubscription', () => {
    it('should emit closeSubscription event when close button is clicked', async () => {
      await init({ status: 'ACCEPTED' } as Subscription);

      const spy = jest.spyOn(component.closeSubscription, 'emit');
      await harness.closeSubscription();

      expect(spy).toHaveBeenCalled();
    });

    it('should show close button when subscription status is ACCEPTED', async () => {
      await init({ status: 'ACCEPTED' } as Subscription);

      expect(await harness.getCloseButton()).toBeTruthy();
    });

    it('should show close button when subscription status is PAUSED', async () => {
      await init({ status: 'PAUSED' } as Subscription);

      expect(await harness.getCloseButton()).toBeTruthy();
    });

    it('should NOT show close button when subscription status is PENDING', async () => {
      await init({ status: 'PENDING' } as Subscription);

      expect(await harness.getCloseButton()).toBeFalsy();
    });

    it('should NOT show close button when subscription status is REJECTED', async () => {
      await init({ status: 'REJECTED' } as Subscription);

      expect(await harness.getCloseButton()).toBeFalsy();
    });

    it('should NOT show close button when subscription status is CLOSED', async () => {
      await init({ status: 'CLOSED' } as Subscription);

      expect(await harness.getCloseButton()).toBeFalsy();
    });
  });
});
