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
import { MatTableDataSource } from '@angular/material/table';
import { combineLatest, Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { IdentityProviderService } from '../../../services-ngx/identity-provider.service';
import { IdentityProvider } from '../../../entities/identity-provider/identityProvider';
import { IdentityProviderListItem } from '../../../entities/identity-provider/identityProviderListItem';
import { ConsoleSettingsService } from '../../../services-ngx/console-settings.service';
import { ConsoleSettings } from '../../../entities/consoleSettings';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

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
  dataSource = new MatTableDataSource<TableData>([]);

  consoleSettings: ConsoleSettings;

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly identityProviderService: IdentityProviderService,
    private readonly consoleSettingsService: ConsoleSettingsService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    combineLatest([this.identityProviderService.list(), this.consoleSettingsService.get()]).subscribe(
      ([identityProviders, consoleSettings]) => {
        this.setDataSourceFromIdentityProviders(identityProviders);
        this.consoleSettings = consoleSettings;
      },
    );
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onIdClicked(identityProvider: IdentityProvider) {
    // eslint-disable-next-line angular/log,no-console
    console.log('Id clicked:', identityProvider);
  }

  onDeleteActionClicked(identityProvider: IdentityProvider) {
    // eslint-disable-next-line angular/log,no-console
    console.log('Delete clicked:', identityProvider);
  }

  onEditActionClicked(identityProvider: IdentityProvider) {
    // eslint-disable-next-line angular/log,no-console
    console.log('Edit clicked:', identityProvider);
  }

  onActivationToggleActionClicked(identityProvider: IdentityProvider) {
    // eslint-disable-next-line angular/log,no-console
    console.log('Activation Toggle clicked:', identityProvider);
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
    return this.dataSource.data.some((idp) => idp.activated);
  }

  private setDataSourceFromIdentityProviders(identityProviders: IdentityProviderListItem[]) {
    this.dataSource = new MatTableDataSource<TableData>(
      identityProviders.map((idp, index) => ({
        logo: `assets/logo_${idp.type.toLowerCase()}-idp.svg`,
        id: idp.id,
        name: idp.name,
        description: idp.description,
        availableOnPortal: idp.enabled,
        activated: index % 2 === 0,
        sync: idp.sync,
        updatedAt: idp.updated_at,
      })),
    );
  }

  getLocalLoginTooltipMessage(): string | null {
    if (this.isReadonlySetting('console.authentication.localLogin.enabled')) {
      return this.providedConfigurationMessage;
    }
    if (!this.hasActivatedIdp()) {
      return 'You must create an identity provider to be able to update this setting';
    }
    return null;
  }
}
