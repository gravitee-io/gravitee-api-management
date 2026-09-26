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
import { Component, computed, DestroyRef, effect, HostListener, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  GioBannerModule,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioFormSlideToggleModule,
  GioLoaderModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';
import { combineLatest, EMPTY } from 'rxjs';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { IdentityProviderActivation, IdentityProviderListItem } from '../../entities/identity-provider';
import { PortalSettings } from '../../entities/portal/portalSettings';
import { EnvironmentIdentityProviderService } from '../../services-ngx/environment-identity-provider.service';
import { IdentityProviderService } from '../../services-ngx/identity-provider.service';
import { PortalSettingsService } from '../../services-ngx/portal-settings.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { GioTableWrapperFilters } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { gioTableFilterCollection } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { HasUnsavedChanges } from '../../shared/guards/has-unsaved-changes.guard';

const FORCE_LOGIN_PROPERTY = 'portal.authentication.forceLogin.enabled';
const LOCAL_LOGIN_PROPERTY = 'portal.authentication.localLogin.enabled';

interface AuthenticationForm {
  forceLogin: FormControl<boolean>;
  localLogin: FormControl<boolean>;
}

type AuthenticationFormValue = ReturnType<FormGroup<AuthenticationForm>['getRawValue']>;

interface AuthenticationData {
  identityProviders: IdentityProviderListItem[];
  activations: IdentityProviderActivation[];
  settings: PortalSettings;
}

interface IdentityProviderRow {
  id: string;
  name: string;
  description: string;
  logo: string;
  isActivated: boolean;
  isActivationDisabled: boolean;
  activationTooltip: string;
}

@Component({
  selector: 'portal-authentication',
  imports: [
    GioBannerModule,
    GioFormSlideToggleModule,
    GioLoaderModule,
    GioSaveBarModule,
    GioTableWrapperModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatSlideToggleModule,
    MatTableModule,
    MatTooltipModule,
    PortalHeaderComponent,
    ReactiveFormsModule,
  ],
  templateUrl: './portal-authentication.component.html',
  styleUrl: './portal-authentication.component.scss',
})
export class PortalAuthenticationComponent implements HasUnsavedChanges {
  private readonly destroyRef = inject(DestroyRef);
  private readonly matDialog = inject(MatDialog);
  private readonly permissionService = inject(GioPermissionService);
  private readonly identityProviderService = inject(IdentityProviderService);
  private readonly environmentIdentityProviderService = inject(EnvironmentIdentityProviderService);
  private readonly portalSettingsService = inject(PortalSettingsService);
  private readonly snackBarService = inject(SnackBarService);

  private readonly canUpdateSettings = this.permissionService.hasAnyMatching([
    'environment-settings-c',
    'environment-settings-u',
    'environment-settings-d',
  ]);
  readonly canUpdateActivation = this.permissionService.hasAnyMatching(['environment-identity_provider_activation-u']);
  readonly displayedColumns = ['logo', 'id', 'name', 'description', 'actions'];
  // A single form instance: gio-form-slide-toggle only listens to the status of the control it was first bound to
  readonly authenticationForm = new FormGroup<AuthenticationForm>({
    forceLogin: new FormControl(false, { nonNullable: true }),
    localLogin: new FormControl(true, { nonNullable: true }),
  });
  readonly formInitialValues = signal<AuthenticationFormValue | null>(null);
  readonly filters = signal<GioTableWrapperFilters>({ pagination: { index: 1, size: 10 }, searchTerm: '' });

  readonly dataResource = rxResource<AuthenticationData, void>({
    stream: () =>
      combineLatest([
        this.identityProviderService.list(),
        this.environmentIdentityProviderService.list(),
        this.portalSettingsService.get(),
      ]).pipe(map(([identityProviders, activations, settings]) => ({ identityProviders, activations, settings }))),
  });
  readonly isForceLoginReadonly = computed(() => this.isSystemReadonly(FORCE_LOGIN_PROPERTY));
  readonly isLocalLoginReadonly = computed(() => this.isSystemReadonly(LOCAL_LOGIN_PROPERTY));
  private readonly rows = computed<IdentityProviderRow[]>(() => {
    if (!this.dataResource.hasValue()) {
      return [];
    }
    const { identityProviders, activations } = this.dataResource.value();
    return identityProviders.map(identityProvider =>
      toIdentityProviderRow(
        identityProvider,
        activations.some(activation => activation.identityProvider === identityProvider.id),
      ),
    );
  });
  private readonly filteredRows = computed(() =>
    gioTableFilterCollection(this.rows(), this.filters(), {
      searchTermIgnoreKeys: ['logo', 'isActivated', 'isActivationDisabled', 'activationTooltip'],
    }),
  );
  readonly pagedRows = computed(() => this.filteredRows().filteredCollection);
  readonly total = computed(() => this.filteredRows().unpaginatedLength);

  constructor() {
    effect(() => {
      if (!this.dataResource.hasValue()) {
        return;
      }
      const { identityProviders, activations, settings } = this.dataResource.value();
      const portalIdentityProviderIds = identityProviders.filter(identityProvider => identityProvider.enabled).map(({ id }) => id);
      const hasPortalLoginProvider = activations.some(activation => portalIdentityProviderIds.includes(activation.identityProvider));
      this.applySettings(settings, hasPortalLoginProvider);
      this.formInitialValues.set(this.authenticationForm.getRawValue());

      // Without any identity provider usable on the portal, the login form is the only way to log in to it
      if (
        !hasPortalLoginProvider &&
        !this.authenticationForm.controls.localLogin.value &&
        this.canUpdateSettings &&
        !this.isLocalLoginReadonly()
      ) {
        this.authenticationForm.controls.localLogin.setValue(true);
        this.saveSettings(this.authenticationForm.getRawValue());
      }
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
    const initialValues = this.formInitialValues();
    return !!initialValues && !isEqual(this.authenticationForm.getRawValue(), initialValues);
  }

  retry(): void {
    this.dataResource.reload();
  }

  reset(): void {
    const initialValues = this.formInitialValues();
    if (!initialValues) {
      return;
    }
    this.authenticationForm.reset(initialValues);
  }

  save(): void {
    if (!this.hasUnsavedChanges()) {
      return;
    }
    this.saveSettings(this.authenticationForm.getRawValue());
  }

  onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters.set(filters);
  }

  toggleActivation(row: IdentityProviderRow): void {
    const action = row.isActivated ? 'deactivate' : 'activate';
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: `${row.isActivated ? 'Deactivate' : 'Activate'} an identity provider`,
          content: `Are you sure you want to ${action} the identity provider <strong>${row.name}</strong> for the portal?`,
          confirmButton: row.isActivated ? 'Deactivate' : 'Activate',
        },
        role: 'alertdialog',
        id: 'portalIdentityProviderActivationConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.environmentIdentityProviderService.update(this.activationsAfterToggle(row))),
        tap(() => this.snackBarService.success(`Identity provider ${row.name} successfully ${action}d.`)),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? `Unable to ${action} the identity provider ${row.name}.`);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.dataResource.reload());
  }

  private activationsAfterToggle(row: IdentityProviderRow): IdentityProviderActivation[] {
    const activatedIds = this.dataResource
      .value()
      .activations.map(activation => activation.identityProvider)
      .filter(id => id !== row.id);
    const ids = row.isActivated ? activatedIds : [...activatedIds, row.id];
    return ids.map(identityProvider => ({ identityProvider }));
  }

  private saveSettings(formValue: AuthenticationFormValue): void {
    // Settings are fetched again to not override changes made meanwhile on other portal settings
    this.portalSettingsService
      .get()
      .pipe(
        switchMap(settings =>
          this.portalSettingsService.save({
            ...settings,
            authentication: {
              ...settings.authentication,
              forceLogin: { ...settings.authentication?.forceLogin, enabled: formValue.forceLogin },
              localLogin: { ...settings.authentication?.localLogin, enabled: formValue.localLogin },
            },
          }),
        ),
        tap(() => this.snackBarService.success('Authentication configuration successfully updated.')),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'Unable to update the authentication configuration.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.dataResource.reload());
  }

  private isSystemReadonly(property: string): boolean {
    return this.dataResource.hasValue() && PortalSettingsService.isReadonly(this.dataResource.value().settings, property);
  }

  private applySettings(settings: PortalSettings, hasPortalLoginProvider: boolean): void {
    const { forceLogin, localLogin } = this.authenticationForm.controls;
    setDisabled(forceLogin, !this.canUpdateSettings || PortalSettingsService.isReadonly(settings, FORCE_LOGIN_PROPERTY));
    setDisabled(
      localLogin,
      !this.canUpdateSettings || PortalSettingsService.isReadonly(settings, LOCAL_LOGIN_PROPERTY) || !hasPortalLoginProvider,
    );
    this.authenticationForm.reset({
      forceLogin: settings.authentication?.forceLogin?.enabled ?? false,
      localLogin: settings.authentication?.localLogin?.enabled ?? true,
    });
  }
}

// An activation that is not allowed on the portal has no effect there, but it can still be removed
function toIdentityProviderRow(identityProvider: IdentityProviderListItem, isActivated: boolean): IdentityProviderRow {
  const isAllowedOnPortal = identityProvider.enabled;
  let activationTooltip = isActivated ? 'Deactivate identity provider' : 'Activate identity provider';
  if (!isAllowedOnPortal) {
    activationTooltip = isActivated
      ? 'Not allowed for portal authentication, so not displayed on the portal. Deactivate it, or enable it in Platform → Authentication.'
      : 'Not allowed for portal authentication. Enable it in Platform → Authentication.';
  }
  return {
    id: identityProvider.id,
    name: identityProvider.name,
    description: identityProvider.description,
    logo: `assets/logo_${identityProvider.type.toLowerCase()}-idp.svg`,
    isActivated,
    isActivationDisabled: !isAllowedOnPortal && !isActivated,
    activationTooltip,
  };
}

function setDisabled(control: FormControl<boolean>, isDisabled: boolean): void {
  if (isDisabled) {
    control.disable();
  } else {
    control.enable();
  }
}
