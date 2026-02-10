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
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatTabGroupHarness } from '@angular/material/tabs/testing';
import { By } from '@angular/platform-browser';

import { ApiAccessComponent } from './api-access.component';
import { Configuration } from '../../entities/configuration/configuration';
import { Subscription } from '../../entities/subscription/subscription';
import { ConfigService } from '../../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';
import { CopyCodeHarness } from '../copy-code/copy-code.harness';

describe('ApiAccessComponent', () => {
  let component: ApiAccessComponent;
  let fixture: ComponentFixture<ApiAccessComponent>;
  let harnessLoader: HarnessLoader;

  const CONFIGURATION_KAFKA_SASL_MECHANISMS = ['PLAIN', 'SCRAM-SHA-256', 'SCRAM-SHA-512'];

  @Injectable()
  class CustomConfigurationServiceStub {
    get baseURL(): string {
      return TESTING_BASE_URL;
    }
    get configuration(): Configuration {
      return {
        portal: {
          apikeyHeader: 'X-My-Apikey',
          kafkaSaslMechanisms: CONFIGURATION_KAFKA_SASL_MECHANISMS,
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
      component.entrypointUrls = ['my-entrypoint-url'];
      fixture.detectChanges();
      expect(await baseUrlShown()).toBeTruthy();
      expect(await commandLineShown()).toBeTruthy();
    });

    it('should show base url and command line with multiple urls', async () => {
      component.planSecurity = 'KEY_LESS';
      component.entrypointUrls = ['my-entrypoint-url', 'my-entrypoint-url-2'];
      fixture.detectChanges();
      expect(await baseUrlShown()).toBeFalsy();

      const selectEntrypoints = await getSelectEntrypoints();
      expect(selectEntrypoints).toBeTruthy();
      expect(await selectEntrypoints?.getValueText()).toContain('my-entrypoint-url');
      expect(await commandLineShown()).toBeTruthy();
    });
  });

  describe('Accepted', () => {
    describe('HTTP API', () => {
      describe('API Key', () => {
        it('should show api key, base url and command line', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
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
          component.subscription = { status: 'ACCEPTED' } as Subscription;
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
          component.subscription = { status: 'ACCEPTED' } as Subscription;
          component.clientId = 'my-client-id';
          component.clientSecret = 'my-client-secret';

          fixture.detectChanges();
          expect(await clientIdShown()).toBeTruthy();
          expect(await clientSecretShown()).toBeTruthy();
          expect(await baseUrlShown()).toBeFalsy();
        });
      });
    });
    describe('Native API', () => {
      beforeEach(() => {
        component.apiType = 'NATIVE';
        component.entrypointUrls = ['my-entrypoint-url'];
        component.subscription = { status: 'ACCEPTED' } as Subscription;
      });
      describe('API Key', () => {
        it('should show api key, config, producer and consumer calls', async () => {
          component.planSecurity = 'API_KEY';
          component.apiKey = 'api-key';
          component.apiKeyConfigUsername = 'hash-username';

          fixture.detectChanges();
          expect(await apiKeyShown()).toBeFalsy();
          expect(await baseUrlShown()).toBeFalsy();
          expect(await commandLineShown()).toBeFalsy();

          expect(await apiKeyUsernameShown()).toBeTruthy();
          expect(await apiKeyPasswordShown()).toBeTruthy();
          expect(await plainConfigurationShown()).toBeTruthy();
          expect(await scram256ConfigurationShown()).toBeFalsy();
          expect(await scram512ConfigurationShown()).toBeFalsy();

          const plainConfiguration = await getPlainConfiguration();
          const plainConfigurationContent = await plainConfiguration?.host().then(host => host.text());
          expect(plainConfigurationContent).toContain(component.apiKey);
          expect(plainConfigurationContent).toContain(component.apiKeyConfigUsername);

          const configTabs = await getApiKeyPropertiesConfiguration();
          expect(await configTabs.getTabs().then(tabs => tabs.length)).toEqual(3);
          await configTabs.selectTab({ label: 'SCRAM-SHA-256' });

          expect(await plainConfigurationShown()).toBeFalsy();
          expect(await scram256ConfigurationShown()).toBeTruthy();
          expect(await scram512ConfigurationShown()).toBeFalsy();

          await configTabs.selectTab({ label: 'SCRAM-SHA-512' });

          expect(await plainConfigurationShown()).toBeFalsy();
          expect(await scram256ConfigurationShown()).toBeFalsy();
          expect(await scram512ConfigurationShown()).toBeTruthy();

          const commandExamples = await getCommandExamples();
          expect(await commandExamples.getTabs().then(tabs => tabs.length)).toEqual(2);
          expect(await producerCommandShown()).toBeTruthy();
          expect(await consumerCommandShown()).toBeFalsy();

          await commandExamples.selectTab({ label: 'Consumer' });
          expect(await producerCommandShown()).toBeFalsy();
          expect(await consumerCommandShown()).toBeTruthy();
        });

        describe('Multiple Entrypoint URLs', () => {
          it('should show api key, config, producer and consumer calls', async () => {
            component.planSecurity = 'API_KEY';
            component.apiKey = 'api-key';
            component.apiKeyConfigUsername = 'hash-username';
            component.entrypointUrls = ['my-entrypoint-url-1', 'my-entrypoint-url-2'];

            fixture.detectChanges();

            const selectEntrypoints = await getSelectEntrypoints();
            expect(selectEntrypoints).toBeTruthy();

            const commandExamples = await getCommandExamples();
            expect(await (await getCopyCodeHarnessOrNullById('native-kafka-producer'))?.getText()).toContain('my-entrypoint-url-1');

            await selectEntrypoints?.clickOptions({ text: 'my-entrypoint-url-2' });
            fixture.detectChanges();

            await commandExamples.selectTab({ label: 'Consumer' });
            expect(await (await getCopyCodeHarnessOrNullById('native-kafka-consumer'))?.getText()).toContain('my-entrypoint-url-2');
          });
        });
      });
      describe('OAuth2', () => {
        it('should show client id and secret', async () => {
          component.planSecurity = 'OAUTH2';
          component.clientId = 'my-client-id';
          component.clientSecret = 'my-client-secret';

          fixture.detectChanges();
          expect(await clientIdShown()).toBeTruthy();
          expect(await clientSecretShown()).toBeTruthy();
          expect(await baseUrlShown()).toBeFalsy();

          expect(await getCommandExamples()).toBeTruthy();
          expect(await producerCommandShown()).toBeTruthy();
          expect(await consumerCommandShown()).toBeFalsy();
        });
      });
      describe('JWT', () => {
        it('should show client id and secret', async () => {
          component.planSecurity = 'JWT';
          component.clientId = 'my-client-id';
          component.clientSecret = 'my-client-secret';

          fixture.detectChanges();
          expect(await clientIdShown()).toBeTruthy();
          expect(await clientSecretShown()).toBeTruthy();
          expect(await baseUrlShown()).toBeFalsy();

          expect(await getCommandExamples()).toBeTruthy();
          expect(await producerCommandShown()).toBeTruthy();
          expect(await consumerCommandShown()).toBeFalsy();
        });
      });
    });
  });

  describe('Pending', () => {
    it('should show message', async () => {
      component.planSecurity = 'API_KEY';
      component.subscription = { status: 'PENDING' } as Subscription;
      component.entrypointUrls = ['my-entrypoint-url'];
      component.apiKey = 'api-key';
      fixture.detectChanges();
      await expectMessageTextContains('Subscription in progress');
      await expectMessageTextContains('Your subscription request is being validated. Come back later.');
    });
  });
  describe('Rejected', () => {
    it('should show message', async () => {
      component.planSecurity = 'API_KEY';
      component.subscription = { status: 'REJECTED' } as Subscription;
      component.entrypointUrls = ['my-entrypoint-url'];
      component.apiKey = 'api-key';

      fixture.detectChanges();
      await expectMessageTextContains('Subscription rejected');
    });
  });
  describe('Paused', () => {
    it('should show message', async () => {
      component.planSecurity = 'API_KEY';
      component.subscription = { status: 'PAUSED' } as Subscription;
      component.entrypointUrls = ['my-entrypoint-url'];
      component.apiKey = 'api-key';

      fixture.detectChanges();
      await expectMessageTextContains('Subscription paused');
    });
  });
  describe('Closed', () => {
    it('should show message', async () => {
      component.planSecurity = 'API_KEY';
      component.subscription = { status: 'CLOSED' } as Subscription;
      component.entrypointUrls = ['my-entrypoint-url'];
      component.apiKey = 'api-key';

      fixture.detectChanges();
      await expectMessageTextContains('Subscription closed');
    });
  });

  async function apiKeyShown() {
    return !!(await getCopyCodeHarnessOrNullByTitle('API Key'));
  }
  async function apiKeyUsernameShown() {
    return !!(await getCopyCodeHarnessOrNullByTitle('Username'));
  }
  async function apiKeyPasswordShown() {
    return !!(await getCopyCodeHarnessOrNullByTitle('Password'));
  }
  async function baseUrlShown() {
    return !!(await getCopyCodeHarnessOrNullById('base-url'));
  }
  async function getSelectEntrypoints() {
    return await harnessLoader.getHarnessOrNull(MatSelectHarness.with({ selector: '#select-entrypoints' }));
  }

  async function commandLineShown() {
    return !!(await getCopyCodeHarnessOrNullById('command-line'));
  }
  async function clientIdShown() {
    return !!(await getCopyCodeHarnessOrNullByTitle('Client ID'));
  }
  async function clientSecretShown() {
    return !!(await getCopyCodeHarnessOrNullByTitle('Client Secret'));
  }
  async function getApiKeyPropertiesConfiguration() {
    return await harnessLoader.getHarness(MatTabGroupHarness.with({ selector: '[aria-label="API Key connect properties"]' }));
  }
  async function getPlainConfiguration() {
    return await getCopyCodeHarnessOrNullById('native-kafka-api-key-plain-properties');
  }
  async function plainConfigurationShown() {
    return !!(await getCopyCodeHarnessOrNullById('native-kafka-api-key-plain-properties'));
  }
  async function scram256ConfigurationShown() {
    return !!(await getCopyCodeHarnessOrNullById('native-kafka-api-key-scram-sha-256-properties'));
  }
  async function scram512ConfigurationShown() {
    return !!(await getCopyCodeHarnessOrNullById('native-kafka-api-key-scram-sha-512-properties'));
  }
  async function getCommandExamples() {
    return await harnessLoader.getHarness(MatTabGroupHarness.with({ selector: '[aria-label="Example commands"]' }));
  }
  async function producerCommandShown() {
    return !!(await getCopyCodeHarnessOrNullById('native-kafka-producer'));
  }
  async function consumerCommandShown() {
    return !!(await getCopyCodeHarnessOrNullById('native-kafka-consumer'));
  }
  async function expectMessageTextContains(expectedMessage: string) {
    expect(fixture.debugElement.query(By.css('.api-access__message')).nativeElement.innerHTML).toContain(expectedMessage);
  }

  async function getCopyCodeHarnessOrNullByTitle(title: string): Promise<CopyCodeHarness | null> {
    return await harnessLoader.getHarnessOrNull(CopyCodeHarness.with({ selector: `[title="${title}"]` }));
  }
  async function getCopyCodeHarnessOrNullById(id: string): Promise<CopyCodeHarness | null> {
    return await harnessLoader.getHarnessOrNull(CopyCodeHarness.with({ selector: `#${id}` }));
  }
});
