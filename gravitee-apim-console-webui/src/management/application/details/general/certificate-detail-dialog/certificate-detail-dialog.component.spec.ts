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
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { CertificateDetailDialogComponent } from './certificate-detail-dialog.component';

import { ApplicationGeneralModule } from '../application-general.module';
import { ClientCertificate, ClientCertificateStatus } from '../../../../../entities/application/ClientCertificate';

describe('CertificateDetailDialogComponent', () => {
  const MOCK_CERTIFICATE: ClientCertificate = {
    id: 'cert-1',
    name: 'My Certificate',
    createdAt: '2026-01-15T10:00:00Z',
    certificateExpiration: '2027-01-15T10:00:00Z',
    endsAt: '2027-01-15T10:00:00Z',
    subject: 'CN=test-subject, O=Acme Corp',
    issuer: 'CN=test-issuer, O=Acme CA',
    status: ClientCertificateStatus.ACTIVE,
  };

  let fixture: ComponentFixture<CertificateDetailDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApplicationGeneralModule, MatIconTestingModule],
      providers: [
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
        { provide: MAT_DIALOG_DATA, useValue: MOCK_CERTIFICATE },
      ],
    });

    fixture = TestBed.createComponent(CertificateDetailDialogComponent);
    fixture.detectChanges();
  });

  it('should_display_certificate_name', () => {
    const el = fixture.nativeElement.querySelector('[data-testid="certificate-detail-name-value"]');
    expect(el.textContent.trim()).toBe('My Certificate');
  });

  it('should_display_subject', () => {
    const el = fixture.nativeElement.querySelector('[data-testid="certificate-detail-subject-value"]');
    expect(el.textContent.trim()).toBe('CN=test-subject, O=Acme Corp');
  });

  it('should_display_issuer', () => {
    const el = fixture.nativeElement.querySelector('[data-testid="certificate-detail-issuer-value"]');
    expect(el.textContent.trim()).toBe('CN=test-issuer, O=Acme CA');
  });

  it('should_display_expiry_information', () => {
    const el = fixture.nativeElement.querySelector('[data-testid="certificate-detail-expires-value"]');
    expect(el.textContent.trim()).toContain('2027');
  });
});
