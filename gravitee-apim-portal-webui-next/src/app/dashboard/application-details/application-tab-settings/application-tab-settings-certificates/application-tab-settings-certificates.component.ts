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
import { DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { switchMap } from 'rxjs';
import { EMPTY } from 'rxjs/internal/observable/empty';

import { AddCertificateDialogComponent, AddCertificateDialogData } from './add-certificate-dialog/add-certificate-dialog.component';
import { ClientCertificate, CreateClientCertificateInput } from '../../../../../entities/application/client-certificate';
import { UserApplicationPermissions } from '../../../../../entities/permission/permission';
import { CapitalizeFirstPipe } from '../../../../../pipe/capitalize-first.pipe';
import { ApplicationCertificateService } from '../../../../../services/application-certificate.service';

export type CertificateTab = 'active' | 'history';

@Component({
  selector: 'app-application-tab-settings-certificates',
  imports: [DatePipe, CapitalizeFirstPipe, MatButtonModule, MatDialogModule, MatIconModule, MatTableModule],
  templateUrl: './application-tab-settings-certificates.component.html',
  styleUrl: './application-tab-settings-certificates.component.scss',
})
export class ApplicationTabSettingsCertificatesComponent implements OnInit {
  private readonly certService = inject(ApplicationCertificateService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);

  applicationId = input.required<string>();
  userApplicationPermissions = input.required<UserApplicationPermissions>();

  canManage = computed(() => this.userApplicationPermissions().DEFINITION?.includes('U') ?? false);

  certificates = signal<ClientCertificate[]>([]);
  activeTab = signal<CertificateTab>('active');

  activeCertificates = computed(() =>
    this.certificates().filter(c => c.status === 'ACTIVE' || c.status === 'ACTIVE_WITH_END' || c.status === 'SCHEDULED'),
  );
  historyCertificates = computed(() => this.certificates().filter(c => c.status === 'REVOKED'));

  displayedCertificates = computed(() => (this.activeTab() === 'active' ? this.activeCertificates() : this.historyCertificates()));

  displayedColumns = computed(() => {
    const base = ['name', 'createdAt', 'endsAt', 'status', 'daysRemaining'];
    return this.canManage() ? [...base, 'actions'] : base;
  });

  ngOnInit(): void {
    this.loadCertificates();
  }

  loadCertificates(): void {
    this.certService
      .list(this.applicationId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => this.certificates.set(res.data ?? []));
  }

  daysRemaining(endsAt: string | undefined): number | null {
    if (!endsAt) return null;
    return Math.ceil((new Date(endsAt).getTime() - Date.now()) / (1000 * 60 * 60 * 24));
  }

  openAddCertificateDialog(): void {
    this.dialog
      .open<AddCertificateDialogComponent, AddCertificateDialogData, CreateClientCertificateInput>(AddCertificateDialogComponent, {
        data: { hasActiveCertificates: this.activeCertificates().length > 0 },
      })
      .afterClosed()
      .pipe(
        switchMap(input => (input ? this.certService.create(this.applicationId(), input) : EMPTY)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({ next: () => this.loadCertificates(), error: err => console.error(err) });
  }
}
