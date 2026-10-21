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
import { Component, DestroyRef, effect, HostListener, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { GioBannerModule, GioLoaderModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';
import { EMPTY } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { PortalCatalogTileSettings, PortalSettings } from '../../entities/portal/portalSettings';
import { PortalSettingsService } from '../../services-ngx/portal-settings.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { HasUnsavedChanges } from '../../shared/guards/has-unsaved-changes.guard';

interface CatalogTileForm {
  name: FormControl<boolean>;
  type: FormControl<boolean>;
  image: FormControl<boolean>;
  version: FormControl<boolean>;
  description: FormControl<boolean>;
  status: FormControl<boolean>;
  owner: FormControl<boolean>;
}

type CatalogTileFormValue = ReturnType<FormGroup<CatalogTileForm>['getRawValue']>;

const TILE_FIELDS: { key: keyof CatalogTileForm; label: string }[] = [
  { key: 'name', label: 'API name' },
  { key: 'type', label: 'API type' },
  { key: 'image', label: 'API image' },
  { key: 'version', label: 'API version' },
  { key: 'description', label: 'API description' },
  { key: 'status', label: 'API status' },
  { key: 'owner', label: 'API owner name' },
];

@Component({
  selector: 'portal-catalog-tile',
  imports: [
    GioBannerModule,
    GioLoaderModule,
    GioSaveBarModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    PortalHeaderComponent,
    ReactiveFormsModule,
  ],
  templateUrl: './catalog-tile.component.html',
  styleUrl: './catalog-tile.component.scss',
})
export class CatalogTileComponent implements HasUnsavedChanges {
  private readonly destroyRef = inject(DestroyRef);
  private readonly permissionService = inject(GioPermissionService);
  private readonly portalSettingsService = inject(PortalSettingsService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly canUpdate = this.permissionService.hasAnyMatching(['environment-settings-u']);

  readonly fields = TILE_FIELDS;
  readonly preview = {
    name: 'Payments API',
    type: 'API',
    version: '1.2.0',
    description: 'Process and reconcile payments across channels.',
    status: 'Started',
    owner: 'Alex Morgan',
  };

  private readonly currentSettings = signal<PortalSettings | null>(null);
  readonly tileForm = signal<FormGroup<CatalogTileForm> | null>(null);
  readonly formInitialValues = signal<CatalogTileFormValue | null>(null);

  readonly settingsResource = rxResource({
    stream: () => this.portalSettingsService.get(),
  });

  constructor() {
    effect(() => {
      if (this.settingsResource.hasValue()) {
        this.currentSettings.set(this.settingsResource.value());
      }
    });

    effect(() => {
      const settings = this.currentSettings();
      if (!settings || this.tileForm()) {
        return;
      }
      const form = this.createForm(settings.portalNext?.catalog?.tile);
      this.tileForm.set(form);
      this.formInitialValues.set(form.getRawValue());
    });
  }

  @HostListener('window:beforeunload', ['$event'])
  beforeUnloadHandler(event: BeforeUnloadEvent): string | void {
    if (this.hasUnsavedChanges()) {
      event.preventDefault();
      event.returnValue = '';
      return '';
    }
  }

  hasUnsavedChanges(): boolean {
    const form = this.tileForm();
    const initialValues = this.formInitialValues();
    return !!form && !!initialValues && !isEqual(form.getRawValue(), initialValues);
  }

  retry(): void {
    this.tileForm.set(null);
    this.settingsResource.reload();
  }

  reset(): void {
    const form = this.tileForm();
    const initialValues = this.formInitialValues();
    if (!form || !initialValues) {
      return;
    }
    form.reset(initialValues);
    form.markAsPristine();
  }

  save(): void {
    const form = this.tileForm();
    const settings = this.currentSettings();
    if (!form || !settings || !this.hasUnsavedChanges()) {
      return;
    }

    const payload = this.toSettingsPayload(settings, form.getRawValue());
    this.portalSettingsService
      .save(payload)
      .pipe(
        tap(savedSettings => {
          const next = savedSettings ?? payload;
          this.currentSettings.set(next);
          this.formInitialValues.set(form.getRawValue());
          form.markAsPristine();
          this.snackBarService.success('Catalog tile settings saved.');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'Unable to save catalog tile settings.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private createForm(tile?: PortalCatalogTileSettings): FormGroup<CatalogTileForm> {
    const enabled = (field?: { enabled?: boolean }) => field?.enabled !== false;
    const form = new FormGroup<CatalogTileForm>({
      name: new FormControl(enabled(tile?.name), { nonNullable: true }),
      type: new FormControl(enabled(tile?.type), { nonNullable: true }),
      image: new FormControl(enabled(tile?.image), { nonNullable: true }),
      version: new FormControl(enabled(tile?.version), { nonNullable: true }),
      description: new FormControl(enabled(tile?.description), { nonNullable: true }),
      status: new FormControl(enabled(tile?.status), { nonNullable: true }),
      owner: new FormControl(enabled(tile?.owner), { nonNullable: true }),
    });
    if (!this.canUpdate) {
      form.disable();
    }
    return form;
  }

  private toSettingsPayload(settings: PortalSettings, value: CatalogTileFormValue): PortalSettings {
    const tile: PortalCatalogTileSettings = {
      name: { enabled: value.name },
      type: { enabled: value.type },
      image: { enabled: value.image },
      version: { enabled: value.version },
      description: { enabled: value.description },
      status: { enabled: value.status },
      owner: { enabled: value.owner },
    };
    return {
      ...settings,
      email: settings.email
        ? {
            ...settings.email,
            from: unwrapQuotedSender(settings.email.from),
            brandedSenders: settings.email.brandedSenders?.map(sender => ({
              ...sender,
              from: unwrapQuotedSender(sender.from),
            })),
          }
        : settings.email,
      portalNext: {
        ...settings.portalNext,
        access: settings.portalNext?.access ?? { enabled: false },
        catalog: {
          ...settings.portalNext?.catalog,
          tile,
        },
      },
    };
  }
}

/**
 * The settings API rejects a sender wrapped in extra quotes (`"user@my.domain"`).
 * Catalog save posts the whole settings document, so those quotes must be removed
 * or the unrelated email field blocks the tile update.
 */
function unwrapQuotedSender(value: string): string {
  const trimmed = value?.trim();
  if (trimmed && trimmed.length > 2 && trimmed.startsWith('"') && trimmed.endsWith('"')) {
    const inner = trimmed.slice(1, -1).trim();
    if (inner && !inner.includes('"')) {
      return inner;
    }
  }
  return value;
}
