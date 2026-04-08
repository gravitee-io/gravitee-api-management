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
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { differenceInCalendarDays } from 'date-fns';
import { EMPTY, map, Observable, of, switchMap, tap } from 'rxjs';

import { AddCertificateDialogComponent, AddCertificateDialogData } from './add-certificate-dialog/add-certificate-dialog.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../../components/confirm-dialog/confirm-dialog.component';
import { PaginatedTableComponent, TableAction, TableColumn } from '../../../../../components/paginated-table/paginated-table.component';
import { ClientCertificate } from '../../../../../entities/application/client-certificate';
import { UserApplicationPermissions } from '../../../../../entities/permission/permission';
import { ApplicationCertificateService } from '../../../../../services/application-certificate.service';

interface CertificateTableRow {
  certificate: ClientCertificate;
  id: string;
  name: string;
  createdAt: string;
  endsAt: string;
  status: string;
  daysRemaining: string;
}

@Component({
  selector: 'app-application-tab-settings-certificates',
  imports: [MatTabsModule, MatButtonModule, MatIconModule, PaginatedTableComponent],
  templateUrl: './application-tab-settings-certificates.component.html',
  styleUrl: './application-tab-settings-certificates.component.scss',
})
export class ApplicationTabSettingsCertificatesComponent implements OnInit {
  private readonly certService = inject(ApplicationCertificateService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  applicationId = input.required<string>();
  userApplicationPermissions = input.required<UserApplicationPermissions>();

  canManage = computed(() => this.userApplicationPermissions().DEFINITION?.includes('U') ?? false);

  certificates = signal<ClientCertificate[]>([]);
  currentPage = signal(1);
  totalElements = signal(0);
  error = signal<string | null>(null);
  readonly pageSize = 10;

  activeCertificates = computed(() => this.certificates().filter(cert => this.isActiveCertificate(cert)));
  historyCertificates = computed(() => this.certificates().filter(c => c.status === 'REVOKED'));
  activeRows = computed(() => this.activeCertificates().map(cert => this.toTableRow(cert)));
  historyRows = computed(() => this.historyCertificates().map(cert => this.toTableRow(cert)));

  tableColumns: TableColumn[] = [
    { id: 'name', label: 'Name' },
    { id: 'createdAt', label: 'Uploaded', type: 'date' },
    { id: 'endsAt', label: 'Expiry date' },
    { id: 'status', label: 'Status' },
    { id: 'daysRemaining', label: 'Days Remaining' },
  ];

  activeActions: TableAction<CertificateTableRow>[] = [
    {
      id: 'delete-certificate-button',
      icon: 'delete',
      ariaLabel: $localize`:@@deleteCertificateAriaLabel:Delete certificate`,
      color: 'warn',
    },
  ];

  historyActions: TableAction<CertificateTableRow>[] = [
    {
      id: 'delete-history-certificate-button',
      icon: 'delete',
      ariaLabel: $localize`:@@deleteCertificateAriaLabel:Delete certificate`,
      color: 'warn',
    },
  ];

  ngOnInit(): void {
    this.loadCertificates();
  }

  loadCertificates(): void {
    this.certService
      .list(this.applicationId(), this.currentPage(), this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.error.set(null);
          this.certificates.set(res.data ?? []);
          this.totalElements.set(res.metadata?.paginateMetaData?.totalElements ?? 0);
        },
        error: () => {
          this.error.set($localize`:@@certificatesLoadError:Failed to load certificates. Please try again.`);
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

  openUploadDialog(): void {
    const activeCerts = this.activeCertificates();
    const activeCert = activeCerts[0];
    const data: AddCertificateDialogData = {
      applicationId: this.applicationId(),
      hasActiveCertificates: activeCerts.length > 0,
      activeCertificateId: activeCert?.id,
      activeCertificateName: activeCert?.name,
      activeCertificateExpiration: activeCert?.endsAt,
    };
    this.dialog
      .open(AddCertificateDialogComponent, { data, width: '560px' })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(needsRefresh => {
        if (needsRefresh) this.loadCertificates();
      });
  }

  deleteCertificate(cert: ClientCertificate): void {
    this.error.set(null);

    this.openConfirmDialog(this.buildDeleteDialogData(cert), 'confirmCertificateDeleteDialog')
      .pipe(
        switchMap(confirmed => (confirmed ? this.loadCertificatesForDeleteCheck(cert) : EMPTY)),
        switchMap(certificates => this.confirmAndDeleteCertificate(cert, certificates)),
        tap(() => this.loadCertificates()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        error: () => {
          this.error.set($localize`:@@deleteCertificateError:An error occurred while deleting the certificate. Please try again`);
        },
      });
  }

  onActiveAction(event: { actionId: string; row: CertificateTableRow }): void {
    this.handleCertificateAction(event);
  }

  onHistoryAction(event: { actionId: string; row: CertificateTableRow }): void {
    this.handleCertificateAction(event);
  }

  private formatEndsAtDisplay(endsAt: string | undefined): string {
    return endsAt ? new Date(endsAt).toLocaleDateString() : '—';
  }

  private formatStatus(status: string): string {
    return status.charAt(0) + status.slice(1).toLowerCase().replace(/_/g, ' ');
  }

  private openConfirmDialog(dialogData: ConfirmDialogData, id: string): Observable<boolean | undefined> {
    return this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        role: 'alertdialog',
        id,
        data: dialogData,
      })
      .afterClosed();
  }

  private buildDeleteDialogData(cert: ClientCertificate): ConfirmDialogData {
    return {
      title: $localize`:@@titleDeleteCertificateDialog:Delete certificate`,
      content: $localize`:@@contentDeleteCertificateDialog:Are you sure you want to delete the certificate "${cert.name}:certificateName:"?`,
      confirmLabel: $localize`:@@confirmDeleteCertificateDialog:Delete`,
      cancelLabel: $localize`:@@cancelDeleteCertificateDialog:Cancel`,
    };
  }

  private buildLastActiveWarningDialogData(): ConfirmDialogData {
    return {
      title: $localize`:@@titleDeleteLastCertificateDialog:Warning`,
      content: $localize`:@@contentDeleteLastCertificateDialog:There is no active certificate in case you proceed with the deletion. Do you want to proceed?`,
      confirmLabel: $localize`:@@confirmDeleteCertificateDialog:Delete`,
      cancelLabel: $localize`:@@cancelDeleteCertificateDialog:Cancel`,
    };
  }

  private loadCertificatesForDeleteCheck(cert: ClientCertificate): Observable<ClientCertificate[]> {
    if (!this.isActiveCertificate(cert)) {
      return of(this.certificates());
    }

    const currentPageActiveCertificates = this.certificates().filter(candidate => this.isActiveCertificate(candidate));
    if (currentPageActiveCertificates.some(candidate => candidate.id !== cert.id)) {
      return of(this.certificates());
    }

    const pageSize = Math.max(this.totalElements(), this.pageSize);

    return this.certService.list(this.applicationId(), 1, pageSize).pipe(map(response => response.data ?? []));
  }

  private handleCertificateAction(event: { actionId: string; row: CertificateTableRow }): void {
    if (event.actionId === 'delete-certificate-button' || event.actionId === 'delete-history-certificate-button') {
      this.deleteCertificate(event.row.certificate);
    }
  }

  private confirmAndDeleteCertificate(cert: ClientCertificate, certificates: ClientCertificate[]): Observable<unknown> {
    if (!this.isLastActiveCertificate(cert, certificates)) {
      return this.certService.delete(this.applicationId(), cert.id);
    }

    return this.openConfirmDialog(this.buildLastActiveWarningDialogData(), 'confirmLastActiveCertificateDeleteDialog').pipe(
      switchMap(confirmed => (confirmed ? this.certService.delete(this.applicationId(), cert.id) : EMPTY)),
    );
  }

  private isLastActiveCertificate(cert: ClientCertificate, certificates: ClientCertificate[]): boolean {
    const activeCertificates = certificates.filter(candidate => this.isActiveCertificate(candidate));
    return activeCertificates.length === 1 && activeCertificates[0].id === cert.id;
  }

  private isActiveCertificate(cert: ClientCertificate): boolean {
    return cert.status === 'ACTIVE' || cert.status === 'ACTIVE_WITH_END' || cert.status === 'SCHEDULED';
  }

  private toTableRow(cert: ClientCertificate): CertificateTableRow {
    return {
      certificate: cert,
      id: cert.id,
      name: cert.name,
      createdAt: cert.createdAt ?? '',
      endsAt: this.formatEndsAtDisplay(cert.endsAt),
      status: this.formatStatus(cert.status),
      daysRemaining: this.formatDaysRemaining(cert.endsAt),
    };
  }
}
