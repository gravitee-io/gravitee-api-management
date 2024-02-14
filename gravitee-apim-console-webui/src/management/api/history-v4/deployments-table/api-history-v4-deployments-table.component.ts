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
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH } from '@gravitee/ui-particles-angular';

import { Pagination, Event, Api } from '../../../../entities/management-api-v2';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiHistoryV4DeploymentCompareComponent } from '../deployment-compare/api-history-v4-deployment-compare.component';

@Component({
  selector: 'api-deployments-table',
  templateUrl: './api-history-v4-deployments-table.component.html',
  styleUrls: ['./api-history-v4-deployments-table.component.scss'],
})
export class ApiHistoryV4DeploymentsTableComponent {
  DEPLOYMENT_NUMBER_PROPERTY = 'DEPLOYMENT_NUMBER';
  LABEL_PROPERTY = 'DEPLOYMENT_LABEL';
  displayedColumns = ['version', 'createdAt', 'user', 'label', 'action'];
  protected tableWrapperPagination: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  protected total = 0;
  private _currentApi: Api;

  constructor(private readonly matDialog: MatDialog) {}

  @Input()
  public deployments: Event[];

  @Input()
  public get currentApi() {
    return this._currentApi;
  }

  public set currentApi(currentApi: Api) {
    this._currentApi = currentApi;
  }

  @Output()
  public paginationChange = new EventEmitter<Pagination>();

  protected tableWrapperPaginationChange(event: GioTableWrapperFilters) {
    this.paginationChange.emit({
      page: event.pagination.index,
      perPage: event.pagination.size,
    });
  }

  @Input()
  public set pagination(pagination: Pagination) {
    this.total = pagination.totalCount;
    this.tableWrapperPagination = {
      ...this.tableWrapperPagination,
      pagination: {
        index: pagination.page,
        size: pagination.perPage,
      },
    };
  }

  compareWithCurrent(eventToCompare: Event) {
    const jsonApiDefinition = this.extractApiDefinition(eventToCompare);
    const jsonFormattedCurrentApi = JSON.stringify(this._currentApi, null, 2);
    this.matDialog
      .open(ApiHistoryV4DeploymentCompareComponent, {
        autoFocus: false,
        data: {
          left: { apiDefinition: jsonApiDefinition, version: eventToCompare.properties[this.DEPLOYMENT_NUMBER_PROPERTY] },
          right: { apiDefinition: jsonFormattedCurrentApi, version: 'to deploy' },
        },
        width: GIO_DIALOG_WIDTH.LARGE,
      })
      .afterClosed()
      .pipe();
  }

  private extractApiDefinition(event: Event): string {
    const payload = JSON.parse(event.payload);
    const definition = JSON.parse(payload.definition);
    return JSON.stringify(definition, null, 2);
  }
}
