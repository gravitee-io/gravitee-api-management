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
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { uniqBy } from 'lodash';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ApiLogsV2Service } from '../../../../../../services-ngx/api-logs-v2.service';
import { AggregatedMessageLog, ConnectionLogDetail, ConnectorPlugin, ConnectorType } from '../../../../../../entities/management-api-v2';
import { IconService } from '../../../../../../services-ngx/icon.service';
import { ConnectorPluginsV2Service } from '../../../../../../services-ngx/connector-plugins-v2.service';

@Component({
  selector: 'api-runtime-logs-messages',
  templateUrl: './api-runtime-logs-messages.component.html',
  styleUrls: ['./api-runtime-logs-messages.component.scss'],
  standalone: false,
})
export class ApiRuntimeLogsMessagesComponent implements OnInit, OnDestroy {
  private readonly pageSize: number = 5;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public connectorIcons: { [key: string]: string } = {};
  public connectionLog$ = this.loadConnectionLog();
  public messageLogs$: BehaviorSubject<AggregatedMessageLog[]> = new BehaviorSubject<AggregatedMessageLog[]>([]);
  public pageIndex = 1;
  public pageCount: number;
  public selectedTab: string;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly apiLogsService: ApiLogsV2Service,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly iconService: IconService,
    private readonly matSnackBar: MatSnackBar,
  ) {}

  public ngOnInit(): void {
    this.loadMessages(this.pageIndex);
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  public openLogsSettings() {
    this.router.navigate(['../../runtime-logs-settings'], { relativeTo: this.activatedRoute });
  }

  private loadConnectionLog(): Observable<ConnectionLogDetail> {
    return this.apiLogsService
      .searchConnectionLogDetail(this.activatedRoute.snapshot.params.apiId, this.activatedRoute.snapshot.params.requestId)
      .pipe(
        catchError(() => of(undefined)),
        takeUntil(this.unsubscribe$),
      );
  }

  private loadMessages(pageIndex: number): void {
    this.apiLogsService
      .searchMessageLogs(this.activatedRoute.snapshot.params.apiId, this.activatedRoute.snapshot.params.requestId, pageIndex, this.pageSize)
      .pipe(
        map(messageLogs => {
          this.messageLogs$.next([...this.messageLogs$.getValue(), ...messageLogs.data]);
          this.pageIndex += 1;
          this.pageCount = messageLogs.pagination.pageCount;
          return messageLogs.data;
        }),
        switchMap(messageLogs =>
          uniqBy(
            messageLogs.flatMap(messageLog => [
              {
                connectorType: 'ENTRYPOINT',
                connectorId: messageLog.entrypoint?.connectorId,
              },
              {
                connectorType: 'ENDPOINT',
                connectorId: messageLog.endpoint?.connectorId,
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
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public loadMoreMessages(): void {
    this.loadMessages(this.pageIndex);
  }

  private loadConnectorIcon(connectorType: ConnectorType, connectorId: string) {
    if (connectorType === 'ENDPOINT' && null != connectorId) {
      this.connectorPluginsV2Service.getEndpointPlugin(connectorId).subscribe((connectorPlugin: ConnectorPlugin) => {
        this.connectorIcons[connectorId] = this.iconService.registerSvg(connectorId, connectorPlugin.icon);
      });
    }

    if (connectorType === 'ENTRYPOINT' && null != connectorId) {
      this.connectorPluginsV2Service.getEntrypointPlugin(connectorId).subscribe((connectorPlugin: ConnectorPlugin) => {
        this.connectorIcons[connectorId] = this.iconService.registerSvg(connectorId, connectorPlugin.icon);
      });
    }
  }

  onTabChange($event: MatTabChangeEvent) {
    this.selectedTab = $event.tab.textLabel;
  }
}
