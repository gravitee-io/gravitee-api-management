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
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { switchMap } from 'rxjs';

import { CopyCodeIconComponent } from '../../../../../components/copy-code/copy-code-icon/copy-code-icon/copy-code-icon.component';
import { Application, ApplicationType } from '../../../../../entities/application/application';
import { ApplicationService } from '../../../../../services/application.service';
import { ObservabilityBreakpointService } from '../../../../../services/observability-breakpoint.service';

@Component({
  selector: 'app-application-tab-settings-read',
  imports: [DatePipe, MatButtonModule, MatCardModule, MatChipsModule, CopyCodeIconComponent],
  templateUrl: './application-tab-settings-read.component.html',
  styleUrl: './application-tab-settings-read.component.scss',
})
export class ApplicationTabSettingsReadComponent {
  applicationId = input.required<string>();
  applicationTypeConfiguration = input.required<ApplicationType>();
  canUpdate = input<boolean>(false);

  editClicked = output<void>();
  clientSecretVisible = signal(false);

  protected readonly isNarrow = inject(ObservabilityBreakpointService).isNarrow;

  private applicationService = inject(ApplicationService);

  private application = toSignal(toObservable(this.applicationId).pipe(switchMap(id => this.applicationService.get(id))));

  readOnlyValues = computed(() => buildReadOnlyValues(this.application(), this.applicationTypeConfiguration()));

  getRelativeTime(dateString: string | undefined): string {
    if (!dateString) return '';
    const diff = Date.now() - new Date(dateString).getTime();
    const seconds = Math.floor(diff / 1000);
    if (seconds < 60) return $localize`:@@secondsAgoRelativeTime:${seconds}:seconds: seconds ago`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return $localize`:@@minutesAgoRelativeTime:${minutes}:minutes: minutes ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return $localize`:@@hoursAgoRelativeTime:${hours}:hours: hours ago`;
    const days = Math.floor(hours / 24);
    return $localize`:@@daysAgoRelativeTime:${days}:days: days ago`;
  }
}

function buildReadOnlyValues(app: Application | undefined, typeConfig: ApplicationType) {
  if (!app) {
    return {
      name: '',
      ownerDisplayName: undefined,
      type: '',
      securityType: '',
      createdAt: undefined,
      updatedAt: undefined,
      domain: undefined,
      description: undefined,
      grantTypes: undefined,
      clientId: undefined,
      clientSecret: undefined,
      redirectUris: undefined,
    };
  }

  const hasOAuth = !!app.settings.oauth;
  const grantTypes = hasOAuth
    ? (app.settings.oauth!.grant_types ?? [])
        .map(type => typeConfig.allowed_grant_types?.find(g => g.type === type)?.name ?? type)
        .join(', ') || undefined
    : undefined;

  return {
    name: app.name,
    ownerDisplayName: app.owner?.display_name,
    type: typeConfig.name,
    securityType: typeConfig.name,
    createdAt: app.created_at,
    updatedAt: app.updated_at,
    domain: app.domain,
    description: app.description,
    grantTypes,
    clientId: hasOAuth && !typeConfig.requires_redirect_uris ? app.settings.oauth!.client_id : undefined,
    clientSecret: hasOAuth && !typeConfig.requires_redirect_uris ? app.settings.oauth!.client_secret : undefined,
    redirectUris: typeConfig.requires_redirect_uris ? app.settings.oauth?.redirect_uris?.join(', ') : undefined,
  };
}
