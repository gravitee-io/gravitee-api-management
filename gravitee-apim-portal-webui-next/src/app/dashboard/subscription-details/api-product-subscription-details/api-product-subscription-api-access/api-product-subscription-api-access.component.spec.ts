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
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { ApiProductSubscriptionApiAccessComponent } from './api-product-subscription-api-access.component';
import { ApiProductSubscriptionApiAccessHarness } from './api-product-subscription-api-access.harness';
import { ApiProductSubscriptionApi } from '../../../../../entities/subscription';
import { ConfigService } from '../../../../../services/config.service';
import { AppTestingModule, ConfigServiceStub } from '../../../../../testing/app-testing.module';

describe('ApiProductSubscriptionApiAccessComponent', () => {
  let fixture: ComponentFixture<ApiProductSubscriptionApiAccessComponent>;
  let configService: ConfigServiceStub;

  const api: ApiProductSubscriptionApi = {
    id: 'api-id',
    name: 'Orders API',
    version: '1.0',
    type: 'PROXY',
    availability: 'AVAILABLE',
    entrypoints: ['https://api.example.com/orders'],
    documentation: { rootId: 'root-id', navigationItemId: 'navigation-item-id' },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiProductSubscriptionApiAccessComponent, AppTestingModule],
      providers: [provideNoopAnimations(), provideRouter([])],
    }).compileComponents();

    configService = TestBed.inject(ConfigService) as unknown as ConfigServiceStub;
    configService.configuration = { portal: { apikeyHeader: 'X-Gravitee-Api-Key' } };
    fixture = TestBed.createComponent(ApiProductSubscriptionApiAccessComponent);
    fixture.componentRef.setInput('api', api);
    fixture.componentRef.setInput('accessEnabled', true);
  });

  it('should show the base URL, cURL command and documentation link for an available API', async () => {
    fixture.componentRef.setInput('planSecurity', 'API_KEY');
    fixture.componentRef.setInput('apiKey', 'secret-key');
    fixture.detectChanges();

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProductSubscriptionApiAccessHarness);

    expect(await harness.getText()).toContain('https://api.example.com/orders');
    expect(await harness.getText()).toContain('curl --header "X-Gravitee-Api-Key: secret-key" https://api.example.com/orders');
    expect(await harness.getDocumentationLink()).toContain('/documentation/root-id?selectedId=navigation-item-id');
  });

  it('should not expose endpoints or documentation for an unavailable API', async () => {
    fixture.componentRef.setInput('api', { ...api, availability: 'UNAVAILABLE' });
    fixture.detectChanges();

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProductSubscriptionApiAccessHarness);

    expect(await harness.getText()).toContain('This API is no longer available');
    expect(await harness.getCopyCodeCount()).toBe(0);
    expect(await harness.getDocumentationLink()).toBeNull();
  });

  it('should use an access token placeholder for OAuth2', () => {
    fixture.componentRef.setInput('planSecurity', 'OAUTH2');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'curl --header "Authorization: Bearer {{ ACCESS_TOKEN }}" https://api.example.com/orders',
    );
  });
});
