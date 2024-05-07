/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';

import { IntegrationAgentComponent } from './integration-agent.component';
import { IntegrationAgentHarness } from './integration-agent.harness';

import { IntegrationsModule } from '../integrations.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { Integration } from '../integrations.model';
import { fakeIntegration } from '../../../entities/integrations/integration.fixture';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('IntegrationAgentComponent', () => {
  let fixture: ComponentFixture<IntegrationAgentComponent>;
  let componentHarness: IntegrationAgentHarness;
  let httpTestingController: HttpTestingController;
  const integrationId: string = 'TestTestTest123';

  const init = async (
    permissions: GioTestingPermission = [
      'environment-integration-u',
      'environment-integration-d',
      'environment-integration-c',
      'environment-integration-r',
    ],
  ): Promise<void> => {
    await TestBed.configureTestingModule({
      declarations: [IntegrationAgentComponent],
      imports: [GioTestingModule, IntegrationsModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: permissions,
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { integrationId: integrationId } } },
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(IntegrationAgentComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, IntegrationAgentHarness);
    fixture.detectChanges();
  };

  afterEach(() => {
    init();
    httpTestingController.verify();
  });

  describe('wizard', () => {
    beforeEach(() => {
      init();
    });

    it('should modify configuration code', async () => {
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));
      expect(true).toBeTruthy();

      fixture.componentInstance.code = `
      - gravitee_integration_providers_0_configuration_accessKeyId=\${AWS_ACCESS_KEY_ID}
      - gravitee_integration_providers_0_configuration_region=\${AWS_REGION}
      - gravitee_integration_providers_0_configuration_secretAccessKey=\${AWS_SECRET_ACCESS_KEY}
      - gravitee_integration_providers_0_integrationId=\${INTEGRATION_ID}
      - gravitee_integration_providers_0_type=aws-api-gateway
      `;

      await componentHarness.openAccordion();

      await componentHarness.setAccessKeyId('Test-setAccessKeyId--');
      await componentHarness.setSecretAccessKey('Test-setSecretAccessKey');

      fixture.detectChanges();

      const expectedResult = `
      - gravitee_integration_providers_0_configuration_accessKeyId=Test-setAccessKeyId--
      - gravitee_integration_providers_0_configuration_region=\${AWS_REGION}
      - gravitee_integration_providers_0_configuration_secretAccessKey=Test-setSecretAccessKey
      - gravitee_integration_providers_0_integrationId=\${INTEGRATION_ID}
      - gravitee_integration_providers_0_type=aws-api-gateway
      `;

      expect(fixture.componentInstance.codeForEditor).toEqual(expectedResult);
    });
  });

  describe('refresh status', () => {
    beforeEach(() => {
      init();
    });

    it('should call backend for new status', async () => {
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));

      fixture.detectChanges();

      await componentHarness.refreshStatus();
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));
    });
  });

  function expectIntegrationGetRequest(integrationMock: Integration): void {
    const req: TestRequest = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}`);
    req.flush(integrationMock);
    expect(req.request.method).toEqual('GET');
  }
});
