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
import { By } from '@angular/platform-browser';

import { IntegrationAgentComponent } from './integration-agent.component';
import { IntegrationAgentHarness } from './integration-agent.harness';

import { IntegrationsModule } from '../integrations.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { Integration } from '../integrations.model';
import { fakeIntegration } from '../../../entities/integrations/integration.fixture';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../entities/Constants';
import { IntegrationProviderService } from '../integration-provider.service';

describe('IntegrationAgentComponent', () => {
  let fixture: ComponentFixture<IntegrationAgentComponent>;
  let componentHarness: IntegrationAgentHarness;
  let httpTestingController: HttpTestingController;
  const integrationId: string = 'TestTestTest123';

  const fakeIntegrationProviderService = {
    getApimDocsNameByValue: jest.fn(),
  };

  const init = async (
    permissions: GioTestingPermission = [
      'environment-integration-u',
      'environment-integration-d',
      'environment-integration-c',
      'environment-integration-r',
    ],
    constantsOverride: Partial<Constants> = {},
  ): Promise<void> => {
    const constants = { ...CONSTANTS_TESTING, ...constantsOverride };

    await TestBed.configureTestingModule({
      declarations: [IntegrationAgentComponent],
      imports: [GioTestingModule, IntegrationsModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: permissions,
        },
        {
          provide: Constants,
          useValue: constants,
        },
        {
          provide: IntegrationProviderService,
          useValue: fakeIntegrationProviderService,
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
    httpTestingController.verify();
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

  describe('view documentation button', () => {
    it('generate proper apim docs link', async () => {
      fakeIntegrationProviderService.getApimDocsNameByValue.mockReturnValue('test-provider');

      await init();
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));

      fixture.detectChanges();

      const anchorEl = fixture.debugElement.query(By.css('a[mat-raised-button]')).nativeElement;
      expect(anchorEl.getAttribute('href')).toBe(
        'https://documentation.gravitee.io/apim/4.7/governance/federation/3rd-party-providers/test-provider',
      );
    });

    it('disable button if unknown provider', async () => {
      fakeIntegrationProviderService.getApimDocsNameByValue.mockReturnValue(undefined);

      await init();
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));

      fixture.detectChanges();

      const anchorEl = fixture.debugElement.query(By.css('a[mat-raised-button]')).nativeElement;
      expect(anchorEl.getAttribute('disabled')).toBe('true');
    });

    it('disable button if incorrect build version', async () => {
      fakeIntegrationProviderService.getApimDocsNameByValue.mockReturnValue('test-provider');

      await init(undefined, {
        build: {
          ...CONSTANTS_TESTING.build,
          version: 'an-incorrect-version',
        },
      });
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));

      fixture.detectChanges();

      const anchorEl = fixture.debugElement.query(By.css('a[mat-raised-button]')).nativeElement;
      expect(anchorEl.getAttribute('disabled')).toBe('true');
    });
  });
});
