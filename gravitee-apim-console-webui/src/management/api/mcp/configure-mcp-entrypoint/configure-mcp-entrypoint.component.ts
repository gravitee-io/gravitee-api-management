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
import { GioFormJsonSchemaModule, GioIconsModule, GioJsonSchema, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { AsyncPipe } from '@angular/common';
import { EMPTY, Observable } from 'rxjs';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ConnectorPluginsV2Service } from '../../../../services-ngx/connector-plugins-v2.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4, HttpListener } from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

const MCP_ENTRYPOINT_ID = 'mcp';
interface ConfigurationForm {
  configuration: FormControl<unknown>;
}

@Component({
  selector: 'configure-mcp-entrypoint',
  imports: [
    MatCardModule,
    MatStepperModule,
    MatButtonModule,
    ReactiveFormsModule,
    GioFormJsonSchemaModule,
    AsyncPipe,
    GioIconsModule,
    GioSaveBarModule,
    RouterLink,
  ],
  templateUrl: './configure-mcp-entrypoint.component.html',
  styleUrl: './configure-mcp-entrypoint.component.scss',
})
export class ConfigureMcpEntrypointComponent implements OnInit {
  creationMode: boolean = this.activatedRoute.snapshot.data['creationMode'] ?? false;
  apiId: string = this.activatedRoute.snapshot.params['apiId'];

  form: FormGroup<ConfigurationForm> = new FormGroup<ConfigurationForm>({
    configuration: new FormControl(null),
  });

  formInitialValues: { configuration: unknown };

  mcpEntrypointSchema$: Observable<GioJsonSchema> = inject(ConnectorPluginsV2Service)
    .getEntrypointPluginSchema(MCP_ENTRYPOINT_ID)
    .pipe(
      catchError((_) => {
        this.networkError = true;
        return EMPTY;
      }),
    );

  networkError: boolean = false;

  private destroyRef = inject(DestroyRef);

  constructor(
    private apiV2Service: ApiV2Service,
    private snackBarService: SnackBarService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit() {
    if (this.creationMode) {
      this.formInitialValues = this.form.getRawValue();
    } else {
      this.apiV2Service
        .get(this.apiId)
        .pipe(
          tap((api: ApiV4) => {
            this.form.controls.configuration.setValue(api.listeners[0].entrypoints.find((e) => e.type === MCP_ENTRYPOINT_ID).configuration);
            this.formInitialValues = this.form.getRawValue();
          }),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe();
    }
  }

  onSubmit() {
    const configuration = this.form.getRawValue().configuration;
    this.apiV2Service
      .get(this.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const listeners = api.listeners;
          const httpListener = listeners[0] as HttpListener;

          if (httpListener.entrypoints?.some((entrypoint) => entrypoint.type === MCP_ENTRYPOINT_ID)) {
            httpListener.entrypoints = httpListener.entrypoints.map((entrypoint) =>
              entrypoint.type === MCP_ENTRYPOINT_ID ? { ...entrypoint, configuration } : entrypoint,
            );
          } else {
            httpListener.entrypoints = [...(httpListener.entrypoints || []), { type: MCP_ENTRYPOINT_ID, configuration }];
          }

          listeners[0] = httpListener;
          return this.apiV2Service.update(this.apiId, { ...api, listeners });
        }),
        tap(() => {
          const successMessage = this.creationMode ? 'MCP has been enabled successfully.' : 'MCP updated successfully.';
          this.snackBarService.success(successMessage);

          this.router.navigate(['..'], { relativeTo: this.activatedRoute });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  importToolsFromOpenAPI() {

  }
}
