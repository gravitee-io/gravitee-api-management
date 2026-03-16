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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioConfirmDialogHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY } from 'rxjs';

import { ApplicationGeneralComponent } from './application-general.component';
import { ApplicationGeneralModule } from './application-general.module';
import { AddCertificateDialogComponent } from './add-certificate-dialog/add-certificate-dialog.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakeApplication, fakeApplicationType } from '../../../../entities/application/Application.fixture';
import { Application, ApplicationType } from '../../../../entities/application/Application';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ClientCertificate, ClientCertificateStatus } from '../../../../entities/application/ClientCertificate';

describe('ApplicationGeneralInfoComponent', () => {
  const APPLICATION_ID = 'id_test';

  let fixture: ComponentFixture<ApplicationGeneralComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApplicationGeneralModule, MatIconTestingModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['application-definition-u', 'application-definition-d', 'application-definition-c', 'application-definition-r'],
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .overrideProvider(ActivatedRoute, { useValue: { snapshot: { params: { applicationId: APPLICATION_ID } } } });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApplicationGeneralComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('Application General details', () => {
    it('should edit application details', async () => {
      const applicationDetails = fakeApplication({ type: 'NATIVE' });
      const applicationType = fakeApplicationType();
      expectListApplicationRequest(applicationDetails);
      expectApplicationTypeRequest(applicationType);
      expectListCertificatesRequest([]);
      fixture.detectChanges();
      await waitImageCheck();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      expect(await nameInput.getValue()).toEqual('Default application');
      await nameInput.setValue('new test name');

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/applications/${applicationDetails.id}`);
      expect(req.request.method).toEqual('PUT');
      expect(req.request.body).toEqual({
        api_key_mode: 'UNSPECIFIED',
        background: undefined,
        description: 'My default application',
        disable_membership_notifications: undefined,
        domain: undefined,
        name: 'new test name',
        picture: undefined,
        groups: ['f1194262-9157-4986-9942-629157f98682'],
        settings: {
          oauth: {
            additional_client_metadata: {},
            application_type: 'id_test',
            client_id: 'test_client_id',
            client_secret: 'test_client_secret',
            grant_types: ['authorization_code', 'refresh_token', 'password', 'implicit'],
            redirect_uris: ['https://apim-master-console.team-apim.gravitee.dev/'],
            renew_client_secret_supported: false,
            response_types: ['code', 'token', 'id_token'],
          },
        },
      });
    });
  });

  describe('Application General details when 0Auth2 integration enabled', () => {
    it('should edit 0Auth2 form details', async () => {
      const applicationDetails = fakeApplication({ type: 'SIMPLE' });
      const applicationType = fakeApplicationType();
      expectListApplicationRequest(applicationDetails);
      expectApplicationTypeRequest(applicationType);
      expectListCertificatesRequest([]);
      fixture.detectChanges();
      await waitImageCheck();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="client_id"]' }));
      expect(await nameInput.getValue()).toEqual('');
      await nameInput.setValue('123');

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/applications/${applicationDetails.id}`);
      expect(req.request.method).toEqual('PUT');
      expect(req.request.body).toEqual({
        api_key_mode: 'UNSPECIFIED',
        background: undefined,
        description: 'My default application',
        name: 'Default application',
        picture: undefined,
        groups: ['f1194262-9157-4986-9942-629157f98682'],
        settings: {
          app: {
            client_id: '123',
          },
        },
      });
    });
  });

  describe('Application General details when OpenID Connect integration enabled', () => {
    it('should edit OpenID Connect form details', async () => {
      const applicationDetails = fakeApplication({ type: 'NATIVE' });
      const applicationType = fakeApplicationType();
      expectListApplicationRequest(applicationDetails);
      expectApplicationTypeRequest(applicationType);
      expectListCertificatesRequest([]);
      fixture.detectChanges();
      await waitImageCheck();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="client_id"]' }));
      expect(await nameInput.getValue()).toEqual('test_client_id');
      await nameInput.setValue('123');

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/applications/${applicationDetails.id}`);
      expect(req.request.method).toEqual('PUT');
      expect(req.request.body).toEqual({
        api_key_mode: 'UNSPECIFIED',
        background: undefined,
        description: 'My default application',
        disable_membership_notifications: undefined,
        domain: undefined,
        name: 'Default application',
        picture: undefined,
        groups: ['f1194262-9157-4986-9942-629157f98682'],
        settings: {
          oauth: {
            additional_client_metadata: {},
            application_type: 'id_test',
            client_id: '123',
            client_secret: 'test_client_secret',
            grant_types: ['authorization_code', 'refresh_token', 'password', 'implicit'],
            redirect_uris: ['https://apim-master-console.team-apim.gravitee.dev/'],
            renew_client_secret_supported: false,
            response_types: ['code', 'token', 'id_token'],
          },
        },
      });
    });
  });

  describe('Certificate table', () => {
    it('should display certificates in the table', async () => {
      const applicationDetails = fakeApplication({ type: 'SIMPLE' });
      const applicationType = fakeApplicationType();
      const certificates: ClientCertificate[] = [
        {
          id: 'cert-1',
          name: 'My Certificate',
          createdAt: '2026-01-15T10:00:00Z',
          certificateExpiration: '2027-01-15T10:00:00Z',
          endsAt: '2027-01-15T10:00:00Z',
          status: ClientCertificateStatus.ACTIVE,
        },
        {
          id: 'cert-2',
          name: 'Old Certificate',
          createdAt: '2025-06-01T10:00:00Z',
          certificateExpiration: '2026-06-01T10:00:00Z',
          endsAt: '2026-06-01T10:00:00Z',
          status: ClientCertificateStatus.REVOKED,
        },
      ];

      expectListApplicationRequest(applicationDetails);
      expectApplicationTypeRequest(applicationType);
      expectListCertificatesRequest(certificates);
      fixture.detectChanges();
      await waitImageCheck();

      const rows = fixture.nativeElement.querySelectorAll('.tls-card__table tr[mat-row]');
      expect(rows.length).toBe(2);
    });

    it('should display empty state when no certificates', async () => {
      const applicationDetails = fakeApplication({ type: 'SIMPLE' });
      const applicationType = fakeApplicationType();

      expectListApplicationRequest(applicationDetails);
      expectApplicationTypeRequest(applicationType);
      expectListCertificatesRequest([]);
      fixture.detectChanges();
      await waitImageCheck();

      const emptyMessage = fixture.nativeElement.querySelector('[data-testid="no-certificates-message"]');
      expect(emptyMessage).toBeTruthy();
      expect(emptyMessage.textContent).toContain('No mTLS certificates added');
    });

    it('should show add certificate button', async () => {
      const applicationDetails = fakeApplication({ type: 'SIMPLE' });
      const applicationType = fakeApplicationType();

      expectListApplicationRequest(applicationDetails);
      expectApplicationTypeRequest(applicationType);
      expectListCertificatesRequest([]);
      fixture.detectChanges();
      await waitImageCheck();

      const addButton = fixture.nativeElement.querySelector('[data-testid="add-certificate-button"]');
      expect(addButton).toBeTruthy();
    });
  });

  describe('Delete certificate', () => {
    it('should_delete_certificate_after_confirmation', async () => {
      const applicationDetails = fakeApplication({ type: 'SIMPLE' });
      const applicationType = fakeApplicationType();
      const certificates: ClientCertificate[] = [
        {
          id: 'cert-1',
          name: 'My Certificate',
          createdAt: '2026-01-15T10:00:00Z',
          certificateExpiration: '2027-01-15T10:00:00Z',
          endsAt: '2027-01-15T10:00:00Z',
          status: ClientCertificateStatus.ACTIVE,
        },
      ];

      expectListApplicationRequest(applicationDetails);
      expectApplicationTypeRequest(applicationType);
      expectListCertificatesRequest(certificates);
      fixture.detectChanges();
      await waitImageCheck();

      const deleteButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="delete-certificate-button"]' }));
      await deleteButton.click();

      const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await confirmDialog.confirm();

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/certificates/cert-1`,
          method: 'DELETE',
        })
        .flush(null);

      expectListCertificatesRequest([]);
    });
  });

  describe('Add certificate dialog targeting', () => {
    it('should_target_active_cert_when_both_active_and_active_with_end_exist', async () => {
      const applicationDetails = fakeApplication({ type: 'SIMPLE' });
      const applicationType = fakeApplicationType();
      const certificates: ClientCertificate[] = [
        {
          id: 'cert-old',
          name: 'First Certificate (rotated)',
          createdAt: '2025-06-01T10:00:00Z',
          certificateExpiration: '2027-06-01T10:00:00Z',
          endsAt: '2026-06-01T10:00:00Z',
          status: ClientCertificateStatus.ACTIVE_WITH_END,
        },
        {
          id: 'cert-new',
          name: 'Second Certificate (current)',
          createdAt: '2026-01-15T10:00:00Z',
          certificateExpiration: '2027-01-15T10:00:00Z',
          status: ClientCertificateStatus.ACTIVE,
        },
      ];

      expectListApplicationRequest(applicationDetails);
      expectApplicationTypeRequest(applicationType);
      expectListCertificatesRequest(certificates);
      fixture.detectChanges();
      await waitImageCheck();

      const matDialog = TestBed.inject(MatDialog);
      const openSpy = jest.spyOn(matDialog, 'open').mockReturnValue({ afterClosed: () => EMPTY } as any);

      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="add-certificate-button"]' }));
      await addButton.click();

      expect(openSpy).toHaveBeenCalledWith(
        AddCertificateDialogComponent,
        expect.objectContaining({
          data: {
            hasActiveCertificates: true,
            activeCertificateId: 'cert-new',
          },
        }),
      );
    });

    it('should_target_newest_active_with_end_cert_when_no_active_cert_exists', async () => {
      const applicationDetails = fakeApplication({ type: 'SIMPLE' });
      const applicationType = fakeApplicationType();
      const certificates: ClientCertificate[] = [
        {
          id: 'cert-older',
          name: 'Older Certificate',
          createdAt: '2025-01-01T10:00:00Z',
          certificateExpiration: '2027-01-01T10:00:00Z',
          endsAt: '2026-12-01T10:00:00Z',
          status: ClientCertificateStatus.ACTIVE_WITH_END,
        },
        {
          id: 'cert-newer',
          name: 'Newer Certificate',
          createdAt: '2025-06-01T10:00:00Z',
          certificateExpiration: '2027-06-01T10:00:00Z',
          endsAt: '2027-01-01T10:00:00Z',
          status: ClientCertificateStatus.ACTIVE_WITH_END,
        },
      ];

      expectListApplicationRequest(applicationDetails);
      expectApplicationTypeRequest(applicationType);
      expectListCertificatesRequest(certificates);
      fixture.detectChanges();
      await waitImageCheck();

      const matDialog = TestBed.inject(MatDialog);
      const openSpy = jest.spyOn(matDialog, 'open').mockReturnValue({ afterClosed: () => EMPTY } as any);

      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="add-certificate-button"]' }));
      await addButton.click();

      expect(openSpy).toHaveBeenCalledWith(
        AddCertificateDialogComponent,
        expect.objectContaining({
          data: {
            hasActiveCertificates: true,
            activeCertificateId: 'cert-newer',
          },
        }),
      );
    });
  });

  describe('Application General details status is ARCHIVED', () => {
    it('details form should be set to readonly', async () => {
      const applicationDetails = fakeApplication({ status: 'ARCHIVED' });
      const applicationType = fakeApplicationType();
      expectListApplicationRequest(applicationDetails);
      expectApplicationTypeRequest(applicationType);
      expectListCertificatesRequest([]);
      fixture.detectChanges();
      await waitImageCheck();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="client_id"]' }));
      expect(await nameInput.isDisabled()).toEqual(true);

      const addButton = fixture.nativeElement.querySelector('[data-testid="add-certificate-button"]');
      expect(addButton).toBeFalsy();
    });
  });

  function expectListApplicationRequest(applicationDetails: Application) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`,
        method: 'GET',
      })
      .flush(applicationDetails);
  }

  function expectApplicationTypeRequest(applicationType: ApplicationType) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/configuration`,
        method: 'GET',
      })
      .flush(applicationType);
  }

  function expectListCertificatesRequest(certificates: ClientCertificate[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/certificates?page=1&size=100`,
        method: 'GET',
      })
      .flush({
        data: certificates,
        page: { current: 1, per_page: 100, size: certificates.length, total_elements: certificates.length, total_pages: 1 },
      });
  }
});

const waitImageCheck = () => new Promise(resolve => setTimeout(resolve, 1));
