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
import {Component, Input, input, OnInit} from '@angular/core';

import { Log } from '../../../../../entities/log/log';
import {catchError, map, Observable} from "rxjs";
import {of} from "rxjs/internal/observable/of";
import {AggregatedMessageLog} from "../../../../../entities/log/messageLog";
import {ApplicationLogService} from "../../../../../services/application-log.service";
import {AsyncPipe, DatePipe} from "@angular/common";
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

@Component({
  selector: 'app-application-log-messages',
  standalone: true,
  imports: [
    AsyncPipe,
    LoaderComponent,
    MatTable,
    DatePipe,
    MatTableModule,
    MatIcon
  ],
  templateUrl: './application-log-messages.component.html',
  styleUrl: './application-log-messages.component.scss',
})
export class ApplicationLogMessagesComponent implements OnInit {
  @Input()
  applicationId!: string;

  log = input.required<Log>();

  messages$: Observable<{ data: AggregatedMessageLog[]; hasError: boolean; }> = of();

  displayedColumns: string[] = ['timestamp', 'correlationId', 'phase', 'endpoint', 'action'];

  constructor(private applicationLogService: ApplicationLogService) {
  }

  ngOnInit() {
    this.messages$ = this.applicationLogService.getMessages(this.applicationId, this.log().id, this.log().timestamp, 1, 10).pipe(
      map(({ data }) => ({data, hasError: false})),
      catchError(_ => of({ data: [], hasError: true}))
    );
  }

}
