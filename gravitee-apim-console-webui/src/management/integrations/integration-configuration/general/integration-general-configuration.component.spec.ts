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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { MatErrorHarness } from '@angular/material/form-field/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCardHarness } from '@angular/material/card/testing';
import { GioConfirmDialogHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { IntegrationGeneralConfigurationComponent } from './integration-general-configuration.component';
import { IntegrationGeneralConfigurationHarness } from './integration-general-configuration.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { IntegrationsModule } from '../../integrations.module';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { DeletedFederatedAPIsResponse, FederatedAPI, FederatedAPIsResponse, Integration } from '../../integrations.model';
import { fakeIntegration } from '../../../../entities/integrations/integration.fixture';
import { fakeFederatedAPI } from '../../../../entities/integrations/federatedAPI.fixture';

describe('IntegrationGeneralConfigurationComponent', (): void => {
  let fixture: ComponentFixture<IntegrationGeneralConfigurationComponent>;
  let componentHarness: IntegrationGeneralConfigurationHarness;
  let httpTestingController: HttpTestingController;
  const integrationId: string = '123TestID';

  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  const init = async (
    permissions: GioTestingPermission = [
      'integration-definition-u',
      'integration-definition-d',
      'integration-definition-c',
      'integration-definition-r',
    ],
  ): Promise<void> => {
    await TestBed.configureTestingModule({
      declarations: [IntegrationGeneralConfigurationComponent],
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
    fixture = TestBed.createComponent(IntegrationGeneralConfigurationComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, IntegrationGeneralConfigurationHarness);
    fixture.detectChanges();
  };

  afterEach(() => {
    init();
    httpTestingController.verify();
  });

  describe('update integration details', () => {
    beforeEach(() => {
      init();
    });

    it('should show validation error when name is not valid', async () => {
      expectIntegrationGetRequest(fakeIntegration());
      expectFederatedAPIsGetRequest();

      await componentHarness.setName('');
      await componentHarness.setDescription('Some description');
      fixture.detectChanges();

      const error: MatErrorHarness = await componentHarness.matErrorMessage();
      expect(await error.getText()).toEqual('Integration name is required.');

      await componentHarness.setName('test too long name 01234567890123456789012345678901234567890123456789');
      await componentHarness.setDescription('Some description');
      fixture.detectChanges();

      const errorToLongName: MatErrorHarness = await componentHarness.matErrorMessage();
      expect(await errorToLongName.getText()).toEqual('Integration name can not exceed 50 characters.');
    });

    it('should not show submit bar when form not valid', async () => {
      expectIntegrationGetRequest(fakeIntegration());
      expectFederatedAPIsGetRequest();

      await componentHarness.setName('');
      await componentHarness.setDescription('Some description');
      fixture.detectChanges();

      const submitBar: GioSaveBarHarness = await componentHarness.getSubmitButton();
      expect(submitBar).toBeNull();

      await componentHarness.setName('test too long name 01234567890123456789012345678901234567890123456789');
      await componentHarness.setDescription('Some description');
      fixture.detectChanges();

      const submitBarTwo: GioSaveBarHarness = await componentHarness.getSubmitButton();
      expect(submitBarTwo).toBeNull();
    });

    it('should send request PUT with payload', async () => {
      expectIntegrationGetRequest(fakeIntegration());
      expectFederatedAPIsGetRequest();

      await componentHarness.setName('Test Title');
      await componentHarness.setDescription('Description');
      fixture.detectChanges();

      await componentHarness.clickOnSubmit();

      expectIntegrationPutRequest(fakeIntegration(), {
        description: 'Description',
        name: 'Test Title',
        groups: [],
      });
      expectIntegrationGetRequest(fakeIntegration());
    });
  });

  describe('no delete permissions', () => {
    beforeEach((): void => {
      init([]);
    });

    it('should not display delete button', async () => {
      expectIntegrationGetRequest(fakeIntegration());
      expectFederatedAPIsGetRequest();
      const deleteButton: MatButtonHarness = await componentHarness.getDeleteIntegrationButton();
      expect(deleteButton).toBeNull();
    });
  });

  describe('no update permissions', () => {
    beforeEach(() => {
      init([]);
    });

    it('should not display update form', async () => {
      expectIntegrationGetRequest(fakeIntegration());
      expectFederatedAPIsGetRequest();
      const updateForm: MatCardHarness = await componentHarness.getUpdateSection();
      expect(updateForm).toBeNull();
    });
  });

  describe('danger zone', () => {
    beforeEach((): void => {
      init();
    });

    it('should send DELETE request with correct ID', async () => {
      expectIntegrationGetRequest(fakeIntegration({ id: 'idToDelete123' }));
      expectFederatedAPIsGetRequest([]);

      const deleteButton: MatButtonHarness = await componentHarness.getDeleteIntegrationButton();
      await deleteButton.click();

      fixture.detectChanges();

      const dialogHarness: GioConfirmDialogHarness =
        await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      await dialogHarness.confirm();

      expectIntegrationDeleteRequest('idToDelete123');
    });
  });

  describe('delete button', () => {
    beforeEach((): void => {
      init();
    });

    it('should be disabled when federated APIS are present', async (): Promise<void> => {
      expectIntegrationGetRequest(fakeIntegration({ id: 'idToDelete123' }));
      expectFederatedAPIsGetRequest([fakeFederatedAPI(), fakeFederatedAPI(), fakeFederatedAPI()]);

      const deleteButton: MatButtonHarness = await componentHarness.getDeleteIntegrationButton();
      expect(await deleteButton.isDisabled()).toBeTruthy();
    });

    it('should be enabled when federated APIs are not present', async (): Promise<void> => {
      expectIntegrationGetRequest(fakeIntegration({ id: 'idToDelete123' }));
      expectFederatedAPIsGetRequest([]);

      const deleteButton: MatButtonHarness = await componentHarness.getDeleteIntegrationButton();
      expect(fixture.componentInstance.hasFederatedAPIs).toBeFalsy();
      expect(await deleteButton.isDisabled()).toBeFalsy();
    });
  });

  describe('delete federated APIs button', () => {
    it('should be disabled when federated APIS are not present', async (): Promise<void> => {
      await init(['environment-api-d', 'integration-definition-d']);
      expectIntegrationGetRequest(fakeIntegration({ id: 'idToDelete123' }));
      expectFederatedAPIsGetRequest([], 1, 10);

      const deleteButton: MatButtonHarness = await componentHarness.getDeleteFederatedApisButton();
      expect(await deleteButton.isDisabled()).toBeTruthy();
    });

    it('should be hidden when user has no delete API permission', async (): Promise<void> => {
      await init(['integration-definition-d']);
      expectIntegrationGetRequest(fakeIntegration({ id: 'idToDelete123' }));
      expectFederatedAPIsGetRequest([], 1, 10);
      const deleteButton: MatButtonHarness = await componentHarness.getDeleteFederatedApisButton();
      expect(deleteButton).toBeNull();
    });

    it('should send delete request with proper integration ID and display deleted items info', async (): Promise<void> => {
      await init(['environment-api-d', 'integration-definition-d']);
      expectIntegrationGetRequest(fakeIntegration({ id: 'idToDelete123' }));
      expectFederatedAPIsGetRequest([fakeFederatedAPI(), fakeFederatedAPI(), fakeFederatedAPI()]);

      const deleteButton: MatButtonHarness = await componentHarness.getDeleteFederatedApisButton();

      await deleteButton.click();
      fixture.detectChanges();

      const dialogHarness: GioConfirmDialogHarness =
        await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      await dialogHarness.confirm();

      expectDeleteAPIsRequest('idToDelete123', { deleted: 5, skipped: 1, errors: 1 });
      expect(fakeSnackBarService.success).toHaveBeenCalledWith('We’re deleting Federated APIs from this integration...');
      expectFederatedAPIsGetRequest([], 1, 10);

      expect(fakeSnackBarService.success).toHaveBeenCalledWith(
        'Federated APIs have been deleted.\n' + '  • Deleted: 5\n' + '  • Not deleted: 1\n' + '  • Errors: 1',
      );
    });
  });

  function expectIntegrationDeleteRequest(id: string): void {
    const req: TestRequest = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${id}`);
    expect(req.request.method).toEqual('DELETE');
  }

  function expectDeleteAPIsRequest(id: string, response: DeletedFederatedAPIsResponse): void {
    const req: TestRequest = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${id}/apis`);
    req.flush(response);
    expect(req.request.method).toEqual('DELETE');
  }

  function expectIntegrationGetRequest(integrationMock: Integration): void {
    const req: TestRequest = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}`);
    req.flush(integrationMock);
    expect(req.request.method).toEqual('GET');
  }

  function expectIntegrationPutRequest(integrationMock: Integration, payload): void {
    const req: TestRequest = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}`);
    req.flush(integrationMock);
    expect(req.request.method).toEqual('PUT');
    expect(req.request.body).toEqual(payload);
  }

  function expectFederatedAPIsGetRequest(federatedAPIs: FederatedAPI[] = [fakeFederatedAPI()], page = 1, perPage = 10): void {
    const req: TestRequest = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/apis?page=${page}&perPage=${perPage}`,
    );
    const res: FederatedAPIsResponse = {
      data: federatedAPIs,
      pagination: {
        page,
        perPage,
      },
    };

    req.flush(res);
    expect(req.request.method).toEqual('GET');
  }
});
