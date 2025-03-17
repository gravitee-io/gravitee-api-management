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
import { DatePipe, TitleCasePipe } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MAT_DATE_FORMATS, MAT_NATIVE_DATE_FORMATS, provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';

import { CopyCodeComponent } from '../../../../../components/copy-code/copy-code.component';

export interface MessageLogDetailDialogData {
  timestamp: string;
  requestId: string;
  clientId: string;
  correlationId: string;
  operation: string;
  entrypoint: MessageLogDetailConnectorData;
  endpoint: MessageLogDetailConnectorData;
}

interface MessageLogDetailConnectorData {
  name?: string;
  payload?: string;
  headers?: Record<string, string[]>;
  metadata?: Record<string, string>;
}

interface MessageLogDetailDialogVM {
  timestamp: string;
  requestId: string;
  clientId: string;
  correlationId: string;
  operation: string;
  entrypoint: MessageLogDetailConnectorVM;
  endpoint: MessageLogDetailConnectorVM;
}

interface MessageLogDetailConnectorVM {
  name?: string;
  payload?: string;
  headers: { key: string; value: string[] }[];
  metadata: { key: string; value: string }[];
}

@Component({
  imports: [
    MatDatepickerModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    DatePipe,
    MatTabsModule,
    CopyCodeComponent,
    TitleCasePipe,
    MatCardModule,
    MatExpansionModule,
  ],
  providers: [provideNativeDateAdapter(), { provide: MAT_DATE_FORMATS, useValue: MAT_NATIVE_DATE_FORMATS }],
  selector: 'app-message-log-detail-dialog',
  standalone: true,
  styleUrl: './message-log-detail-dialog.component.scss',
  templateUrl: './message-log-detail-dialog.component.html',
})
export class MessageLogDetailDialogComponent {
  messageLogDetailData: MessageLogDetailDialogVM;

  constructor(
    public readonly dialogRef: MatDialogRef<MessageLogDetailDialogComponent, MessageLogDetailDialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: MessageLogDetailDialogData,
  ) {
    this.messageLogDetailData = {
      ...dialogData,
      entrypoint: this.transformConnector(dialogData.entrypoint),
      endpoint: this.transformConnector(dialogData.endpoint),
    };
  }

  private transformConnector(connector: MessageLogDetailConnectorData): MessageLogDetailConnectorVM {
    return { ...connector, headers: this.transformHeaders(connector.headers), metadata: this.transformMetadata(connector.metadata) };
  }

  private transformHeaders(headers?: Record<string, string[]>): { key: string; value: string[] }[] {
    return headers ? Object.entries(headers).map(keyValue => ({ key: keyValue[0], value: keyValue[1] })) : [];
  }

  private transformMetadata(metadata?: Record<string, string>): { key: string; value: string }[] {
    return metadata ? Object.entries(metadata).map(keyValue => ({ key: keyValue[0], value: keyValue[1] })) : [];
  }
}
