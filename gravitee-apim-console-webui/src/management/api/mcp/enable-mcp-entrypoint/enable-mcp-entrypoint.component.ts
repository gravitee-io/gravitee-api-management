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
import { MatStepperModule } from '@angular/material/stepper';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { GioFormJsonSchemaModule, GioIconsModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { switchMap, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatFormFieldModule } from '@angular/material/form-field';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4, HttpListener } from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ConfigureMcpEntrypointComponent } from '../components/configure-mcp-entrypoint/configure-mcp-entrypoint.component';
import { DEFAULT_MCP_ENTRYPOINT_PATH, MCP_ENTRYPOINT_ID, MCPConfiguration } from '../../../../entities/entrypoint/mcp';

@Component({
  selector: 'enable-mcp-entrypoint',
  imports: [
    MatCardModule,
    MatStepperModule,
    MatButtonModule,
    ReactiveFormsModule,
    GioFormJsonSchemaModule,
    MatFormFieldModule,
    GioIconsModule,
    GioSaveBarModule,
    RouterLink,
    ConfigureMcpEntrypointComponent,
  ],
  templateUrl: './enable-mcp-entrypoint.component.html',
  styleUrl: './enable-mcp-entrypoint.component.scss',
})
export class EnableMcpEntrypointComponent implements OnInit {
  apiId: string = this.activatedRoute.snapshot.params['apiId'];

  form = new FormGroup({
    mcpConfig: new FormControl<MCPConfiguration>({
      tools: [],
      mcpPath: DEFAULT_MCP_ENTRYPOINT_PATH,
    }),
  });

  formInitialValues: { mcpConfig: MCPConfiguration };

  private destroyRef = inject(DestroyRef);

  constructor(
    private apiV2Service: ApiV2Service,
    private snackBarService: SnackBarService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.formInitialValues = this.form.getRawValue();
  }

  onSubmit() {
    const configuration: MCPConfiguration = this.form.value.mcpConfig;

    this.apiV2Service
      .get(this.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const listeners = api.listeners;
          const httpListener = listeners[0] as HttpListener;

          if (httpListener.entrypoints?.some(entrypoint => entrypoint.type === MCP_ENTRYPOINT_ID)) {
            httpListener.entrypoints = httpListener.entrypoints.map(entrypoint =>
              entrypoint.type === MCP_ENTRYPOINT_ID ? { ...entrypoint, configuration } : entrypoint,
            );
          } else {
            httpListener.entrypoints = [...(httpListener.entrypoints || []), { type: MCP_ENTRYPOINT_ID, configuration }];
          }

          listeners[0] = httpListener;
          return this.apiV2Service.update(this.apiId, { ...api, listeners });
        }),
        tap(() => {
          this.snackBarService.success('MCP has been enabled successfully.');
          this.router.navigate(['..'], { relativeTo: this.activatedRoute });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}
