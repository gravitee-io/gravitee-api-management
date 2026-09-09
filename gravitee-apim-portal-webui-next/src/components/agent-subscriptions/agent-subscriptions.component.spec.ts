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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AgentSubscriptionsComponent } from './agent-subscriptions.component';
import { AgentSubscriptionsComponentHarness } from './agent-subscriptions.harness';
import { PlanSecurityEnum } from '../../entities/plan/plan';
import { fakeSubscription } from '../../entities/subscription';
import {
  AgentSubscriptionAccessContext,
  AgentSubscriptionService,
  AgentSubscriptionSummary,
} from '../../services/agent-subscription.service';
import { ConfigService } from '../../services/config.service';
import { AppTestingModule, ConfigServiceStub } from '../../testing/app-testing.module';

describe('AgentSubscriptionsComponent', () => {
  let fixture: ComponentFixture<AgentSubscriptionsComponent>;
  let harness: AgentSubscriptionsComponentHarness;
  let loadAccessContexts: jest.Mock;

  const aSummary = (
    opts: {
      id?: string;
      status?: 'ACCEPTED' | 'PENDING' | 'PAUSED';
      applicationName?: string;
      planName?: string;
      planSecurity?: PlanSecurityEnum;
      key?: string;
    } = {},
  ): AgentSubscriptionSummary => {
    const key = opts.key ?? 'key-1';
    return {
      subscription: fakeSubscription({
        id: opts.id ?? 'sub-1',
        status: opts.status ?? 'ACCEPTED',
        keys: opts.status && opts.status !== 'ACCEPTED' ? [] : [{ id: `${key}-id`, key, application: { id: 'app-1', name: 'My App' } }],
      }),
      planName: opts.planName ?? 'Gold',
      planSecurity: opts.planSecurity ?? 'API_KEY',
      applicationName: opts.applicationName ?? 'My App',
    };
  };

  const init = async (
    params: Partial<{
      agentName: string;
      entrypointUrls: string[];
      subscriptions: AgentSubscriptionSummary[];
      contexts: Map<string, AgentSubscriptionAccessContext>;
    }> = {},
  ) => {
    loadAccessContexts = jest.fn().mockReturnValue(of(params.contexts ?? new Map()));
    const config = new ConfigServiceStub();
    config.configuration = { portal: { apikeyHeader: 'X-Gravitee-Api-Key' } };

    await TestBed.configureTestingModule({
      imports: [AgentSubscriptionsComponent, MatIconTestingModule, AppTestingModule],
      providers: [
        provideRouter([]),
        { provide: AgentSubscriptionService, useValue: { loadAccessContexts } },
        { provide: ConfigService, useValue: config },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AgentSubscriptionsComponent);
    fixture.componentRef.setInput('agentName', params.agentName ?? 'Incident Commander');
    fixture.componentRef.setInput('entrypointUrls', params.entrypointUrls ?? ['https://gw.test/agent']);
    fixture.componentRef.setInput('subscriptions', params.subscriptions ?? [aSummary()]);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, AgentSubscriptionsComponentHarness);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  };

  it('renders one accordion item per subscription, in listing order', async () => {
    await init({
      subscriptions: [aSummary({ id: 'sub-1', planName: 'Gold' }), aSummary({ id: 'sub-2', planName: 'Silver', key: 'key-2' })],
    });

    expect(await harness.getPanelCount()).toBe(2);
    expect(await harness.getHeaderTexts()).toEqual([
      expect.objectContaining({ description: 'Gold - Accepted' }),
      expect.objectContaining({ description: 'Silver - Accepted' }),
    ]);
  });

  it('shows the plan name and capitalized status in the header', async () => {
    await init({
      subscriptions: [aSummary({ planName: 'Gold', status: 'ACCEPTED' })],
    });

    expect(await harness.getHeaderTexts()).toEqual([expect.objectContaining({ description: 'Gold - Accepted' })]);
  });

  it('hides the application name when there is only one subscription', async () => {
    await init({
      subscriptions: [aSummary({ applicationName: 'Ops App' })],
    });

    expect(await harness.getApplicationNameAt(0)).toBeNull();
  });

  it('shows the application name when there are multiple subscriptions', async () => {
    await init({
      subscriptions: [
        aSummary({ id: 'sub-1', applicationName: 'First App' }),
        aSummary({ id: 'sub-2', applicationName: 'Second App', key: 'key-2' }),
      ],
    });

    expect(await harness.getApplicationNameAt(0)).toBe('First App');
    await harness.expandAt(1);
    expect(await harness.getApplicationNameAt(1)).toBe('Second App');
  });

  it('shows the API key of an accepted subscription', async () => {
    await init({
      subscriptions: [aSummary({ status: 'ACCEPTED', key: 'live-key-1' })],
    });

    expect(await harness.getAccessTextAt(0)).toContain('live-key-1');
  });

  it('shows a pending message and no key for a pending subscription', async () => {
    await init({
      subscriptions: [aSummary({ id: 'sub-pending', status: 'PENDING', key: 'hidden-key' })],
    });

    const text = await harness.getAccessTextAt(0);
    expect(text).toContain('Subscription in progress');
    expect(text).not.toContain('hidden-key');
  });

  it('emits newSubscription when New subscription is clicked', async () => {
    await init();
    const newSubscription = jest.fn();
    fixture.componentInstance.newSubscription.subscribe(newSubscription);

    await harness.clickNewSubscription();

    expect(newSubscription).toHaveBeenCalled();
  });

  it('renders the client id of an OAUTH2 plan from the loaded context', async () => {
    const oauth = aSummary({ id: 'sub-oauth', planSecurity: 'OAUTH2' });
    await init({
      subscriptions: [oauth],
      contexts: new Map([['sub-oauth', { clientId: 'client-1', clientSecret: 'secret-1' }]]),
    });

    expect(await harness.getClientIdAt(0)).toBe('client-1');
  });

  it('links to the full subscription details page', async () => {
    await init({
      subscriptions: [aSummary({ id: 'sub-42' })],
    });

    expect(await harness.getDetailsHrefAt(0)).toBe('/dashboard/subscriptions/sub-42');
  });
});
