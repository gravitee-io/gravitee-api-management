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
import { Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatTabsModule } from '@angular/material/tabs';
import { differenceInCalendarDays } from 'date-fns';

import { PaginatedTableComponent, TableColumn } from '../../../../../components/paginated-table/paginated-table.component';
import { ClientCertificate } from '../../../../../entities/application/client-certificate';
import { UserApplicationPermissions } from '../../../../../entities/permission/permission';
import { ApplicationCertificateService } from '../../../../../services/application-certificate.service';

@Component({
  selector: 'app-application-tab-settings-certificates',
  imports: [MatTabsModule, PaginatedTableComponent],
  templateUrl: './application-tab-settings-certificates.component.html',
  styleUrl: './application-tab-settings-certificates.component.scss',
})
export class ApplicationTabSettingsCertificatesComponent implements OnInit {
  private readonly certService = inject(ApplicationCertificateService);
  private readonly destroyRef = inject(DestroyRef);

  applicationId = input.required<string>();
  userApplicationPermissions = input.required<UserApplicationPermissions>();

  canManage = computed(() => this.userApplicationPermissions().DEFINITION?.includes('U') ?? false);

  certificates = signal<ClientCertificate[]>([]);
  currentPage = signal(1);
  totalElements = signal(0);
  loadError = signal(false);
  readonly pageSize = 10;

  activeCertificates = computed(() =>
    this.certificates().filter(c => c.status === 'ACTIVE' || c.status === 'ACTIVE_WITH_END' || c.status === 'SCHEDULED'),
  );
  historyCertificates = computed(() => this.certificates().filter(c => c.status === 'REVOKED'));

  tableColumns: TableColumn[] = [
    { id: 'name', label: 'Name' },
    { id: 'createdAt', label: 'Uploaded', type: 'date' },
    { id: 'endsAt', label: 'Expiry date' },
    { id: 'status', label: 'Status' },
    { id: 'daysRemaining', label: 'Days Remaining' },
  ];

  activeRows = computed(() => this.toRows(this.activeCertificates()));
  historyRows = computed(() => this.toRows(this.historyCertificates()));

  ngOnInit(): void {
    this.loadCertificates();
  }

  loadCertificates(): void {
    this.certService
      .list(this.applicationId(), this.currentPage(), this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loadError.set(false);
          this.certificates.set(res.data ?? []);
          this.totalElements.set(res.metadata?.paginateMetaData?.totalElements ?? 0);
        },
        error: () => {
          this.loadError.set(true);
        },
      });
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadCertificates();
  }

  onTabChange(): void {
    this.currentPage.set(1);
    this.loadCertificates();
  }

  formatDaysRemaining(endsAt: string | undefined): string {
    if (!endsAt) return '—';
    const days = differenceInCalendarDays(new Date(endsAt), new Date());
    return days <= 0 ? 'Expired' : String(days);
  }

  private toRows(certs: ClientCertificate[]) {
    return certs.map(cert => ({
      id: cert.id,
      name: cert.name,
      createdAt: cert.createdAt ?? '',
      endsAt: cert.endsAt ? new Date(cert.endsAt).toLocaleDateString() : '—',
      status: this.formatStatus(cert.status),
      daysRemaining: this.formatDaysRemaining(cert.endsAt),
    }));
  }

  private formatStatus(status: string): string {
    return status.charAt(0) + status.slice(1).toLowerCase().replace(/_/g, ' ');
  }
}
