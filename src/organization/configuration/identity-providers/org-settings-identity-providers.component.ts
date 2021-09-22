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

import { IdentityProviderService } from '../../../services-ngx/identity-provider.service';
import { IdentityProvider } from '../../../entities/identity-provider/identityProvider';
import { IdentityProviderListItem } from '../../../entities/identity-provider/identityProviderListItem';

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
  displayedColumns: string[] = ['logo', 'id', 'name', 'description', 'activated', 'sync', 'availableOnPortal', 'updatedAt', 'actions'];
  dataSource = new MatTableDataSource([]);

  constructor(private readonly identityProviderService: IdentityProviderService) {}

  ngOnInit() {
    this.identityProviderService.list().subscribe((identityProviders) => {
      this.setDataSourceFromIdentityProviders(identityProviders);
    });
  }

  ngOnDestroy(): void {}

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
}
