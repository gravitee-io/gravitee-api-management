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

import { AgentAccessCardComponent } from './agent-access-card.component';
import { AgentAccessCardComponentHarness } from './agent-access-card.harness';
import { PlanSecurityEnum } from '../../entities/plan/plan';
import { fakeSubscription, Subscription } from '../../entities/subscription';
import { AppTestingModule } from '../../testing/app-testing.module';

describe('AgentAccessCardComponent', () => {
  let fixture: ComponentFixture<AgentAccessCardComponent>;
  let harness: AgentAccessCardComponentHarness;

  const init = async (
    params: Partial<{
      planSecurity: PlanSecurityEnum;
      subscription: Subscription;
      applicationName: string;
      showApplicationName: boolean;
      entrypointUrl: string;
      apiKey: string;
      clientId: string;
      clientSecret: string;
      apiKeyHeader: string;
    }> = {},
  ) => {
    await TestBed.configureTestingModule({
      imports: [AgentAccessCardComponent, MatIconTestingModule, AppTestingModule],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(AgentAccessCardComponent);
    fixture.componentRef.setInput('planSecurity', params.planSecurity ?? 'API_KEY');
    fixture.componentRef.setInput('subscription', params.subscription ?? fakeSubscription({ id: 'sub-1', status: 'ACCEPTED' }));
    fixture.componentRef.setInput('applicationName', params.applicationName ?? 'My App');
    fixture.componentRef.setInput('showApplicationName', params.showApplicationName ?? false);
    fixture.componentRef.setInput('entrypointUrl', params.entrypointUrl ?? 'https://gw.test/agent');
    fixture.componentRef.setInput('apiKey', params.apiKey ?? 'live-key-1');
    fixture.componentRef.setInput('clientId', params.clientId ?? '');
    fixture.componentRef.setInput('clientSecret', params.clientSecret ?? '');
    fixture.componentRef.setInput('apiKeyHeader', params.apiKeyHeader ?? 'X-Gravitee-Api-Key');
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, AgentAccessCardComponentHarness);
    fixture.detectChanges();
  };

  it('shows the active API key, base URL and a cURL command that includes the key', async () => {
    await init({ apiKey: 'live-key-1', entrypointUrl: 'https://gw.test/agent' });

    expect(await harness.getApiKeyText()).toBe('live-key-1');
    expect(await harness.getBaseUrlText()).toBe('https://gw.test/agent');
    expect(await harness.getCurlText()).toBe('curl --header "X-Gravitee-Api-Key: live-key-1" https://gw.test/agent');
  });

  it('shows a pending status message and no credentials', async () => {
    await init({
      subscription: fakeSubscription({ id: 'sub-pending', status: 'PENDING' }),
      apiKey: 'hidden-key',
    });

    const text = await harness.getHostText();
    expect(text).toContain('Subscription in progress');
    expect(await harness.getApiKeyText()).toBeNull();
    expect(await harness.getCurlText()).toBeNull();
    expect(text).not.toContain('hidden-key');
  });

  it('shows a paused status message and no credentials', async () => {
    await init({
      subscription: fakeSubscription({ id: 'sub-paused', status: 'PAUSED' }),
      apiKey: 'hidden-key',
    });

    const text = await harness.getHostText();
    expect(text).toContain('Subscription paused');
    expect(await harness.getApiKeyText()).toBeNull();
  });

  it('hides the application name unless asked to show it', async () => {
    await init({ applicationName: 'Ops App', showApplicationName: false });

    expect(await harness.getApplicationNameText()).toBeNull();
  });

  it('shows the application name when asked', async () => {
    await init({ applicationName: 'Ops App', showApplicationName: true });

    expect(await harness.getApplicationNameText()).toBe('Ops App');
  });

  it('shows the client id of an OAUTH2 plan', async () => {
    await init({
      planSecurity: 'OAUTH2',
      apiKey: '',
      clientId: 'client-1',
      clientSecret: 'secret-1',
    });

    expect(await harness.getApiKeyText()).toBeNull();
    expect(await harness.getClientIdText()).toBe('client-1');
  });
});
