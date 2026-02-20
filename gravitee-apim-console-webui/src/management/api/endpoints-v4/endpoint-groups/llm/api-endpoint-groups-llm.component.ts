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
import { Component, computed, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { GioConfirmDialogComponent, GioConfirmDialogData, GioLicenseService } from '@gravitee/ui-particles-angular';
import { catchError, filter, map, switchMap } from 'rxjs/operators';
import { remove } from 'lodash';
import { combineLatest, EMPTY } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';

import { Model, Provider, toProviders } from './api-endpoint-groups-llm.adapter';

import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ApiV4, ConnectorPlugin, UpdateApi } from '../../../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../../../services-ngx/icon.service';
import { ApimFeature, UTMTags } from '../../../../../shared/components/gio-license/gio-license-data';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';

type ConnectorPluginWithIcon = ConnectorPlugin & { icon: string };

@Component({
  selector: 'api-endpoint-groups-llm',
  templateUrl: './api-endpoint-groups-llm.component.html',
  styleUrls: ['./api-endpoint-groups-llm.component.scss'],
  standalone: false,
})
export class ApiEndpointGroupsLlmComponent {
  public api = input.required<ApiV4>();
  private apiType: ApiV4['type'];
  private readonly destroyRef = inject(DestroyRef);
  private readonly permissionService = inject(GioPermissionService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly matDialog = inject(MatDialog);
  private readonly apiService = inject(ApiV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly connectorPluginsV2Service = inject(ConnectorPluginsV2Service);
  private readonly iconService = inject(IconService);
  private readonly licenseService = inject(GioLicenseService);

  public providersTableData = signal<Provider[]>([]);
  public license$ = this.licenseService.getLicense$();
  public isOEM$ = this.licenseService.isOEM$();
  public plugins$ = this.connectorPluginsV2Service
    .listEndpointPlugins()
    .pipe(map((plugins: ConnectorPlugin[]) => this.transformPluginsToMap(plugins)));
  public shouldUpgrade$ = combineLatest([this.plugins$, toObservable(this.providersTableData)]).pipe(
    map(([plugins, providers]): boolean => providers.length > 0 && !plugins.get('llm-proxy')?.deployed),
  );
  public isReadOnly = computed(() => {
    const canUpdate = this.permissionService.hasAnyMatching(['api-definition-u']);
    const api = this.api();
    return api?.definitionContext?.origin === 'KUBERNETES' || !canUpdate;
  });
  public llmProxyDisplayedColumns = ['model', 'capabilities', 'costInput', 'costOutput'];
  private readonly messageLicenseOptions = {
    feature: ApimFeature.APIM_EN_MESSAGE_REACTOR,
    context: UTMTags.GENERAL_ENDPOINT_CONFIG,
  };
  private llmProxyLicenseOptions = {
    feature: ApimFeature.APIM_LLM_PROXY_REACTOR,
    context: UTMTags.GENERAL_ENDPOINT_CONFIG,
  };
  constructor() {
    effect(() => {
      const apiValue = this.api();
      this.apiType = apiValue?.type;
      if (apiValue) {
        this.providersTableData.set(toProviders(apiValue));
      }
    });
  }

  public deleteGroup(groupName: string): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete Endpoint Group',
          content: `Are you sure you want to delete the Group <strong>${groupName}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteEndpointGroupConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.apiService.get(this.activatedRoute.snapshot.params.apiId)),
        switchMap((api) => {
          const apiV4 = api as ApiV4;
          const endpointGroups = apiV4.endpointGroups || [];
          remove(endpointGroups, (g) => g.name === groupName);
          return this.apiService.update(apiV4.id, { ...apiV4, endpointGroups } as UpdateApi);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(
            error.message === 'Validation error' ? `${error.message}: ${error.details[0].message}` : error.message,
          );
          return EMPTY;
        }),
        switchMap(() => this.apiService.get(this.activatedRoute.snapshot.params.apiId)),
        map((refreshedApi) => {
          const apiV4 = refreshedApi as ApiV4;
          this.providersTableData.set(toProviders(apiV4));
          this.snackBarService.success(`Provider ${groupName} successfully deleted!`);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  public onRequestUpgrade() {
    this.licenseService.openDialog(this.apiType === 'LLM_PROXY' ? this.llmProxyLicenseOptions : this.messageLicenseOptions);
  }

  public getCapabilityBadges(model: Model): string[] {
    const badges: string[] = [];
    if (model.streaming) badges.push('Streaming');
    if (model.thinking) badges.push('Thinking');
    if (model.functionCalling) badges.push('Function Calling');
    if (model.contextWindowSize) badges.push(`${(model.contextWindowSize / 1000).toFixed(0)}k ctx`);
    for (const ep of model.supportedEndpoints ?? []) {
      badges.push(ep.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (l) => l.toUpperCase()));
    }
    for (const mod of model.inputModalities ?? []) {
      badges.push(`In: ${mod.charAt(0) + mod.slice(1).toLowerCase()}`);
    }
    for (const mod of model.outputModalities ?? []) {
      badges.push(`Out: ${mod.charAt(0) + mod.slice(1).toLowerCase()}`);
    }
    return badges;
  }

  public getProviderTypeDisplayName(providerType: string): string {
    switch (providerType) {
      case 'OPEN_AI_COMPATIBLE':
        return 'OpenAI Compatible';
      case 'OPEN_AI':
        return 'OpenAI';
      default:
        return providerType.replace(/_/g, ' ').replace(/\b\w/g, (l) => l.toUpperCase());
    }
  }

  private transformPluginsToMap(plugins: ConnectorPlugin[]): Map<string, ConnectorPluginWithIcon> {
    return new Map(
      plugins
        .filter((plugin) => plugin.id && plugin.icon)
        .map((plugin) => [
          plugin.id!,
          {
            ...plugin,
            icon: this.iconService.registerSvg(plugin.id!, plugin.icon!),
          } as ConnectorPluginWithIcon,
        ]),
    );
  }
}
