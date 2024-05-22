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
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH } from '@gravitee/ui-particles-angular';
import { isNil } from 'lodash';

import { Pagination, Event } from '../../../../entities/management-api-v2';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiHistoryV4DeploymentCompareComponent } from '../deployment-compare/api-history-v4-deployment-compare.component';

type EventDS = Event & { selected: boolean };

@Component({
  selector: 'api-deployments-table',
  templateUrl: './api-history-v4-deployments-table.component.html',
  styleUrls: ['./api-history-v4-deployments-table.component.scss'],
})
export class ApiHistoryV4DeploymentsTableComponent implements OnChanges {
  DEPLOYMENT_NUMBER_PROPERTY = 'DEPLOYMENT_NUMBER';
  LABEL_PROPERTY = 'DEPLOYMENT_LABEL';
  displayedColumns = ['checkbox', 'version', 'createdAt', 'user', 'label', 'action'];
  protected tableWrapperPagination: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  protected total = 0;

  private selectedEvent: [Event, Event] = [null, null];

  constructor(private readonly matDialog: MatDialog) {}

  @Input()
  public deployments: Event[];

  @Input()
  public currentDefinition: unknown | null;

  @Input()
  public deploymentStates: string;

  @Output()
  public paginationChange = new EventEmitter<Pagination>();

  @Output()
  public rollback = new EventEmitter<string>();

  @Output()
  public selectedEventChange = new EventEmitter<[Event, Event]>();

  public deploymentsDS: EventDS[];

  ngOnChanges(simpleChanges: SimpleChanges) {
    if (simpleChanges.deployments) {
      this.deploymentsDS = this.deployments.map((deployment) => ({ ...deployment, selected: this.getSelected(deployment.id) }));
    }
  }

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

  compareWithCurrent(eventToCompare: EventDS) {
    const jsonDefinitionToCompare = this.extractApiDefinition(eventToCompare);
    const jsonCurrentDefinition = JSON.stringify(this.currentDefinition, null, 2);
    this.matDialog
      .open(ApiHistoryV4DeploymentCompareComponent, {
        autoFocus: false,
        data: {
          left: { apiDefinition: jsonDefinitionToCompare, version: eventToCompare.properties[this.DEPLOYMENT_NUMBER_PROPERTY] },
          right: { apiDefinition: jsonCurrentDefinition, version: 'to deploy' },
        },
        width: GIO_DIALOG_WIDTH.LARGE,
        maxHeight: 'calc(100vh - 90px)',
      })
      .afterClosed()
      .pipe();
  }

  selectRow(event: EventDS) {
    if (this.selectedEvent[0] === null) {
      this.selectedEvent[0] = event;
    } else if (this.selectedEvent[0].id === event.id) {
      this.selectedEvent = [this.selectedEvent[1], null];
    } else if (this.selectedEvent[1] !== null && this.selectedEvent[1].id === event.id) {
      this.selectedEvent[1] = null;
    } else {
      this.selectedEvent[1] = event;
    }

    this.deploymentsDS = this.deploymentsDS.map((deployment) => {
      deployment.selected = this.getSelected(deployment.id);
      return deployment;
    });
    if (this.selectedEvent[0] !== null && this.selectedEvent[1] !== null) {
      this.selectedEventChange.emit(this.selectedEvent);
    }
  }

  private getSelected(deploymentId: string): boolean {
    return this.selectedEvent
      .filter((e) => !isNil(e))
      .map((e) => e.id)
      .includes(deploymentId);
  }

  private extractApiDefinition(event: Event): string {
    const payload = JSON.parse(event.payload);
    const definition = JSON.parse(payload.definition);
    return JSON.stringify(definition, null, 2);
  }
}
