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

import { Component, Inject, OnInit } from '@angular/core';
import { StateService } from '@uirouter/angular';
import { StateParams } from '@uirouter/core';
import { map, switchMap, tap } from 'rxjs/operators';
import { BehaviorSubject } from 'rxjs';
import { uniqBy } from 'lodash';

import { ApiLogsV2Service } from '../../../../services-ngx/api-logs-v2.service';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { AggregatedMessageLog, ConnectorPlugin, ConnectorType } from '../../../../entities/management-api-v2';
import { IconService } from '../../../../services-ngx/icon.service';
import { ConnectorPluginsV2Service } from '../../../../services-ngx/connector-plugins-v2.service';

@Component({
  selector: 'api-runtime-logs-messages',
  template: require('./api-runtime-logs-messages.component.html'),
  styles: [require('./api-runtime-logs-messages.component.scss')],
})
export class ApiRuntimeLogsMessagesComponent implements OnInit {
  private readonly pageSize: number = 5;
  public connectorIcons: { [key: string]: string } = {};
  public messageLogs$: BehaviorSubject<AggregatedMessageLog[]> = new BehaviorSubject<AggregatedMessageLog[]>([]);
  public pageIndex = 1;
  public pageCount: number;

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiLogsService: ApiLogsV2Service,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly iconService: IconService,
  ) {}

  public ngOnInit(): void {
    this.loadMessages(this.pageIndex);
  }
  public openLogsSettings() {
    return this.ajsState.go('management.apis.runtimeLogs-settings');
  }

  private loadMessages(pageIndex: number): void {
    this.apiLogsService
      .searchMessageLogs(this.ajsStateParams.apiId, this.ajsStateParams.requestId, pageIndex, this.pageSize)
      .pipe(
        map((messageLogs) => {
          this.messageLogs$.next([...this.messageLogs$.getValue(), ...messageLogs.data]);
          this.pageIndex += 1;
          this.pageCount = messageLogs.pagination.pageCount;
          return messageLogs.data;
        }),
        switchMap((messageLogs) =>
          uniqBy(
            messageLogs.flatMap((messageLog) => [
              {
                connectorType: 'ENTRYPOINT',
                connectorId: messageLog.entrypoint.connectorId,
              },
              {
                connectorType: 'ENDPOINT',
                connectorId: messageLog.endpoint.connectorId,
              },
            ]),
            'connectorId',
          ),
        ),
        tap((entry: { connectorId: string; connectorType: ConnectorType }) => {
          if (!this.connectorIcons[entry.connectorId]) {
            this.loadConnectorIcon(entry.connectorType, entry.connectorId);
          }
        }),
      )
      .subscribe();
  }

  public loadMoreMessages(): void {
    this.loadMessages(this.pageIndex);
  }

  private loadConnectorIcon(connectorType: ConnectorType, connectorId: string) {
    if (connectorType === 'ENDPOINT') {
      this.connectorPluginsV2Service.getEndpointPlugin(connectorId).subscribe((connectorPlugin: ConnectorPlugin) => {
        this.connectorIcons[connectorId] = this.iconService.registerSvg(connectorId, connectorPlugin.icon);
      });
    }

    if (connectorType === 'ENTRYPOINT') {
      this.connectorPluginsV2Service.getEntrypointPlugin(connectorId).subscribe((connectorPlugin: ConnectorPlugin) => {
        this.connectorIcons[connectorId] = this.iconService.registerSvg(connectorId, connectorPlugin.icon);
      });
    }
  }
}
