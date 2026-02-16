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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { GioConfirmDialogComponent, GioConfirmDialogData, GioLicenseService, License } from '@gravitee/ui-particles-angular';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { find, remove } from 'lodash';
import { combineLatest, EMPTY, Observable, Subject } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

import { EndpointGroup, toEndpoints } from './api-endpoint-groups-standard.adapter';

import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ApiV4, ConnectorPlugin, UpdateApi } from '../../../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../../../services-ngx/icon.service';
import { ApimFeature, UTMTags } from '../../../../../shared/components/gio-license/gio-license-data';
import { AGENT_TO_AGENT } from '../../../../../entities/management-api-v2/api/v4/agentToAgent';
import { disableDlqEntrypoint, getMatchingDlqEntrypoints, getMatchingDlqEntrypointsForGroup } from '../../api-endpoint-v4-matching-dlq';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { TenantService } from '../../../../../services-ngx/tenant.service';
import { Tenant } from '../../../../../entities/tenant/tenant';

@Component({
  selector: 'api-endpoint-groups-standard',
  templateUrl: './api-endpoint-groups-standard.component.html',
  styleUrls: ['./api-endpoint-groups-standard.component.scss'],
  standalone: false,
})
export class ApiEndpointGroupsStandardComponent implements OnInit, OnDestroy {
  public groupsTableData: EndpointGroup[];
  public plugins: Map<string, ConnectorPlugin>;
  public api: ApiV4;
  public isReadOnly = true;
  public isReordering = false;
  public shouldUpgrade = false;
  public license$: Observable<License>;
  public isOEM$: Observable<boolean>;
  public isA2ASelcted: boolean;

  private messageLicenseOptions = {
    feature: ApimFeature.APIM_EN_MESSAGE_REACTOR,
    context: UTMTags.GENERAL_ENDPOINT_CONFIG,
  };
  private llmProxyLicenseOptions = {
    feature: ApimFeature.APIM_LLM_PROXY_REACTOR,
    context: UTMTags.GENERAL_ENDPOINT_CONFIG,
  };
  private nativeLicenseOptions = {
    feature: ApimFeature.APIM_NATIVE_KAFKA_REACTOR,
    context: UTMTags.GENERAL_ENDPOINT_CONFIG,
  };

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private permissionService: GioPermissionService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly matDialog: MatDialog,
    private readonly apiService: ApiV2Service,
    private readonly tenantService: TenantService,
    private readonly snackBarService: SnackBarService,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly iconService: IconService,
    private readonly licenseService: GioLicenseService,
  ) {}

  public ngOnInit(): void {
    combineLatest([
      this.apiService.get(this.activatedRoute.snapshot.params.apiId),
      this.connectorPluginsV2Service.listEndpointPlugins(),
      this.tenantService.list(),
    ])
      .pipe(
        tap(([apiV4, plugins, tenants]: [ApiV4, ConnectorPlugin[], Tenant[]]) => {
          this.api = apiV4;
          this.isA2ASelcted = this.api.listeners?.some(listener => listener.entrypoints?.some(ep => ep.type === AGENT_TO_AGENT.id));
          this.groupsTableData = toEndpoints(apiV4, tenants);

          const canUpdate = this.permissionService.hasAnyMatching(['api-definition-u']);
          this.isReadOnly = this.api.definitionContext?.origin === 'KUBERNETES' || !canUpdate;
          this.plugins = new Map(
            plugins.map(plugin => [
              plugin.id,
              {
                ...plugin,
                icon: this.iconService.registerSvg(plugin.id, plugin.icon),
              },
            ]),
          );

          this.shouldUpgrade = this.groupsTableData.some(group => !this.plugins.get(group.type)?.deployed);

          this.license$ = this.licenseService.getLicense$();
          this.isOEM$ = this.licenseService.isOEM$();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  public deleteGroup(groupName: string): void {
    const matchingDlqEntrypoint = getMatchingDlqEntrypointsForGroup(this.api, groupName);
    const dlqDeleteMessage =
      matchingDlqEntrypoint.length > 0
        ? '<br> This endpoint group is used as dead letter queue. Deleting it will disable dead letter queue for related entrypoints.'
        : '';
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete Endpoint Group',
          content: `Are you sure you want to delete the Group <strong>${groupName}</strong>?${dlqDeleteMessage}`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteEndpointGroupConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.apiService.get(this.activatedRoute.snapshot.params.apiId)),
        switchMap((api: ApiV4) => {
          remove(api.endpointGroups, g => g.name === groupName);
          disableDlqEntrypoint(api, matchingDlqEntrypoint);
          return this.apiService.update(api.id, { ...api } as UpdateApi);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(
            error.message === 'Validation error' ? `${error.message}: ${error.details[0].message}` : error.message,
          );
          return EMPTY;
        }),
        tap(() => {
          this.ngOnInit();
        }),
        map(() => {
          this.snackBarService.success(`Endpoint group ${groupName} successfully deleted!`);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public deleteEndpoint(groupName: string, endpointName: string): void {
    const matchingDlqEntrypoint = getMatchingDlqEntrypoints(this.api, endpointName);
    const dlqDeleteMessage =
      matchingDlqEntrypoint.length > 0
        ? '<br> This endpoint is used as dead letter queue. Deleting it will disable dead letter queue for related entrypoints.'
        : '';
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete Endpoint',
          content: `Are you sure you want to delete the Endpoint <strong>${endpointName}</strong>?${dlqDeleteMessage}`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteEndpointConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.apiService.get(this.activatedRoute.snapshot.params.apiId)),
        switchMap((api: ApiV4) => {
          remove(find(api.endpointGroups, g => g.name === groupName).endpoints, e => e.name === endpointName);
          disableDlqEntrypoint(api, matchingDlqEntrypoint);
          return this.apiService.update(api.id, { ...api } as UpdateApi);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(
            error.message === 'Validation error' ? `${error.message}: ${error.details[0].message}` : error.message,
          );
          return EMPTY;
        }),
        tap(() => {
          this.ngOnInit();
        }),
        map(() => {
          this.snackBarService.success(`Endpoint ${endpointName} successfully deleted!`);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public reorderEndpointGroup(oldIndex: number, newIndex: number): void {
    this.isReordering = true;
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          api.endpointGroups.splice(newIndex, 0, api.endpointGroups.splice(oldIndex, 1)[0]);
          return this.apiService.update(api.id, { ...api } as UpdateApi);
        }),
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Endpoint group reordered successfully');
          this.isReordering = false;
        },
        error: ({ error }) => {
          this.isReordering = false;
          this.snackBarService.error(error.message);
        },
      });
  }

  public onRequestUpgrade() {
    this.licenseService.openDialog(
      this.api.type === 'NATIVE'
        ? this.nativeLicenseOptions
        : this.api.type === 'LLM_PROXY'
          ? this.llmProxyLicenseOptions
          : this.messageLicenseOptions,
    );
  }

  dropRow(event: CdkDragDrop<string[]>, groupName: string) {
    this.isReordering = true;

    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const groupToUpdate = api.endpointGroups.find(g => g.name === groupName);
          if (!groupToUpdate) {
            // Normally not happen. to help debugging if needed
            throw new Error('Group not found!');
          }
          if (event.previousIndex === event.currentIndex) {
            // No change, do nothing. Juste complete observable
            return EMPTY;
          }

          groupToUpdate.endpoints.splice(event.currentIndex, 0, groupToUpdate.endpoints.splice(event.previousIndex, 1)[0]);

          return this.apiService.update(api.id, { ...api } as UpdateApi);
        }),
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Endpoint reordered successfully');
        },
        error: ({ error }) => {
          this.isReordering = false;
          this.snackBarService.error(error.message);
        },
        complete: () => {
          this.isReordering = false;
        },
      });
  }
}
