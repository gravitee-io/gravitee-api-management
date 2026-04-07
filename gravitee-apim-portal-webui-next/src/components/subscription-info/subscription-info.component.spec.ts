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
import { provideRouter } from '@angular/router';

import { SubscriptionInfoComponent } from './subscription-info.component';
import { SubscriptionInfoHarness } from './subscription-info.harness';
import { Api } from '../../entities/api/api';
import { Subscription } from '../../entities/subscription';
import { ApiDocumentationNavigationTarget } from '../../services/portal-navigation-items.service';

describe('SubscriptionInfoComponent', () => {
  let component: SubscriptionInfoComponent;
  let fixture: ComponentFixture<SubscriptionInfoComponent>;
  let harness: SubscriptionInfoHarness;

  const init = async (
    subscription: Subscription,
    options?: {
      api?: Api;
      apiName?: string;
      documentationNavigationTarget?: ApiDocumentationNavigationTarget;
    },
  ) => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionInfoComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionInfoComponent);
    component = fixture.componentInstance;
    component.subscription = subscription;
    component.api = options?.api;
    component.apiName = options?.apiName;
    component.documentationNavigationTarget = options?.documentationNavigationTarget;
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionInfoHarness);
    fixture.detectChanges();
  };

  const getApiLink = (): HTMLAnchorElement | null => fixture.nativeElement.querySelector('[data-testid="subscription-api-link"]');
  const getApiLabel = (): HTMLSpanElement | null => fixture.nativeElement.querySelector('[data-testid="subscription-api-label"]');

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

  describe('api link visibility', () => {
    const api: Api = { id: 'api-id', name: 'My API' } as Api;
    const documentationNavigationTarget: ApiDocumentationNavigationTarget = { rootId: 'root-id', navItemId: 'nav-item-id' };

    it('should render API as clickable link when API details and documentation target are available', async () => {
      await init({ status: 'ACCEPTED' } as Subscription, { api, documentationNavigationTarget });

      expect(getApiLink()).toBeTruthy();
      expect(getApiLink()?.textContent?.trim()).toStrictEqual('My API');
      expect(getApiLink()?.getAttribute('href')).toContain('/documentation/root-id');
      expect(getApiLink()?.getAttribute('href')).toContain('selectedId=nav-item-id');
      expect(getApiLabel()).toBeNull();
    });

    it('should render API metadata name as plain text when documentation target is missing', async () => {
      await init({ status: 'ACCEPTED' } as Subscription, { api });

      expect(getApiLink()).toBeNull();
      expect(getApiLabel()).toBeTruthy();
      expect(getApiLabel()?.textContent?.trim()).toStrictEqual('My API');
    });

    it('should render API metadata name as plain text when api details are missing', async () => {
      await init({ status: 'ACCEPTED' } as Subscription, { apiName: 'API from metadata' });

      expect(getApiLink()).toBeNull();
      expect(getApiLabel()).toBeTruthy();
      expect(getApiLabel()?.textContent?.trim()).toStrictEqual('API from metadata');
    });

    it('should prefer api.name over apiName fallback', async () => {
      await init({ status: 'ACCEPTED' } as Subscription, { api, apiName: 'API from metadata', documentationNavigationTarget });

      expect(getApiLink()).toBeTruthy();
      expect(getApiLink()?.textContent?.trim()).toStrictEqual('My API');
      expect(getApiLabel()).toBeNull();
    });
  });
});
