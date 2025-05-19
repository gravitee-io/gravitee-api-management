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
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, of } from 'rxjs';
import { filter, map, tap } from 'rxjs/operators';
import { GioFormJsonSchemaModule, GioIconsModule, GioJsonSchema } from '@gravitee/ui-particles-angular';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';

import { NoMcpEntrypointComponent } from './no-mcp-entrypoint/no-mcp-entrypoint.component';

import { ApiV4 } from '../../../entities/management-api-v2';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ConnectorPluginsV2Service } from '../../../services-ngx/connector-plugins-v2.service';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';

interface ApiVM extends ApiV4 {
  hasMCPEntrypoint: boolean;
}

const MCP_ENTRYPOINT_ID = 'mcp';

@Component({
  selector: 'mcp',
  templateUrl: './mcp.component.html',
  styleUrls: ['./mcp.component.scss'],
  imports: [
    NoMcpEntrypointComponent,
    AsyncPipe,
    GioFormJsonSchemaModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    RouterLink,
    GioIconsModule,
    GioPermissionModule,
  ],
})
export class McpComponent implements OnInit {
  apiId: string;
  api$: Observable<ApiVM> = of();

  canEnableMcp$: Observable<boolean> = this.connectorPluginsV2Service
    .listEntrypointPlugins()
    .pipe(
      map((plugins) =>
        plugins.some((plugin) => plugin.id === MCP_ENTRYPOINT_ID && this.gioPermissionService.hasAnyMatching(['api-definition-u'])),
      ),
    );

  form: FormGroup<{ configuration: FormControl<unknown> }> = new FormGroup({
    configuration: new FormControl<unknown>({}),
  });
  mcpEntrypointSchema$: Observable<GioJsonSchema> = inject(ConnectorPluginsV2Service).getEntrypointPluginSchema(MCP_ENTRYPOINT_ID);

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
    this.form.disable();
    this.api$ = this.apiV2Service.getLastApiFetch(this.apiId).pipe(
      filter((api) => api.definitionVersion === 'V4'),
      map((api) => ({ ...api, hasMCPEntrypoint: api.listeners?.[0].entrypoints.some((e) => e.type === MCP_ENTRYPOINT_ID) }) as ApiVM),
      tap((api) => {
        if (api.hasMCPEntrypoint) {
          this.form.controls.configuration.setValue(
            api.listeners?.[0].entrypoints.find((e) => e.type === MCP_ENTRYPOINT_ID)?.configuration,
          );
        }
      }),
    );
  }

  addMcpEntrypoint() {
    this.router.navigate(['./add'], { relativeTo: this.activatedRoute });
  }
}
