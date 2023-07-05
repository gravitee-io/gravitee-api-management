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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { FormControl, FormGroup } from '@angular/forms';
import { combineLatest, EMPTY, Observable, Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';

import { UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { Constants } from '../../../entities/Constants';
import { ClientRegistrationProvidersService } from '../../../services-ngx/client-registration-providers.service';
import { ClientRegistrationProvider } from '../../../entities/client-registration-provider/clientRegistrationProvider';
import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { PortalSettings, PortalSettingsApplication } from '../../../entities/portal/portalSettings';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioLicenseService } from '../../../shared/components/gio-license/gio-license.service';

export type ProvidersTableDS = {
  id: string;
  name: string;
  description: string;
  updatedAt: number;
}[];

@Component({
  selector: 'client-registration-providers',
  template: require('./client-registration-providers.component.html'),
  styles: [require('./client-registration-providers.component.scss')],
})
export class ClientRegistrationProvidersComponent implements OnInit, OnDestroy {
  applicationForm: FormGroup;
  providersTableDS: ProvidersTableDS = [];
  displayedColumns = ['name', 'description', 'updatedAt', 'actions'];
  isLoadingData = true;
  disabledMessage = 'Configuration provided by the system';
  dcrRegistrationLicense = { feature: 'apim-dcr-registration', utmMedium: 'feature_dcr' };
  hasDcrRegistrationLock$: Observable<boolean>;
  private unsubscribe$ = new Subject();
  private settings: PortalSettings;

  constructor(
    @Inject(UIRouterStateParams) private ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject('Constants') private readonly constants: Constants,
    private readonly clientRegistrationProvidersService: ClientRegistrationProvidersService,
    private readonly portalSettingsService: PortalSettingsService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    private readonly licenseService: GioLicenseService,
  ) {}

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }
  ngOnInit(): void {
    this.hasDcrRegistrationLock$ = this.licenseService.notAllowed(this.dcrRegistrationLicense.feature);

    combineLatest([this.portalSettingsService.get(), this.clientRegistrationProvidersService.list()])
      .pipe(
        tap(([settings, providers]) => {
          this.settings = settings;
          this.providersTableDS = ClientRegistrationProvidersComponent.toProvidersTableDS(providers);
          this.initApplicationForm(settings.application);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.isLoadingData = false;
      });
  }

  static toProvidersTableDS(providers: ClientRegistrationProvider[]): ProvidersTableDS {
    return (providers || []).map((provider) => {
      return {
        id: provider.id,
        name: provider.name,
        description: provider.description,
        updatedAt: provider.updated_at,
      };
    });
  }
  onAddProvider() {
    this.ajsState.go('management.settings.clientregistrationproviders.create');
  }

  onEditActionClicked(provider) {
    this.ajsState.go('management.settings.clientregistrationproviders.clientregistrationprovider', { id: provider.id });
  }

  onRemoveActionClicked(provider) {
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
        tap(() => this.snackBarService.success(`"${provider.name}" has been deleted"`)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  initApplicationForm(application: PortalSettingsApplication) {
    this.applicationForm = new FormGroup({
      registration: new FormGroup({
        enabled: new FormControl({
          value: application.registration.enabled,
          disabled: this.isReadonly(`application.registration.enabled`),
        }),
      }),
      types: new FormGroup({
        simple: new FormGroup({
          enabled: new FormControl({
            value: application.types.simple.enabled,
            disabled: this.isReadonly(`application.types.simple.enabled`),
          }),
        }),
        browser: new FormGroup({
          enabled: new FormControl({
            value: application.types.browser.enabled,
            disabled: this.isReadonly(`application.types.browser.enabled`),
          }),
        }),
        web: new FormGroup({
          enabled: new FormControl({
            value: application.types.web.enabled,
            disabled: this.isReadonly(`application.types.web.enabled`),
          }),
        }),
        native: new FormGroup({
          enabled: new FormControl({
            value: application.types.native.enabled,
            disabled: this.isReadonly(`application.types.native.enabled`),
          }),
        }),
        backend_to_backend: new FormGroup({
          enabled: new FormControl({
            value: application.types.backend_to_backend.enabled,
            disabled: this.isReadonly(`application.types.backend_to_backend.enabled`),
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
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
  isReadonly(property: string): boolean {
    return PortalSettingsService.isReadonly(this.settings, property);
  }
}
