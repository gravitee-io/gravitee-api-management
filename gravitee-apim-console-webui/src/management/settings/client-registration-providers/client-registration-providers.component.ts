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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { combineLatest, EMPTY, Observable, Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GioConfirmDialogComponent, GioConfirmDialogData, GioLicenseService, LicenseOptions } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';

import { ClientRegistrationProvidersService } from '../../../services-ngx/client-registration-providers.service';
import { ClientRegistrationProvider } from '../../../entities/client-registration-provider/clientRegistrationProvider';
import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { PortalSettings, PortalSettingsApplication } from '../../../entities/portal/portalSettings';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { ApimFeature } from '../../../shared/components/gio-license/gio-license-data';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

export type ProvidersTableDS = {
  id: string;
  name: string;
  description: string;
  updatedAt: number;
};

@Component({
  selector: 'client-registration-providers',
  templateUrl: './client-registration-providers.component.html',
  styleUrls: ['./client-registration-providers.component.scss'],
  standalone: false,
})
export class ClientRegistrationProvidersComponent implements OnInit, OnDestroy {
  applicationForm: UntypedFormGroup;
  providersTableDS: ProvidersTableDS[] = [];
  displayedColumns = ['name', 'description', 'updatedAt', 'actions'];
  isLoadingData = true;
  disabledMessage = 'Configuration provided by the system';
  dcrRegistrationLicenseOptions: LicenseOptions = { feature: ApimFeature.APIM_DCR_REGISTRATION };
  hasDcrRegistrationLock$: Observable<boolean>;
  canUpdateSettings: boolean;
  private unsubscribe$ = new Subject<void>();
  private settings: PortalSettings;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly clientRegistrationProvidersService: ClientRegistrationProvidersService,
    private readonly portalSettingsService: PortalSettingsService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    private readonly licenseService: GioLicenseService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }
  ngOnInit(): void {
    this.hasDcrRegistrationLock$ = this.licenseService.isMissingFeature$(this.dcrRegistrationLicenseOptions.feature);

    this.canUpdateSettings = this.permissionService.hasAnyMatching([
      'environment-settings-c',
      'environment-settings-u',
      'environment-settings-d',
    ]);
    if (!this.canUpdateSettings) {
      this.disabledMessage = undefined;
    }
    combineLatest([this.portalSettingsService.get(), this.clientRegistrationProvidersService.list()])
      .pipe(
        tap(([settings, providers]) => {
          this.settings = settings;
          this.providersTableDS = toProvidersTableDS(providers);

          this.initApplicationForm(settings.application);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.isLoadingData = false;
      });
  }

  onAddProvider() {
    return this.router.navigate(['new'], { relativeTo: this.activatedRoute });
  }

  onEditActionClicked(provider: ProvidersTableDS) {
    return this.router.navigate([provider.id], { relativeTo: this.activatedRoute });
  }

  onRemoveActionClicked(provider: ProvidersTableDS) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete client registration provider',
          content: `Are you sure you want to delete the client registration provider <strong>${provider.name}</strong> ?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'removeClientRegistrationProviderConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.clientRegistrationProvidersService.delete(provider.id)),
        tap(() => this.snackBarService.success(`"${provider.name}" has been deleted.`)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  initApplicationForm(application: PortalSettingsApplication) {
    this.applicationForm = new UntypedFormGroup({
      registration: new UntypedFormGroup({
        enabled: new UntypedFormControl({
          value: application.registration.enabled,
          disabled: this.isReadonly(`application.registration.enabled`) || !this.canUpdateSettings,
        }),
      }),
      types: new UntypedFormGroup({
        simple: new UntypedFormGroup({
          enabled: new UntypedFormControl({
            value: application.types.simple.enabled,
            disabled: this.isReadonly(`application.types.simple.enabled`) || !this.canUpdateSettings,
          }),
        }),
        browser: new UntypedFormGroup({
          enabled: new UntypedFormControl({
            value: application.types.browser.enabled,
            disabled: this.isReadonly(`application.types.browser.enabled`) || !this.canUpdateSettings,
          }),
        }),
        web: new UntypedFormGroup({
          enabled: new UntypedFormControl({
            value: application.types.web.enabled,
            disabled: this.isReadonly(`application.types.web.enabled`) || !this.canUpdateSettings,
          }),
        }),
        native: new UntypedFormGroup({
          enabled: new UntypedFormControl({
            value: application.types.native.enabled,
            disabled: this.isReadonly(`application.types.native.enabled`) || !this.canUpdateSettings,
          }),
        }),
        backend_to_backend: new UntypedFormGroup({
          enabled: new UntypedFormControl({
            value: application.types.backend_to_backend.enabled,
            disabled: this.isReadonly(`application.types.backend_to_backend.enabled`) || !this.canUpdateSettings,
          }),
        }),
      }),
    });

    this.applicationForm.valueChanges
      .pipe(
        switchMap((portalSettingsApplication: PortalSettingsApplication) => {
          this.settings.application = portalSettingsApplication;
          return this.portalSettingsService.save(this.settings);
        }),
        tap(() => this.snackBarService.success(`Configuration has been saved.`)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
  isReadonly(property: string): boolean {
    return PortalSettingsService.isReadonly(this.settings, property);
  }
}

const toProvidersTableDS = (providers: ClientRegistrationProvider[]): ProvidersTableDS[] => {
  return (providers || []).map((provider) => {
    return {
      id: provider.id,
      name: provider.name,
      description: provider.description,
      updatedAt: provider.updated_at,
    };
  });
};
