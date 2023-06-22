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
import { ChangeDetectorRef, Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { EMPTY, forkJoin, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';

import { UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { ApiV4, ConnectorPlugin, HttpListener, PathV4, UpdateApiV4 } from '../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../services-ngx/icon.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { EnvironmentService } from '../../../services-ngx/environment.service';

type EntrypointVM = {
  id: string;
  icon: string;
  type: string;
};
@Component({
  selector: 'api-entrypoints-v4-general',
  template: require('./api-entrypoints-v4-general.component.html'),
  styles: [require('./api-entrypoints-v4-general.component.scss')],
})
export class ApiEntrypointsV4GeneralComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public apiId: string;
  public api: ApiV4;
  public formGroup: FormGroup;
  public pathsFormControl: FormControl;
  public displayedColumns = ['type', 'actions'];
  public dataSource: EntrypointVM[] = [];
  private allEntrypoints: ConnectorPlugin[];
  public enableVirtualHost = false;
  public apiExistingPaths: PathV4[] = [];
  public domainRestrictions: string[] = [];
  public entrypointToBeRemoved: string[] = [];
  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiV2Service,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly environmentService: EnvironmentService,
    private readonly formBuilder: FormBuilder,
    private readonly iconService: IconService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    private readonly changeDetector: ChangeDetectorRef,
  ) {
    this.apiId = this.ajsStateParams.apiId;
  }

  ngOnInit(): void {
    forkJoin([
      this.environmentService.getCurrent(),
      this.apiService.get(this.apiId),
      this.connectorPluginsV2Service.listAsyncEntrypointPlugins(),
    ])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([environment, api, availableEntrypoints]) => {
        this.domainRestrictions = environment.domainRestrictions || [];

        if (api.definitionVersion === 'V4') {
          this.allEntrypoints = availableEntrypoints;
          this.initForm(api);
        }
      });
  }

  private initForm(api: ApiV4) {
    this.api = api as ApiV4;
    this.formGroup = new FormGroup({});

    const httpListeners = this.api.listeners.filter((listener) => listener.type === 'HTTP');
    if (httpListeners.length > 0) {
      this.apiExistingPaths = httpListeners.flatMap((listener) => {
        return (listener as HttpListener).paths;
      });
      this.pathsFormControl = this.formBuilder.control(this.apiExistingPaths, Validators.required);
      this.formGroup.addControl('paths', this.pathsFormControl);
      this.enableVirtualHost = this.apiExistingPaths.some((path) => path.host !== undefined);
    } else {
      this.enableVirtualHost = false;
    }

    const entrypoints = this.api.listeners.flatMap((l) => l.entrypoints);
    this.dataSource = entrypoints
      .map((entrypoint) => {
        const matchingEntrypoint = this.allEntrypoints.find((e) => e.id === entrypoint.type);
        if (matchingEntrypoint) {
          const entry: EntrypointVM = {
            id: entrypoint.type,
            icon: this.iconService.registerSvg(matchingEntrypoint.id, matchingEntrypoint.icon),
            type: matchingEntrypoint.name,
          };
          return entry;
        }
      })
      .sort((a, b) => a.id.localeCompare(b.id));
    this.changeDetector.detectChanges();
  }

  onEdit(element: EntrypointVM) {
    throw new Error(`Edit not implemented yet ${element}`);
  }

  onDelete(elementToRemove: EntrypointVM) {
    this.entrypointToBeRemoved.push(elementToRemove.id);
    this.dataSource = this.dataSource.filter((e) => e !== elementToRemove);
  }

  addNewEntrypoint() {
    throw new Error('Add new entrypoint not implemented yet');
  }

  onSaveChanges() {
    const formValue = this.formGroup.getRawValue();
    const currentHttpListener = this.api.listeners.find((listener) => listener.type === 'HTTP');
    const updatedHttpListener: HttpListener = {
      ...currentHttpListener,
      paths: this.enableVirtualHost
        ? formValue.paths.map(({ path, host, overrideAccess }) => ({ path, host, overrideAccess }))
        : formValue.paths.map(({ path }) => ({ path })),
      entrypoints: [...currentHttpListener.entrypoints.filter((listener) => !this.entrypointToBeRemoved.includes(listener.type))],
    };
    this.apiService
      .get(this.apiId)
      .pipe(
        switchMap((api) => {
          const updateApi: UpdateApiV4 = {
            ...(api as ApiV4),
            listeners: [
              updatedHttpListener,
              ...this.api.listeners
                .filter((listener) => listener.type !== 'HTTP')
                .map((l) => {
                  return { ...l, entrypoints: l.entrypoints.filter((listener) => !this.entrypointToBeRemoved.includes(listener.type)) };
                })
                .filter((listener) => listener.entrypoints.length > 0),
            ],
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
        takeUntil(this.unsubscribe$),
      )
      .subscribe((api) => {
        this.initForm(api as ApiV4);
      });
  }

  switchEntrypointsMode() {
    if (this.enableVirtualHost) {
      this.matDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
          width: '500px',
          data: {
            title: 'Switch to context-path mode',
            content: `By moving back to context-path you will loose all virtual-hosts. Are you sure to continue?`,
            confirmButton: 'Switch',
          },
          role: 'alertdialog',
          id: 'switchContextPathConfirmDialog',
        })
        .afterClosed()
        .pipe(
          tap((response) => {
            if (response) {
              // Keep only the path
              const currentValue = this.formGroup.getRawValue().paths;
              this.formGroup.get('paths').setValue(currentValue.map(({ path }) => ({ path })));
              this.enableVirtualHost = !this.enableVirtualHost;
              this.changeDetector.detectChanges();
            }
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
      return;
    }

    this.enableVirtualHost = !this.enableVirtualHost;
  }
}
