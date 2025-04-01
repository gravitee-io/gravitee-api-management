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
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { EMPTY, forkJoin, Observable, of, Subject } from 'rxjs';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import {
  GIO_DIALOG_WIDTH,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioLicenseService,
  License,
} from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { flatten, isEmpty, remove } from 'lodash';
import { ActivatedRoute } from '@angular/router';

import { ApiEntrypointsV4AddDialogComponent, ApiEntrypointsV4AddDialogComponentData } from './edit/api-entrypoints-v4-add-dialog.component';

import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import {
  Api,
  ApiV4,
  ConnectorPlugin,
  Entrypoint,
  HttpListener,
  Listener,
  PathV4,
  UpdateApiV4,
  ConnectorVM,
  fromConnector,
  TcpListener,
  KafkaHost,
  KafkaListener,
} from '../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../services-ngx/icon.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApimFeature, UTMTags } from '../../../shared/components/gio-license/gio-license-data';
import { RestrictedDomainService } from '../../../services-ngx/restricted-domain.service';
import { TcpHost } from '../../../entities/management-api-v2/api/v4/tcpHost';
import { PortalConfigurationService } from '../../../services-ngx/portal-configuration.service';
import { PortalSettingsPortal } from '../../../entities/portal/portalSettings';

type EntrypointVM = {
  id: string;
  icon: string;
  type: string;
  qos: string;
};
@Component({
  selector: 'api-entrypoints-v4-general',
  templateUrl: './api-entrypoints-v4-general.component.html',
  styleUrls: ['./api-entrypoints-v4-general.component.scss'],
  standalone: false,
})
export class ApiEntrypointsV4GeneralComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public apiId: string;
  public envId: string;
  public api: ApiV4;
  public formGroup: UntypedFormGroup;
  public pathsFormControl: UntypedFormControl;
  public hostsFormControl: UntypedFormControl;
  public hostFormControl: UntypedFormControl;
  public displayedColumns = ['type', 'qos', 'actions'];
  public dataSource: EntrypointVM[] = [];
  private allEntrypoints: ConnectorPlugin[];
  public enableVirtualHost = false;
  public apiExistingPaths: PathV4[] = [];
  public apiExistingHosts: TcpHost[] = [];
  public apiExistingKafkaHost: KafkaHost = {};
  public domainRestrictions: string[] = [];
  public entrypointAvailableForAdd: ConnectorVM[] = [];
  public shouldUpgrade = false;
  public license$: Observable<License>;
  public isOEM$: Observable<boolean>;
  public isReadOnly = false;
  public portalSettings$: Observable<PortalSettingsPortal>;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly restrictedDomainService: RestrictedDomainService,
    private readonly formBuilder: UntypedFormBuilder,
    private readonly iconService: IconService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    private readonly permissionService: GioPermissionService,
    private readonly changeDetector: ChangeDetectorRef,
    private readonly licenseService: GioLicenseService,
    private readonly portalConfigurationService: PortalConfigurationService,
  ) {
    this.apiId = this.activatedRoute.snapshot.params.apiId;
    this.envId = this.activatedRoute.snapshot.params.envHrid;
  }

  ngOnInit(): void {
    this.portalSettings$ = this.portalConfigurationService.getByEnvironmentId(this.envId).pipe(map(({ portal }) => portal));

    forkJoin([this.restrictedDomainService.get(), this.apiService.get(this.apiId), this.connectorPluginsV2Service.listEntrypointPlugins()])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([restrictedDomains, api, availableEntrypoints]) => {
        this.domainRestrictions = restrictedDomains.map((value) => value.domain) || [];

        this.isReadOnly = api.definitionContext?.origin === 'KUBERNETES' || !this.permissionService.hasAnyMatching(['api-definition-u']);

        if (api.definitionVersion === 'V4') {
          this.allEntrypoints = availableEntrypoints.filter((entrypoint) => entrypoint.supportedApiType === api.type);
          this.initForm(api);
        }
      });
  }

  private initForm(api: ApiV4) {
    if (api.type === 'MESSAGE' || api.type === 'NATIVE') {
      const selectedEntrypoints = flatten(api.listeners.map((l) => l.entrypoints)).map((e) => e.type);
      this.shouldUpgrade = this.allEntrypoints.filter((e) => selectedEntrypoints.includes(e.id)).some(({ deployed }) => !deployed);
      this.license$ = this.licenseService.getLicense$();
      this.isOEM$ = this.licenseService.isOEM$();
    }

    this.api = api as ApiV4;
    this.formGroup = new UntypedFormGroup({});

    const httpListeners = this.api.listeners.filter((listener) => listener.type === 'HTTP') ?? [];
    if (httpListeners.length > 0) {
      this.apiExistingPaths = httpListeners.flatMap((listener) => {
        return (listener as HttpListener).paths;
      });
      this.pathsFormControl = this.formBuilder.control({ value: this.apiExistingPaths, disabled: this.isReadOnly }, Validators.required);
      this.formGroup.addControl('paths', this.pathsFormControl);
      this.enableVirtualHost = this.apiExistingPaths.some((path) => path.host !== undefined);
    } else {
      this.apiExistingPaths = [];
      this.formGroup.removeControl('paths');
      this.enableVirtualHost = false;
    }

    const tcpListeners = this.api.listeners.filter((listener) => listener.type === 'TCP') ?? [];
    if (tcpListeners.length > 0) {
      this.apiExistingHosts = tcpListeners.flatMap((listener) => {
        return (listener as TcpListener).hosts.map((host) => {
          return <TcpHost>{ host };
        });
      });
      this.hostsFormControl = this.formBuilder.control({ value: this.apiExistingHosts, disabled: this.isReadOnly }, Validators.required);
      this.formGroup.addControl('hosts', this.hostsFormControl);
    } else {
      this.apiExistingHosts = [];
      this.formGroup.removeControl('hosts');
    }

    const kafkaListener: KafkaListener = this.api.listeners.find((listener) => listener.type === 'KAFKA');
    if (kafkaListener) {
      this.apiExistingKafkaHost = { host: kafkaListener.host ?? '' };
      this.hostFormControl = this.formBuilder.control({ value: this.apiExistingKafkaHost, disabled: this.isReadOnly }, Validators.required);
      this.formGroup.addControl('host', this.hostFormControl);
    } else {
      this.apiExistingKafkaHost = {};
      this.formGroup.removeControl('host');
    }

    const existingEntrypoints = flatten(this.api.listeners.map((l) => l.entrypoints)).map((e) => e.type);
    this.entrypointAvailableForAdd = this.allEntrypoints
      .filter((entrypoint) => !existingEntrypoints.includes(entrypoint.id))
      .map((entrypoint) => fromConnector(this.iconService, entrypoint));
    const entrypoints = this.api.listeners.flatMap((l) => l.entrypoints);
    this.dataSource = entrypoints
      .map((entrypoint) => {
        const matchingEntrypoint = this.allEntrypoints.find((e) => e.id === entrypoint.type);
        if (matchingEntrypoint) {
          const entry: EntrypointVM = {
            id: entrypoint.type,
            icon: this.iconService.registerSvg(matchingEntrypoint.id, matchingEntrypoint.icon),
            type: matchingEntrypoint.name,
            qos: entrypoint.qos,
          };
          return entry;
        }
      })
      .sort((a, b) => a.id.localeCompare(b.id));
    this.changeDetector.detectChanges();
  }

  onDelete(elementToRemove: EntrypointVM) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          title: 'Delete Entrypoint',
          content: `Are you sure you want to delete your <strong>${elementToRemove.type}</strong> entrypoint?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteEntrypointConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.apiService.get(this.api.id)),
        switchMap((api: ApiV4) => {
          api.listeners.forEach((listener) => {
            remove(listener.entrypoints, (e) => e.type === elementToRemove.id);
          });

          const updateApi: UpdateApiV4 = {
            ...(api as ApiV4),
            listeners: [...api.listeners].filter((listener) => listener.entrypoints.length > 0),
          };
          return this.apiService.update(api.id, updateApi);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(
            error.message === 'Validation error' ? `${error.message}: ${error.details[0].message}` : error.message,
          );
          return EMPTY;
        }),
        tap(() => this.snackBarService.success(`${elementToRemove.type} entrypoint successfully deleted!`)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((api) => {
        this.initForm(api as ApiV4);
      });
  }

  addNewEntrypoint() {
    const hasHttpListener = this.api.listeners.find((l) => l.type === 'HTTP') !== undefined;
    // Show dialog to add a new entrypoint
    this.matDialog
      .open<ApiEntrypointsV4AddDialogComponent, ApiEntrypointsV4AddDialogComponentData>(ApiEntrypointsV4AddDialogComponent, {
        data: { entrypoints: this.entrypointAvailableForAdd.sort((e1, e2) => e1.name.localeCompare(e2.name)), hasHttpListener },
        width: GIO_DIALOG_WIDTH.LARGE,
      })
      .afterClosed()
      .pipe(
        switchMap((dialogRes) => {
          if (dialogRes && !isEmpty(dialogRes.selectedEntrypoints)) {
            // Save new entrypoint with default config
            return this.addEntrypointsToApi(dialogRes.selectedEntrypoints, dialogRes.paths);
          }
          return EMPTY;
        }),
        tap(() => {
          this.snackBarService.success('New entrypoint successfully added!');
        }),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? err.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((api) => {
        // Update page
        this.initForm(api as ApiV4);
      });
  }

  onSaveChanges() {
    const formValue = this.formGroup.getRawValue();
    this.apiService
      .get(this.apiId)
      .pipe(
        switchMap((api) => {
          const currentHttpListener = this.api.listeners.find((listener) => listener.type === 'HTTP');
          const updatedHttpListener: HttpListener = {
            ...currentHttpListener,
            paths: this.enableVirtualHost
              ? formValue.paths?.map(({ path, host, overrideAccess }) => ({ path, host, overrideAccess }))
              : formValue.paths?.map(({ path }) => ({ path })),
            entrypoints: currentHttpListener?.entrypoints,
          };
          const currentTcpListener = this.api.listeners.find((listener) => listener.type === 'TCP');
          const updatedTcpListener: TcpListener = {
            ...currentTcpListener,
            hosts: formValue.hosts?.map((host) => host.host),
            entrypoints: currentTcpListener?.entrypoints,
          };

          const currentKafkaListener = this.api.listeners.find((listener) => listener.type === 'KAFKA');
          const updatedKafkaListener: KafkaListener = {
            ...currentKafkaListener,
            host: formValue.host?.host,
            entrypoints: currentKafkaListener?.entrypoints,
          };

          const updateApi: UpdateApiV4 = {
            ...(api as ApiV4),
            listeners: [
              updatedHttpListener,
              updatedTcpListener,
              updatedKafkaListener,
              ...this.api.listeners.filter((listener) => listener.type !== 'HTTP' && listener.type !== 'TCP' && listener.type !== 'KAFKA'),
            ].filter((listener) => listener?.entrypoints?.length > 0),
          };

          return this.apiService.update(this.apiId, updateApi);
        }),
        tap(() => {
          if (this.apiExistingHosts?.length > 0 || !!this.apiExistingKafkaHost.host) {
            this.snackBarService.success('Host configuration successfully saved!');
          } else {
            this.snackBarService.success('Context-path configuration successfully saved!');
          }
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
            content: `By moving back to context-path you will lose all virtual-hosts. Are you sure to continue?`,
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

  private addEntrypointsToApi(entrypointsToAdd: string[], paths: PathV4[]): Observable<Api> {
    if (isEmpty(entrypointsToAdd)) {
      return of(this.api);
    }

    return this.apiService.get(this.apiId).pipe(
      switchMap((api: ApiV4) => {
        const entrypointVMToAdd: ConnectorPlugin[] = this.allEntrypoints.filter((e) => entrypointsToAdd.includes(e.id));
        const allListenerTypes = [
          ...new Set([...api.listeners.map((l) => l.type), ...entrypointVMToAdd.map(({ supportedListenerType }) => supportedListenerType)]),
        ];
        const updatedListeners: Listener[] = allListenerTypes.reduce((listeners: Listener[], listenerType) => {
          const existingListener: Listener = api.listeners.find((l) => l.type === listenerType);
          const emptyListener: Listener = listenerType === 'HTTP' ? { type: listenerType, paths } : { type: listenerType };
          const listener = existingListener ?? emptyListener;
          const existingEntrypoints: Entrypoint[] = existingListener?.entrypoints ?? [];
          const entrypointsToAdd: Entrypoint[] = entrypointVMToAdd
            .filter((e) => e.supportedListenerType === listenerType)
            .map((e) => {
              const newEntrypoint: Entrypoint = { type: e.id, configuration: {} };
              return newEntrypoint;
            });

          return [
            ...listeners,
            {
              ...listener,
              entrypoints: [...existingEntrypoints, ...entrypointsToAdd],
            },
          ];
        }, [] as Listener[]);

        const updateApi: UpdateApiV4 = {
          ...api,
          listeners: updatedListeners,
        };
        return this.apiService.update(this.apiId, updateApi);
      }),
      takeUntil(this.unsubscribe$),
    );
  }

  onReset() {
    this.formGroup.reset();
    this.initForm(this.api);
  }

  onRequestUpgrade() {
    this.licenseService.openDialog({
      feature: this.api.type === 'NATIVE' ? ApimFeature.APIM_NATIVE_KAFKA_REACTOR : ApimFeature.APIM_EN_MESSAGE_REACTOR,
      context: UTMTags.GENERAL_ENTRYPOINT_CONFIG,
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
