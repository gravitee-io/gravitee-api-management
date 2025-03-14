/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import {Component, DestroyRef, inject, Input, input, OnInit, signal, Signal, WritableSignal} from '@angular/core';

import {Log, LogsResponseMetadataTotalData} from '../../../../../entities/log/log';
import {
  BehaviorSubject,
  catchError,
  combineLatest,
  combineLatestAll,
  combineLatestWith,
  map,
  Observable,
  switchMap
} from "rxjs";
import {of} from "rxjs/internal/observable/of";
import {AggregatedMessageLog} from "../../../../../entities/log/messageLog";
import {ApplicationLogService} from "../../../../../services/application-log.service";
import {AsyncPipe, DatePipe, TitleCasePipe} from "@angular/common";
import {LoaderComponent} from "../../../../../components/loader/loader.component";
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderRow,
  MatHeaderRowDef, MatRow, MatRowDef,
  MatTable, MatTableModule
} from "@angular/material/table";
import {MatIcon} from "@angular/material/icon";
import {Application} from "../../../../../entities/application/application";
import {ConnectorService} from "../../../../../services/connector.service";
import {ConnectorsResponse} from "../../../../../entities/connector/connector";
import {PaginationComponent} from "../../../../../components/pagination/pagination.component";
import {takeUntilDestroyed, toObservable, toSignal} from "@angular/core/rxjs-interop";
import {MatDialog} from "@angular/material/dialog";
import {
  MessageLogDetailDialogComponent,
  MessageLogDetailDialogData
} from "../message-log-detail-dialog/message-log-detail-dialog.component";

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
  imports: [
    AsyncPipe,
    LoaderComponent,
    MatTable,
    DatePipe,
    MatTableModule,
    MatIcon,
    PaginationComponent,
    TitleCasePipe
  ],
  templateUrl: './application-log-messages.component.html',
  styleUrl: './application-log-messages.component.scss',
})
export class ApplicationLogMessagesComponent implements OnInit {
  applicationId = input.required<string>();
  log = input.required<Log>();

  messages$: Observable<AggregatedMessageLogResponse> = of();

  displayedColumns: string[] = ['timestamp', 'correlationId', 'phase', 'endpoint', 'action'];

  currentPage: WritableSignal<number> = signal(1);
  totalResults: WritableSignal<number> = signal(0);

  private refreshTableData$ = toObservable(this.currentPage);
  private destroyRef = inject(DestroyRef);

  constructor(private applicationLogService: ApplicationLogService, private connectorService: ConnectorService,

              private matDialog: MatDialog,) {
  }

  ngOnInit() {
    this.messages$ = combineLatest([this.connectorService.getEntrypoints(), this.connectorService.getEndpoints()]).pipe(
      switchMap(([entrypoints, endpoints]) => this.getMessagesLogVM$(entrypoints, endpoints))
    )
  }

  showMessageLogDetails(messageLog: AggregatedMessageLogVM) {
    console.log("opening with id: ", messageLog)
    this.matDialog.open<MessageLogDetailDialogComponent, MessageLogDetailDialogData, void>(MessageLogDetailDialogComponent, {
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
          metadata: messageLog.entrypoint.metadata
        },
        endpoint: {
          name: messageLog.endpointType,
          payload: messageLog.endpoint.payload,
          headers: messageLog.endpoint.headers,
          metadata: messageLog.endpoint.metadata
        }
      },
      width: '800px',
      id: `${messageLog.correlationId}-message-log-detail`
    })
    .afterClosed()
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe();
  }

  private getMessagesLogVM$(entrypoints: ConnectorsResponse, endpoints: ConnectorsResponse): Observable<AggregatedMessageLogResponse> {
    return this.refreshTableData$.pipe(
      switchMap(page => this.applicationLogService.getMessages(this.applicationId(), this.log().id, this.log().timestamp, page, 10)),
      map(({ data, metadata }) => {
        this.totalResults.set((metadata['data'] as LogsResponseMetadataTotalData).total);

        return {
          data: data.map(d => ({...d, endpointType: this.getConnectorName(d.endpoint.connectorId, endpoints), entrypointType: this.getConnectorName(d.entrypoint.connectorId, entrypoints)})),
          hasError: false,
        }
      }),
      catchError(_ => of({ data: [], hasError: true}))
    );
  }

  private getConnectorName(connectorId: string, connectorsResponse: ConnectorsResponse): string {
    const foundEndpoint = connectorsResponse.data.find(c => c.id === connectorId);
    return foundEndpoint ? foundEndpoint.name : connectorId;
  }

  goToPage(pageNumber: number) {
    this.currentPage.set(pageNumber);
  }
}
