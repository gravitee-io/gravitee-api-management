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
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { BehaviorSubject, EMPTY, Observable } from 'rxjs';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';
import { AsyncPipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { McpToolDisplayComponent } from './mcp-tool-display/mcp-tool-display.component';

import { MCP } from '../../../entities/management-api-v2';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';

@Component({
  selector: 'mcp',
  standalone: true,
  imports: [MatCardModule, MatExpansionModule, AsyncPipe, McpToolDisplayComponent, MatButtonModule, MatIconModule],
  templateUrl: './mcp.component.html',
  styleUrl: './mcp.component.scss',
})
export class McpComponent implements OnInit {
  mcp$: Observable<MCP>;

  private apiId = this.activatedRoute.snapshot.params.apiId;
  private refreshApi = new BehaviorSubject(1);
  private destroyRef = inject(DestroyRef);

  constructor(
    private readonly apiServiceV2: ApiV2Service,
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit() {
    this.mcp$ = this.refreshApi.pipe(
      switchMap(() => this.apiServiceV2.get(this.apiId)),
      map((api) => {
        if (api.definitionVersion === 'V4' && api.mcp?.enabled) {
          return api.mcp;
        }
      }),
    );
  }

  importViaOpenAPI() {
    this.router.navigate(['.', 'import'], { relativeTo: this.activatedRoute });
  }

  deleteTools() {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: `Delete Tools`,
          content: 'Are you sure that you want to delete all MCP tools? This action is irreversible.',
          confirmButton: `Delete`,
        },
        role: 'alertdialog',
        id: 'deleteToolsDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.apiServiceV2.get(this.apiId)),
        switchMap((response) => {
          if (response.definitionVersion === 'V4' && response.type === 'PROXY') {
            const mcp: MCP = response.mcp ? { ...response.mcp, tools: [] } : { enabled: true, tools: [] };

            return this.apiServiceV2.update(this.apiId, { ...response, mcp });
          }
          throw Error('An API must be a v4 Proxy API in order to include MCP configuration.');
        }),
        tap(() => {
          this.refreshApi.next(1);
        }),
        catchError(() => {
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}
