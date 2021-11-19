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
import { combineLatest, Subject } from 'rxjs';
import { filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { StateService } from '@uirouter/core';

import { IdentityProviderService } from '../../../services-ngx/identity-provider.service';
import { IdentityProviderListItem, IdentityProviderActivation } from '../../../entities/identity-provider';
import { ConsoleSettingsService } from '../../../services-ngx/console-settings.service';
import { ConsoleSettings } from '../../../entities/consoleSettings';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { OrganizationService } from '../../../services-ngx/organization.service';
import {
  GioConfirmDialogComponent,
  GioConfirmDialogData,
} from '../../../shared/components/gio-confirm-dialog/gio-confirm-dialog.component';
import { UIRouterState } from '../../../ajs-upgraded-providers';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

type TableData = {
  logo: string;
  id: string;
  name: string;
  description: string;
  availableOnPortal: boolean;
  activated: boolean;
  sync: boolean;
  updatedAt: number;
};

@Component({
  selector: 'org-settings-identity-providers',
  styles: [require('./org-settings-identity-providers.component.scss')],
  template: require('./org-settings-identity-providers.component.html'),
})
export class OrgSettingsIdentityProvidersComponent implements OnInit, OnDestroy {
  providedConfigurationMessage = 'Configuration provided by the system';

  displayedColumns: string[] = ['logo', 'id', 'name', 'description', 'activated', 'sync', 'availableOnPortal', 'updatedAt', 'actions'];
  tableData: TableData[] = [];
  filteredTableData: TableData[] = [];

  consoleSettings: ConsoleSettings;

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly identityProviderService: IdentityProviderService,
    private readonly consoleSettingsService: ConsoleSettingsService,
    private readonly organizationService: OrganizationService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    @Inject(UIRouterState) private readonly $state: StateService,
  ) {}

  ngOnInit() {
    combineLatest([
      this.identityProviderService.list(),
      this.consoleSettingsService.get(),
      this.organizationService.listActivatedIdentityProviders(),
    ])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([identityProviders, consoleSettings, activatedIdentityProviders]) => {
        this.setDataSourceFromIdentityProviders(identityProviders, activatedIdentityProviders);
        this.consoleSettings = consoleSettings;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filteredTableData = gioTableFilterCollection(this.tableData, filters);
  }

  onDeleteActionClicked(identityProvider: TableData) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete an Identity Provider',
          content: `Are you sure you want to delete the identity provider <strong>${identityProvider.name}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteIdentityProviderConfirmDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.identityProviderService.delete(identityProvider.id)),
        tap(() => this.snackBarService.success(`Identity Provider ${identityProvider.name} successfully deleted!`)),
      )
      .subscribe(() => this.ngOnInit());
  }

  onEditActionClicked(identityProvider: TableData) {
    this.$state.go('organization.settings.ng-identityprovider-edit', { id: identityProvider.id });
  }

  onActivationToggleActionClicked(identityProvider: TableData) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: `${identityProvider.activated ? 'Deactivate' : 'Activate'} an Identity Provider`,
          content: `Are you sure you want to ${identityProvider.activated ? 'deactivate' : 'activate'} the identity provider <strong>${
            identityProvider.name
          }</strong>?`,
          confirmButton: 'Ok',
        },
        role: 'alertdialog',
        id: 'deleteIdentityProviderConfirmDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => {
          identityProvider.activated = !identityProvider.activated;
          const activatedIdps = this.tableData.filter((idp) => idp.activated === true).map((idp) => ({ identityProvider: idp.id }));
          return this.organizationService.updateActivatedIdentityProviders(activatedIdps);
        }),
        tap(() => this.snackBarService.success(`Identity Provider ${identityProvider.name} successfully deleted!`)),
      )
      .subscribe(() => this.ngOnInit());
  }

  updateLocalLogin(shouldActivateLoginForm: boolean): void {
    this.consoleSettingsService
      .save({
        ...this.consoleSettings,
        authentication: {
          ...this.consoleSettings.authentication,
          localLogin: {
            enabled: shouldActivateLoginForm,
          },
        },
      })
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(() => this.snackBarService.success('Configuration successfully updated!')),
      )
      .subscribe((updatedConsoleSettings) => {
        this.consoleSettings = updatedConsoleSettings;
      });
  }

  isReadonlySetting(property: string): boolean {
    return ConsoleSettingsService.isReadonly(this.consoleSettings, property);
  }

  hasActivatedIdp(): boolean {
    return this.tableData.some((idp) => idp.activated);
  }

  private setDataSourceFromIdentityProviders(
    identityProviders: IdentityProviderListItem[],
    activatedIdentityProviders: IdentityProviderActivation[],
  ) {
    const matTableData = identityProviders
      .map((idp) => ({
        logo: `assets/logo_${idp.type.toLowerCase()}-idp.svg`,
        id: idp.id,
        name: idp.name,
        description: idp.description,
        availableOnPortal: idp.enabled,
        activated: activatedIdentityProviders.some((activatedIdp) => activatedIdp.identityProvider === idp.id),
        sync: idp.sync,
        updatedAt: idp.updated_at,
      }))
      // Use a custom sort to always have Gravitee AM first
      .sort((idpA, idpB) => {
        if (idpA.id === 'gravitee-am') {
          return -1;
        } else if (idpB.id === 'gravitee-am') {
          return 1;
        }
        return idpA.id.localeCompare(idpB.id);
      });

    this.tableData = matTableData;
    this.filteredTableData = matTableData;
  }

  getLocalLoginTooltipMessage(): string | null {
    if (this.isReadonlySetting('console.authentication.localLogin.enabled')) {
      return this.providedConfigurationMessage;
    }
    if (!this.hasActivatedIdp()) {
      return 'You must create and activate an identity provider to be able to update this setting';
    }
    return null;
  }

  onAddIdpClicked(): void {
    this.$state.go('organization.settings.ng-identityprovider-new');
  }
}
