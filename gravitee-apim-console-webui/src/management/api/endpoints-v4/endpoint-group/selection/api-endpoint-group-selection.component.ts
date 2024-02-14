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
import { Component, Input, OnInit } from '@angular/core';
import { catchError, switchMap, takeUntil } from 'rxjs/operators';
import { of, Subject } from 'rxjs';
import { UntypedFormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';

import { ApiType, ConnectorVM, fromConnector } from '../../../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../../../services-ngx/icon.service';
import {
  GioConnectorDialogComponent,
  GioConnectorDialogData,
} from '../../../component/gio-connector-dialog/gio-connector-dialog.component';

@Component({
  selector: 'api-endpoints-group-selection',
  templateUrl: './api-endpoint-group-selection.component.html',
  styleUrls: ['./api-endpoint-group-selection.component.scss'],
})
export class ApiEndpointGroupSelectionComponent implements OnInit {
  private unsubscribe$: Subject<void> = new Subject<void>();

  @Input()
  public endpointGroupTypeForm: UntypedFormGroup;
  @Input()
  public apiType: ApiType;

  public endpoints: ConnectorVM[];

  constructor(
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly iconService: IconService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit() {
    this.connectorPluginsV2Service
      .listEndpointPluginsByApiType(this.apiType)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((endpointPlugins) => {
        this.endpoints = endpointPlugins.map((endpoint) => fromConnector(this.iconService, endpoint));
      });
  }

  onMoreInfoClick(event, endpoint: ConnectorVM) {
    event.stopPropagation();
    this.connectorPluginsV2Service
      .getEndpointPluginMoreInformation(endpoint.id)
      .pipe(
        catchError(() => of({})),
        switchMap((pluginMoreInformation) =>
          this.matDialog
            .open<GioConnectorDialogComponent, GioConnectorDialogData, boolean>(GioConnectorDialogComponent, {
              data: {
                name: endpoint.name,
                pluginMoreInformation,
              },
              role: 'alertdialog',
              id: 'moreInfoDialog',
            })
            .afterClosed(),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
