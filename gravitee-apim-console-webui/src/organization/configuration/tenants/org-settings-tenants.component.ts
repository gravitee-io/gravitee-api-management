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
import { Subject } from 'rxjs';
import { MatTableDataSource } from '@angular/material/table';

import { TenantService } from '../../../services-ngx/tenant.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Tenant } from '../../../entities/tenant/tenant';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

@Component({
  selector: 'org-settings-tenants',
  template: require('./org-settings-tenants.component.html'),
  styles: [require('./org-settings-tenants.component.scss')],
})
export class OrgSettingsTenantsComponent implements OnInit, OnDestroy {
  displayedColumns: string[] = ['id', 'name', 'description', 'actions'];
  tenantsDataSource: MatTableDataSource<Tenant> = new MatTableDataSource([]);

  private unsubscribe$ = new Subject<boolean>();
  private tenants: Tenant[] = [];

  constructor(private readonly tenantService: TenantService) {}

  ngOnInit(): void {
    this.tenantService.list().subscribe((tenants) => {
      this.tenants = tenants;
      this.tenantsDataSource.data = tenants;
    });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onAddTenantClicked(): void {}

  // eslint-disable-next-line @typescript-eslint/no-empty-function,@typescript-eslint/no-unused-vars
  onDeleteTenantClicked(element: any) {}

  // eslint-disable-next-line @typescript-eslint/no-empty-function,@typescript-eslint/no-unused-vars
  onEditTenantClicked(element: any) {}

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.tenantsDataSource.data = gioTableFilterCollection(this.tenants, filters);
  }
}
