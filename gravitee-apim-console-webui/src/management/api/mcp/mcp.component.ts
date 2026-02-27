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
import { combineLatest, Observable, of } from 'rxjs';
import { filter, map, switchMap, tap } from 'rxjs/operators';
import {
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioFormJsonSchemaModule,
  GioFormSlideToggleModule,
  GioIconsModule,
  GioLicenseService,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatSlideToggle, MatSlideToggleChange } from '@angular/material/slide-toggle';
import { MatTooltip } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';

import { NoMcpEntrypointComponent } from './no-mcp-entrypoint/no-mcp-entrypoint.component';
import { ConfigureMcpEntrypointComponent } from './components/configure-mcp-entrypoint/configure-mcp-entrypoint.component';

import { ApiV4, HttpListener } from '../../../entities/management-api-v2';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ConnectorPluginsV2Service } from '../../../services-ngx/connector-plugins-v2.service';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { DEFAULT_MCP_ENTRYPOINT_PATH, MCP_ENTRYPOINT_ID, MCPConfiguration } from '../../../entities/entrypoint/mcp';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { ApimFeature } from '../../../shared/components/gio-license/gio-license-data';

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
    GioFormSlideToggleModule,
    MatSlideToggle,
    MatTooltip,
  ],
})
export class McpComponent implements OnInit {
  apiId: string;
  api$: Observable<ApiVM> = of();

  canEnableMcp$ = combineLatest([
    this.connectorPluginsV2Service.listEntrypointPlugins(),
    this.licenseService.isMissingFeature$(ApimFeature.APIM_MCP_TOOL_SERVER),
  ]).pipe(
    map(([plugins, isMissingFeature]) => {
      const hasPlugin = plugins.some(p => p.id === MCP_ENTRYPOINT_ID);
      const hasPermission = this.gioPermissionService.hasAnyMatching(['api-definition-u']);

      return hasPlugin && hasPermission && !isMissingFeature;
    }),
  );

  form = new FormGroup({
    mcpConfig: new FormControl<MCPConfiguration>({
      tools: [],
      mcpPath: DEFAULT_MCP_ENTRYPOINT_PATH,
    }),
  });

  formInitialValues: { mcpConfig: MCPConfiguration };

  private destroyRef = inject(DestroyRef);

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private apiV2Service: ApiV2Service,
    private gioPermissionService: GioPermissionService,
    private connectorPluginsV2Service: ConnectorPluginsV2Service,
    private snackBarService: SnackBarService,
    private matDialog: MatDialog,
    private readonly licenseService: GioLicenseService,
  ) {
    this.apiId = this.activatedRoute.snapshot.params['apiId'] || '';
  }

  ngOnInit() {
    this.api$ = this.apiV2Service.get(this.apiId).pipe(
      filter(api => api.definitionVersion === 'V4'),
      map(api => ({ ...api, hasMCPEntrypoint: api.listeners?.[0].entrypoints.some(e => e.type === MCP_ENTRYPOINT_ID) }) as ApiVM),
      tap(api => {
        if (api.hasMCPEntrypoint) {
          this.updateFormValues(api);
        } else {
          this.formInitialValues = this.form.getRawValue();
        }
      }),
    );
  }

  addMcpEntrypoint() {
    this.router.navigate(['./enable'], { relativeTo: this.activatedRoute });
  }

  onSubmit() {
    const configuration: MCPConfiguration = this.form.getRawValue().mcpConfig;

    this.apiV2Service
      .get(this.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const listeners = api.listeners;
          const httpListener = listeners[0] as HttpListener;

          httpListener.entrypoints = httpListener.entrypoints.map(entrypoint =>
            entrypoint.type === MCP_ENTRYPOINT_ID ? { ...entrypoint, configuration } : entrypoint,
          );

          listeners[0] = httpListener;
          return this.apiV2Service.update(this.apiId, { ...api, listeners });
        }),
        tap(api => {
          this.snackBarService.success('MCP entrypoint has been updated successfully.');
          this.updateFormValues(api as ApiV4);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  disableMcpEntrypoint($event: MatSlideToggleChange) {
    const removeMcpEntrypoint$ = this.apiV2Service.get(this.apiId).pipe(
      switchMap((api: ApiV4) => {
        const listeners = api.listeners;
        const httpListener = listeners[0] as HttpListener;

        // Remove MCP entrypoint
        httpListener.entrypoints = httpListener.entrypoints.filter(entrypoint => entrypoint.type !== MCP_ENTRYPOINT_ID);

        return this.apiV2Service.update(this.apiId, { ...api, listeners });
      }),
      tap(() => {
        this.snackBarService.success('MCP has been disabled successfully.');
        this.ngOnInit();
      }),
    );

    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Disable MCP entrypoint',
          content: 'Do you really want to disable the MCP entrypoint? This will remove all tools and configurations.',
          confirmButton: 'Disable',
        },
      })
      .afterClosed()
      .pipe(
        switchMap(confirmed => {
          if (confirmed) {
            return removeMcpEntrypoint$;
          }
          // If not confirmed, return an empty observable to avoid further actions
          // Revert the toggle state if the user cancels
          $event.checked = true;
          $event.source.checked = true;
          return of(false);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private updateFormValues(api: ApiV4): void {
    const mcpConfiguration = api.listeners[0].entrypoints.find(e => e.type === MCP_ENTRYPOINT_ID).configuration as MCPConfiguration;
    this.form.patchValue({
      mcpConfig: mcpConfiguration,
    });
    this.form.markAsPristine();
    this.formInitialValues = this.form.getRawValue();
  }
}
