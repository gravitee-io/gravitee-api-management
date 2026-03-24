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
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';

import { ClientCertificate } from '../../../../entities/application/client-certificate';
import { UserApplicationPermissions } from '../../../../entities/permission/permission';
import { ApplicationCertificateService } from '../../../../services/application-certificate.service';

@Component({
  selector: 'app-application-tab-certificates',
  imports: [DatePipe, MatButtonModule, MatIconModule, MatTableModule],
  templateUrl: './application-tab-certificates.component.html',
})
export class ApplicationTabCertificatesComponent implements OnInit {
  private readonly certService = inject(ApplicationCertificateService);
  private readonly destroyRef = inject(DestroyRef);

  applicationId = input.required<string>();
  userApplicationPermissions = input.required<UserApplicationPermissions>();

  canManage = computed(() => this.userApplicationPermissions().DEFINITION?.includes('U') ?? false);

  certificates = signal<ClientCertificate[]>([]);
  displayedColumns = computed(() => {
    const base = ['name', 'status', 'endsAt', 'subject'];
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
}
