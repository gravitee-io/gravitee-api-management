<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 47db69a114 (style: apply prettier)
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
<<<<<<< HEAD
import { AsyncPipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';

import { NoMcpEntrypointComponent } from './no-mcp-entrypoint/no-mcp-entrypoint.component';

import { ApiV4 } from '../../../entities/management-api-v2';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ConnectorPluginsV2Service } from '../../../services-ngx/connector-plugins-v2.service';

interface ApiVM extends ApiV4 {
  hasMCPEntrypoint: boolean;
}

@Component({
  selector: 'mcp',
  templateUrl: './mcp.component.html',
  styleUrls: ['./mcp.component.scss'],
  imports: [NoMcpEntrypointComponent, AsyncPipe],
})
export class McpComponent implements OnInit {
  apiId: string;
  api$: Observable<ApiVM> = of();

  canEnableMcp$: Observable<boolean> = this.connectorPluginsV2Service
    .listEntrypointPlugins()
    .pipe(
      map((plugins) =>
        plugins.some((plugin) => plugin.id === this.mcpEntrypointId && this.gioPermissionService.hasAnyMatching(['api-definition-u'])),
      ),
    );

  private mcpEntrypointId = 'mcp';

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private apiV2Service: ApiV2Service,
    private gioPermissionService: GioPermissionService,
    private connectorPluginsV2Service: ConnectorPluginsV2Service,
  ) {
    this.apiId = this.activatedRoute.snapshot.params['apiId'] || '';
  }

  ngOnInit() {
    this.api$ = this.apiV2Service.getLastApiFetch(this.apiId).pipe(
      filter((api) => api.definitionVersion === 'V4'),
      map((api) => ({ ...api, hasMCPEntrypoint: api.listeners?.[0].entrypoints.some((e) => e.type === this.mcpEntrypointId) }) as ApiVM),
    );
  }

  addMcpEntrypoint() {
    this.router.navigate(['./add'], { relativeTo: this.activatedRoute });
=======
import {Component, Input, OnInit} from '@angular/core';
=======
import {Component, DestroyRef, inject, Input, OnInit} from '@angular/core';
>>>>>>> 3219f16fd8 (feat(console): import tools via openapi spec and delete)
import {MatCardModule} from "@angular/material/card";
import {MatExpansionModule} from "@angular/material/expansion";
import {ApiV2Service} from "../../../services-ngx/api-v2.service";
import {BehaviorSubject, EMPTY, Observable} from "rxjs";
import {Api, MCP} from "../../../entities/management-api-v2";
import {catchError, filter, map, switchMap, takeUntil, tap} from "rxjs/operators";
import {AsyncPipe} from "@angular/common";
import {McpToolDisplayComponent} from "./mcp-tool-display/mcp-tool-display.component";
import {ActivatedRoute, Router} from "@angular/router";
import {MatButton, MatButtonModule} from "@angular/material/button";
import {MatIcon, MatIconModule} from "@angular/material/icon";
import {MatDialog, MatDialogRef} from "@angular/material/dialog";
import {GioConfirmDialogComponent, GioConfirmDialogData} from "@gravitee/ui-particles-angular";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
=======
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
>>>>>>> 47db69a114 (style: apply prettier)

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
<<<<<<< HEAD
      if (api.definitionVersion === "V4" && api.mcp?.enabled) {
        return api.mcp;
      }
    }));
>>>>>>> 4fd3c69d69 (feat(console): manage mcp tools)
=======
        if (api.definitionVersion === 'V4' && api.mcp?.enabled) {
          return api.mcp;
        }
      }),
    );
>>>>>>> 47db69a114 (style: apply prettier)
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
