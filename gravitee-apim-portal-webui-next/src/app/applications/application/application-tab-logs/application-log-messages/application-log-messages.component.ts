/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { AsyncPipe, DatePipe, TitleCasePipe } from '@angular/common';
import { Component, DestroyRef, inject, input, OnInit, signal, WritableSignal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatTable, MatTableModule } from '@angular/material/table';
import { catchError, combineLatest, map, Observable, switchMap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { LoaderComponent } from '../../../../../components/loader/loader.component';
import { PaginationComponent } from '../../../../../components/pagination/pagination.component';
import { Connector } from '../../../../../entities/connector';
import { Log, LogsResponseMetadataTotalData, AggregatedMessageLog } from '../../../../../entities/log';
import { ApplicationLogService } from '../../../../../services/application-log.service';
import { ConnectorService } from '../../../../../services/connector.service';
import {
  MessageLogDetailDialogComponent,
  MessageLogDetailDialogData,
} from '../message-log-detail-dialog/message-log-detail-dialog.component';

interface AggregatedMessageLogVM extends AggregatedMessageLog {
  entrypointType: string;
  endpointType: string;
}

interface AggregatedMessageLogResponse {
  data: AggregatedMessageLogVM[];
  hasError: boolean;
}

@Component({
  selector: 'app-application-log-messages',
  standalone: true,
  imports: [AsyncPipe, LoaderComponent, MatTable, DatePipe, MatTableModule, MatIcon, PaginationComponent, TitleCasePipe],
  templateUrl: './application-log-messages.component.html',
  styleUrl: './application-log-messages.component.scss',
})
export class ApplicationLogMessagesComponent implements OnInit {
  applicationId = input.required<string>();
  log = input.required<Log>();

  currentPage: WritableSignal<number> = signal(1);
  totalResults: WritableSignal<number> = signal(0);

  messages$: Observable<AggregatedMessageLogResponse> = of();

  displayedColumns: string[] = ['timestamp', 'correlationId', 'operation', 'endpoint', 'action'];

  private readonly refreshTableData$ = toObservable(this.currentPage);
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private readonly applicationLogService: ApplicationLogService,
    private readonly connectorService: ConnectorService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit() {
    this.messages$ = this.getConnectors$().pipe(switchMap(({ entrypoints, endpoints }) => this.getMessagesLogVM$(entrypoints, endpoints)));
  }

  showMessageLogDetails(messageLog: AggregatedMessageLogVM) {
    this.matDialog
      .open<MessageLogDetailDialogComponent, MessageLogDetailDialogData, void>(MessageLogDetailDialogComponent, {
        data: {
          timestamp: messageLog.timestamp,
          requestId: messageLog.requestId,
          correlationId: messageLog.correlationId,
          operation: messageLog.operation,
          clientId: messageLog.clientIdentifier,
          entrypoint: {
            name: messageLog.entrypointType,
            payload: messageLog.entrypoint.payload,
            headers: messageLog.entrypoint.headers,
            metadata: messageLog.entrypoint.metadata,
          },
          endpoint: {
            name: messageLog.endpointType,
            payload: messageLog.endpoint.payload,
            headers: messageLog.endpoint.headers,
            metadata: messageLog.endpoint.metadata,
          },
        },
        width: '800px',
        id: `${messageLog.correlationId}-message-log-detail`,
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();
  }

  goToPage(pageNumber: number) {
    this.currentPage.set(pageNumber);
  }

  private getConnectors$(): Observable<{ entrypoints: Connector[]; endpoints: Connector[] }> {
    return combineLatest([this.connectorService.getEntrypoints(), this.connectorService.getEndpoints()]).pipe(
      map(([entrypoints, endpoints]) => ({ entrypoints: entrypoints.data, endpoints: endpoints.data })),
      catchError(() => of({ entrypoints: [], endpoints: [] })),
    );
  }

  private getMessagesLogVM$(entrypoints: Connector[], endpoints: Connector[]): Observable<AggregatedMessageLogResponse> {
    return this.refreshTableData$.pipe(
      switchMap(page =>
        this.applicationLogService.getAggregatedMessages(this.applicationId(), this.log().id, this.log().timestamp, page, 10),
      ),
      map(({ data, metadata }) => {
        this.totalResults.set((metadata['data'] as LogsResponseMetadataTotalData).total);

        return {
          data: data.map(d => ({
            ...d,
            endpointType: this.getConnectorName(d.endpoint.connectorId, endpoints),
            entrypointType: this.getConnectorName(d.entrypoint.connectorId, entrypoints),
          })),
          hasError: false,
        };
      }),
      catchError(_ => of({ data: [], hasError: true })),
    );
  }

  private getConnectorName(connectorId: string, connectors: Connector[]): string {
    const foundEndpoint = connectors.find(c => c.id === connectorId);
    return foundEndpoint ? foundEndpoint.name : connectorId;
  }
}
