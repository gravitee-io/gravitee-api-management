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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { ApplicationTabSettingsEditComponent } from './application-tab-settings-edit.component';
import { fakeApplication, fakeSimpleApplicationType } from '../../../../../entities/application/application.fixture';
import { fakeUserApplicationPermissions } from '../../../../../entities/permission/permission.fixtures';
import { ApplicationCertificateService } from '../../../../../services/application-certificate.service';
import { ApplicationService } from '../../../../../services/application.service';
import { ConfigService } from '../../../../../services/config.service';

describe('ApplicationTabSettingsEditComponent', () => {
  const APPLICATION_ID = 'test-app-id';
  let fixture: ComponentFixture<ApplicationTabSettingsEditComponent>;
  let component: ApplicationTabSettingsEditComponent;

  async function init(configuration: object) {
    await TestBed.configureTestingModule({
      imports: [ApplicationTabSettingsEditComponent, NoopAnimationsModule],
      providers: [
        { provide: ConfigService, useValue: { configuration } },
        { provide: ApplicationCertificateService, useValue: { list: jest.fn().mockReturnValue(of({ data: [] })) } },
        { provide: ApplicationService, useValue: { get: () => of(fakeApplication()), save: jest.fn() } },
        { provide: Router, useValue: { url: '/', navigate: jest.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationTabSettingsEditComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('applicationId', APPLICATION_ID);
    fixture.componentRef.setInput('applicationTypeConfiguration', fakeSimpleApplicationType());
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions());
    fixture.detectChanges();
    await fixture.whenStable();
  }

  describe('mtlsEnabled', () => {
    it('should return true when mtls is enabled', async () => {
      await init({ portalNext: { mtls: { enabled: true } } });

      expect((component as unknown as { mtlsEnabled: boolean }).mtlsEnabled).toBe(true);
    });

    it('should return false when mtls is disabled', async () => {
      await init({ portalNext: { mtls: { enabled: false } } });

      expect((component as unknown as { mtlsEnabled: boolean }).mtlsEnabled).toBe(false);
    });

    it('should return false when mtls config is absent', async () => {
      await init({});

      expect((component as unknown as { mtlsEnabled: boolean }).mtlsEnabled).toBe(false);
    });
  });
});
