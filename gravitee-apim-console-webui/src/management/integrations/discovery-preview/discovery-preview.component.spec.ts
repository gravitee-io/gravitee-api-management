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

import { DiscoveryPreviewComponent } from './discovery-preview.component';
import { DiscoveryPreviewHarness } from './discovery-preview.harness';

import { IntegrationsModule } from '../integrations.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { Integration } from '../integrations.model';
import { fakeIntegration } from '../../../entities/integrations/integration.fixture';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { fakeDiscoveryPreview } from '../../../entities/integrations/preview.fixture';

describe('DiscoveryPreviewComponent', () => {
  let fixture: ComponentFixture<DiscoveryPreviewComponent>;
  let componentHarness: DiscoveryPreviewHarness = null;
  let httpTestingController: HttpTestingController;
  const integrationId: string = 'TestTestTest123';

  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  const init = async (
    permissions: GioTestingPermission = [
      'environment-integration-u',
      'environment-integration-d',
      'environment-integration-c',
      'environment-integration-r',
    ],
  ): Promise<void> => {
    await TestBed.configureTestingModule({
      declarations: [DiscoveryPreviewComponent],
      imports: [GioTestingModule, IntegrationsModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [
        {
          provide: SnackBarService,
          useValue: fakeSnackBarService,
        },
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

    fixture = TestBed.createComponent(DiscoveryPreviewComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DiscoveryPreviewHarness);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('DiscoveryPreviewComponent', () => {
    beforeEach(() => {
      init();
    });

    it('should display preview toggles with correct values', async () => {
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));
      expectPreviewGetRequest(fakeDiscoveryPreview());

      const newItemsToggle = await componentHarness.getNewItemsToggle();
      const updateItemsToggle = await componentHarness.getUpdateItemsToggle();

      expect(await newItemsToggle.isDisabled()).toBe(false);
      expect(await updateItemsToggle.isDisabled()).toBe(true);
    });

    it('should display table with correct number of items', async () => {
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));
      expectPreviewGetRequest(fakeDiscoveryPreview());

      expect(await componentHarness.rowsNumber()).toEqual(3);
    });
  });

  function expectIntegrationGetRequest(integrationMock: Integration): void {
    const req: TestRequest = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}`);
    req.flush(integrationMock);
    expect(req.request.method).toEqual('GET');
  }

  function expectPreviewGetRequest(preview = {}): void {
    const req: TestRequest = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/_preview`);
    req.flush(preview);
    expect(req.request.method).toEqual('GET');
  }
});
