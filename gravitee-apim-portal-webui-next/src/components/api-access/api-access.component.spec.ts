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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ApiAccessComponent } from './api-access.component';
import { Configuration } from '../../entities/configuration/configuration';
import { ConfigService } from '../../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';
import { CopyCodeHarness } from '../copy-code/copy-code.harness';

describe('ApiAccessComponent', () => {
  let component: ApiAccessComponent;
  let fixture: ComponentFixture<ApiAccessComponent>;
  let harnessLoader: HarnessLoader;

  @Injectable()
  class CustomConfigurationServiceStub {
    get baseURL(): string {
      return TESTING_BASE_URL;
    }
    get configuration(): Configuration {
      return {
        portal: {
          apikeyHeader: 'X-My-Apikey',
        },
      };
    }
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAccessComponent, AppTestingModule],
      providers: [
        {
          provide: ConfigService,
          useClass: CustomConfigurationServiceStub,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiAccessComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
  });

  describe('Keyless', () => {
    it('should show base url and command line', async () => {
      component.planSecurity = 'KEY_LESS';
      fixture.detectChanges();
      expect(await baseUrlShown()).toBeTruthy();
      expect(await commandLineShown()).toBeTruthy();
    });
  });

  describe('Accepted', () => {
    describe('API Key', () => {
      it('should show api key, base url and command line', async () => {
        component.planSecurity = 'API_KEY';
        component.subscriptionStatus = 'ACCEPTED';
        component.entrypointUrl = 'my-entrypoint-url';
        component.apiKey = 'api-key';

        fixture.detectChanges();
        expect(await apiKeyShown()).toBeTruthy();
        expect(await baseUrlShown()).toBeTruthy();
        expect(await commandLineShown()).toBeTruthy();
      });
    });
    describe('OAuth2', () => {
      it('should show client id and secret', async () => {
        component.planSecurity = 'OAUTH2';
        component.subscriptionStatus = 'ACCEPTED';
        component.clientId = 'my-client-id';
        component.clientSecret = 'my-client-secret';

        fixture.detectChanges();
        expect(await clientIdShown()).toBeTruthy();
        expect(await clientSecretShown()).toBeTruthy();
        expect(await baseUrlShown()).toBeFalsy();
      });
    });
    describe('JWT', () => {
      it('should show client id and secret', async () => {
        component.planSecurity = 'JWT';
        component.subscriptionStatus = 'ACCEPTED';
        component.clientId = 'my-client-id';
        component.clientSecret = 'my-client-secret';

        fixture.detectChanges();
        expect(await clientIdShown()).toBeTruthy();
        expect(await clientSecretShown()).toBeTruthy();
        expect(await baseUrlShown()).toBeFalsy();
      });
    });
  });

  describe('Pending', () => {
    it('should show message', async () => {
      component.planSecurity = 'API_KEY';
      component.subscriptionStatus = 'PENDING';
      component.entrypointUrl = 'my-entrypoint-url';
      component.apiKey = 'api-key';
      fixture.detectChanges();
      await expectMessageTextContains('Subscription in progress');
      await expectMessageTextContains('Your subscription request is being validated. Come back later.');
    });
  });
  describe('Rejected', () => {
    it('should show message', async () => {
      component.planSecurity = 'API_KEY';
      component.subscriptionStatus = 'REJECTED';
      component.entrypointUrl = 'my-entrypoint-url';
      component.apiKey = 'api-key';

      fixture.detectChanges();
      await expectMessageTextContains('Subscription rejected');
    });
  });
  describe('Paused', () => {
    it('should show message', async () => {
      component.planSecurity = 'API_KEY';
      component.subscriptionStatus = 'PAUSED';
      component.entrypointUrl = 'my-entrypoint-url';
      component.apiKey = 'api-key';

      fixture.detectChanges();
      await expectMessageTextContains('Subscription paused');
    });
  });
  describe('Closed', () => {
    it('should show message', async () => {
      component.planSecurity = 'API_KEY';
      component.subscriptionStatus = 'CLOSED';
      component.entrypointUrl = 'my-entrypoint-url';
      component.apiKey = 'api-key';

      fixture.detectChanges();
      await expectMessageTextContains('Subscription closed');
    });
  });

  async function apiKeyShown() {
    return !!(await getCopyCodeHarnessOrNull('API Key'));
  }
  async function baseUrlShown() {
    return !!(await getCopyCodeHarnessOrNull('Base URL'));
  }
  async function commandLineShown() {
    return !!(await getCopyCodeHarnessOrNull('Command Line'));
  }
  async function clientIdShown() {
    return !!(await getCopyCodeHarnessOrNull('Client ID'));
  }
  async function clientSecretShown() {
    return !!(await getCopyCodeHarnessOrNull('Client Secret'));
  }
  async function expectMessageTextContains(expectedMessage: string) {
    expect(fixture.debugElement.query(By.css('.api-access__message')).nativeElement.innerHTML).toContain(expectedMessage);
  }

  async function getCopyCodeHarnessOrNull(title: string): Promise<CopyCodeHarness | null> {
    return await harnessLoader.getHarnessOrNull(CopyCodeHarness.with({ selector: `[title="${title}"]` }));
  }
});
