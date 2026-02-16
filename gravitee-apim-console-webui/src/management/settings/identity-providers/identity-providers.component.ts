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
import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';

import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { IdentityProviderService } from '../../../services-ngx/identity-provider.service';
import { IdentityProvider, IdentityProviderActivation } from '../../../entities/identity-provider';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { PortalSettings } from '../../../entities/portal/portalSettings';
import { EnvironmentIdentityProviderService } from '../../../services-ngx/environment-identity-provider.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

interface IdentityProviderForm {
  forceLogin: FormGroup<{
    enabled: FormControl<boolean>;
  }>;
  localLogin: FormGroup<{
    enabled: FormControl<boolean>;
  }>;
}

@Component({
  selector: 'identity-providers',
  templateUrl: './identity-providers.component.html',
  styleUrls: ['./identity-providers.component.scss'],
  standalone: false,
})
export class IdentityProvidersComponent implements OnInit {
  identityProvidersForm: FormGroup;
  settings: PortalSettings;
  activatedIdentityProvider: IdentityProviderActivation[] = [];
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public formInitialValues: unknown;
  public identityProviderListTable: IdentityProvider[] = [];
  public isLoadingData = true;
  public displayedColumns = ['logo', 'id', 'name', 'description', 'actions'];
  public filteredIdentityProviderSettingsTable: IdentityProvider[] = [];
  public identityProviderUnpaginatedLength = 0;
  public initialFilters: GioTableWrapperFilters = {
    pagination: {
      size: 10,
      index: 1,
    },
    searchTerm: '',
  };

  constructor(
    private readonly portalSettingsService: PortalSettingsService,
    private readonly snackBarService: SnackBarService,
    private readonly identityProviderService: IdentityProviderService,
    private readonly environmentIdentityProviderService: EnvironmentIdentityProviderService,
    private readonly matDialog: MatDialog,
    private readonly permissionService: GioPermissionService,
  ) {}

  public ngOnInit() {
    this.isLoadingData = true;

    combineLatest([this.identityProviderService.list(), this.environmentIdentityProviderService.list(), this.portalSettingsService.get()])
      .pipe(
        tap(([identityProvider, activatedIdentityProvider, portalSettings]) => {
          this.activatedIdentityProvider = activatedIdentityProvider.map(activated => ({ identityProvider: activated.identityProvider }));
          this.identityProviderListTable = identityProvider.map(identityProvider => {
            const matchedId = activatedIdentityProvider.find(item => item.identityProvider === identityProvider.id);
            return {
              ...identityProvider,
              isActivated: !!matchedId,
              logo: `assets/logo_${identityProvider.type.toLowerCase()}-idp.svg`,
            };
          });
          this.onPropertiesFiltersChanged(this.initialFilters);
          this.settings = portalSettings;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.isLoadingData = false;
        this.initIdentityProvidersForm();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    this.portalSettingsService
      .get()
      .pipe(
        switchMap(settings =>
          this.portalSettingsService.save({
            ...settings,
            authentication: {
              ...settings.authentication,
              forceLogin: {
                ...settings.authentication.forceLogin,
                enabled: this.identityProvidersForm.get('forceLogin.enabled').value,
              },
              localLogin: {
                ...settings.authentication.localLogin,
                enabled: this.identityProvidersForm.get('localLogin.enabled').value,
              },
            },
          }),
        ),
        tap(() => {
          this.snackBarService.success('Authentication configuration successfully updated!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.ngOnInit();
      });
  }

  onPropertiesFiltersChanged(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.identityProviderListTable, filters);
    this.filteredIdentityProviderSettingsTable = filtered.filteredCollection;
    this.identityProviderUnpaginatedLength = filtered.unpaginatedLength;
  }

  onActivationToggleActionClicked(element) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: `${element.isActivated ? 'Deactivate' : 'Activate'} an Identity Provider`,
          content: `Are you sure you want to ${element.activated ? 'deactivate' : 'activate'} the identity provider <strong>${
            element.name
          }</strong>?`,
          confirmButton: 'Ok',
        },
        role: 'alertdialog',
        id: 'activateIdentityProviderConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        tap(() => {
          if (element.isActivated) {
            this.activatedIdentityProvider = this.activatedIdentityProvider.filter(obj => obj.identityProvider !== element.id);
          } else {
            this.activatedIdentityProvider.push({
              identityProvider: element.id,
            });
          }
        }),
        switchMap(() => {
          return this.environmentIdentityProviderService.update(this.activatedIdentityProvider);
        }),
        tap(() =>
          this.snackBarService.success(
            `Identity Provider ${element.name} successfully ${element.isActivated ? 'deactivated' : 'activated'}!`,
          ),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  initIdentityProvidersForm() {
    this.identityProvidersForm = new FormGroup<IdentityProviderForm>({
      forceLogin: new FormGroup({
        enabled: new FormControl({
          value: this.settings.authentication.forceLogin.enabled,
          disabled:
            this.isReadonly('portal.authentication.forceLogin.enabled') ||
            !this.permissionService.hasAnyMatching(['environment-settings-c', 'environment-settings-u', 'environment-settings-d']),
        }),
      }),
      localLogin: new FormGroup({
        enabled: new FormControl({
          value: this.settings.authentication.localLogin.enabled,
          disabled:
            this.isReadonly('portal.authentication.localLogin.enabled') ||
            !this.permissionService.hasAnyMatching(['environment-settings-c', 'environment-settings-u', 'environment-settings-d']) ||
            this.activatedIdentityProvider.length === 0,
        }),
      }),
    });
    this.formInitialValues = this.identityProvidersForm.getRawValue();

    if (this.activatedIdentityProvider.length === 0 && !this.identityProvidersForm.get('localLogin.enabled').value) {
      this.identityProvidersForm.get('localLogin.enabled').setValue(true);
      this.onSubmit();
    }
  }

  isReadonly(property: string): boolean {
    return PortalSettingsService.isReadonly(this.settings, property);
  }
}
