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
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { filter, map, switchMap, tap } from 'rxjs/operators';
import { GioFormJsonSchemaModule, GioIconsModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { NoMcpEntrypointComponent } from './no-mcp-entrypoint/no-mcp-entrypoint.component';
import {
  ConfigurationMCPForm,
  ConfigureMcpEntrypointComponent,
} from './components/configure-mcp-entrypoint/configure-mcp-entrypoint.component';

import { ApiV4, HttpListener } from '../../../entities/management-api-v2';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ConnectorPluginsV2Service } from '../../../services-ngx/connector-plugins-v2.service';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { DEFAULT_MCP_ENTRYPOINT_PATH, MCP_ENTRYPOINT_ID, MCPConfiguration, MCPTool } from '../../../entities/entrypoint/mcp';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

interface ApiVM extends ApiV4 {
  hasMCPEntrypoint: boolean;
}

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
    GioIconsModule,
    GioPermissionModule,
    ConfigureMcpEntrypointComponent,
    GioSaveBarModule,
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

  form: FormGroup<ConfigurationMCPForm> = new FormGroup<ConfigurationMCPForm>({
    tools: new FormControl<MCPTool[]>([]),
    mcpPath: new FormControl<string>(DEFAULT_MCP_ENTRYPOINT_PATH),
  });

  formInitialValues: { tools: MCPTool[]; mcpPath: string };

  private destroyRef = inject(DestroyRef);

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private apiV2Service: ApiV2Service,
    private gioPermissionService: GioPermissionService,
    private connectorPluginsV2Service: ConnectorPluginsV2Service,
    private snackBarService: SnackBarService,
  ) {
    this.apiId = this.activatedRoute.snapshot.params['apiId'] || '';
  }

  ngOnInit() {
    if (!this.gioPermissionService.hasAnyMatching(['api-definition-u'])) {
      this.form.disable();
    }

    this.api$ = this.apiV2Service.get(this.apiId).pipe(
      filter((api) => api.definitionVersion === 'V4'),
      map((api) => ({ ...api, hasMCPEntrypoint: api.listeners?.[0].entrypoints.some((e) => e.type === MCP_ENTRYPOINT_ID) }) as ApiVM),
      tap((api) => {
        if (api.hasMCPEntrypoint) {
          this.updateFormValues(api);
        }
      }),
    );
  }

  addMcpEntrypoint() {
    this.router.navigate(['./enable'], { relativeTo: this.activatedRoute });
  }

  onSubmit() {
    const configuration: MCPConfiguration = this.form.getRawValue();

    this.apiV2Service
      .get(this.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const listeners = api.listeners;
          const httpListener = listeners[0] as HttpListener;

          httpListener.entrypoints = httpListener.entrypoints.map((entrypoint) =>
            entrypoint.type === MCP_ENTRYPOINT_ID ? { ...entrypoint, configuration } : entrypoint,
          );

          listeners[0] = httpListener;
          return this.apiV2Service.update(this.apiId, { ...api, listeners });
        }),
        tap((api) => {
          this.snackBarService.success('MCP entrypoint has been updated successfully.');
          this.updateFormValues(api as ApiV4);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private updateFormValues(api: ApiV4): void {
    const mcpConfiguration = api.listeners[0].entrypoints.find((e) => e.type === MCP_ENTRYPOINT_ID).configuration as MCPConfiguration;
    this.form.reset({
      mcpPath: mcpConfiguration.mcpPath,
      tools: mcpConfiguration.tools,
    });
    this.formInitialValues = this.form.getRawValue();
  }
}
