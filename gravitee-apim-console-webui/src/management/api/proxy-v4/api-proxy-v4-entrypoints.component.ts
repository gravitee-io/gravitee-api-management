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
import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EMPTY, forkJoin, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { ApiV4, ConnectorPlugin, HttpListener, PathV4, UpdateApiV4 } from '../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../services-ngx/icon.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

type EntrypointVM = {
  id: string;
  icon: string;
  type: string;
};
@Component({
  selector: 'api-proxy-v4-entrypoints',
  template: require('./api-proxy-v4-entrypoints.component.html'),
  styles: [require('./api-proxy-v4-entrypoints.component.scss')],
})
export class ApiProxyV4EntrypointsComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public apiId: string;
  public api: ApiV4;
  public formGroup: FormGroup;
  public displayedColumns = ['type', 'actions'];
  public dataSource: EntrypointVM[] = [];
  private allEntrypoints: ConnectorPlugin[];
  public enableVirtualHost = false;
  public enableContextPath = true;
  public apiExistingPaths: PathV4[] = [];
  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiV2Service,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly formBuilder: FormBuilder,
    private readonly iconService: IconService,
    private readonly snackBarService: SnackBarService,
  ) {
    this.apiId = this.ajsStateParams.apiId;
  }

  ngOnInit(): void {
    forkJoin([this.apiService.get(this.apiId), this.connectorPluginsV2Service.listAsyncEntrypointPlugins()])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([api, availableEntrypoints]) => {
        if (api.definitionVersion === 'V4') {
          this.allEntrypoints = availableEntrypoints;
          this.initForm(api);
        }
      });
  }

  private initForm(api: ApiV4) {
    this.api = api as ApiV4;

    const httpListeners = this.api.listeners.filter((listener) => listener.type === 'HTTP');
    if (httpListeners.length > 0) {
      this.apiExistingPaths = httpListeners.flatMap((listener) => {
        return (listener as HttpListener).paths;
      });
      this.formGroup = new FormGroup({});
      this.formGroup.addControl('paths', this.formBuilder.control(this.apiExistingPaths, Validators.required));
      this.enableVirtualHost = this.apiExistingPaths.some((path) => path.host !== undefined);
    } else {
      this.enableContextPath = false;
    }

    const entrypoints = this.api.listeners.flatMap((l) => l.entrypoints);
    this.dataSource = entrypoints.map((entrypoint) => {
      const matchingEntrypoint = this.allEntrypoints.find((e) => e.id === entrypoint.type);
      if (matchingEntrypoint) {
        const entry: EntrypointVM = {
          id: entrypoint.type,
          icon: this.iconService.registerSvg(matchingEntrypoint.id, matchingEntrypoint.icon),
          type: matchingEntrypoint.name,
        };
        return entry;
      }
    });
  }

  onEdit(element: EntrypointVM) {
    throw new Error(`Edit not implemented yet ${element}`);
  }

  onDelete(element: EntrypointVM) {
    throw new Error(`Delete not implemented yet ${element}`);
  }

  addNewEntrypoint() {
    throw new Error('Add new entrypoint not implemented yet');
  }

  onSaveChanges() {
    const formValue = this.formGroup.getRawValue();
    const updatedHttpListener: HttpListener = {
      ...this.api.listeners.find((listener) => listener.type === 'HTTP'),
      paths: formValue.paths,
    };
    this.apiService
      .get(this.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((api) => {
          const updateApi: UpdateApiV4 = {
            ...(api as ApiV4),
            listeners: [...this.api.listeners.filter((listener) => listener.type !== 'HTTP'), updatedHttpListener],
          };

          return this.apiService.update(this.apiId, updateApi);
        }),
        tap(() => {
          this.snackBarService.success('Configuration successfully saved!');
        }),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? err.message);
          return EMPTY;
        }),
      )
      .subscribe((api) => {
        this.initForm(api as ApiV4);
      });
  }
}
