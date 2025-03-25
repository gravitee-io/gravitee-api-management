<<<<<<< HEAD
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
import {MatCardModule} from "@angular/material/card";
import {MatExpansionModule} from "@angular/material/expansion";
import {ApiV2Service} from "../../../services-ngx/api-v2.service";
import {EMPTY, Observable} from "rxjs";
import {MCP} from "../../../entities/management-api-v2";
import {map} from "rxjs/operators";
import {AsyncPipe} from "@angular/common";
import {McpToolReadComponent} from "./mcp-tool-read/mcp-tool-read.component";
import {ActivatedRoute} from "@angular/router";
import {MatButton, MatButtonModule} from "@angular/material/button";
import {MatIcon, MatIconModule} from "@angular/material/icon";

@Component({
  selector: 'mcp',
  standalone: true,
  imports: [MatCardModule, MatExpansionModule, AsyncPipe, McpToolReadComponent, MatButtonModule, MatIconModule],
  templateUrl: './mcp.component.html',
  styleUrl: './mcp.component.scss'
})
export class McpComponent implements OnInit {

  mcp$: Observable<MCP>;
  private apiId = this.activatedRoute.snapshot.params.apiId;

  constructor(private readonly apiServiceV2: ApiV2Service, private readonly activatedRoute: ActivatedRoute) {
  }

  ngOnInit() {
    this.mcp$ = this.apiServiceV2.get(this.apiId).pipe(map((api) => {
      if (api.definitionVersion === "V4" && api.mcp?.enabled) {
        return api.mcp;
      }
    }));
>>>>>>> 4fd3c69d69 (feat(console): manage mcp tools)
  }
}
