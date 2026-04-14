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
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GioFormJsonSchemaComponent, GioJsonSchema } from '@gravitee/ui-particles-angular';
import { AbstractControl, UntypedFormControl, UntypedFormGroup, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { isEqual } from 'lodash';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4, EndpointGroupV4, EndpointV4, EndpointV4Default, UpdateApiV4 } from '../../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../../services-ngx/connector-plugins-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { isEndpointNameUniqueAndDoesNotMatchDefaultValue } from '../api-endpoint-v4-unique-name';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';

export type ProviderMode = 'create-group' | 'edit-group' | 'create-endpoint' | 'edit-endpoint';

@Component({
  selector: 'api-llm-provider',
  templateUrl: './api-llm-provider.component.html',
  styleUrls: ['./api-llm-provider.component.scss'],
  standalone: false,
})
export class ApiLlmProviderComponent implements OnInit {
  private providerIndex: number | null = null;
  private endpointIndex: number | null = null;
  private api: ApiV4;

  public provider: EndpointGroupV4 | null = null;
  public endpoint: EndpointV4 | null = null;
  public mode: ProviderMode = 'create-group';
  public isReadOnly = false;
  public formGroup: UntypedFormGroup;
  public providerSchema: { config: GioJsonSchema | null; sharedConfig: GioJsonSchema | null };
  public initialFormValue: any;
  public backPath = '../../';

  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly apiService = inject(ApiV2Service);
  private readonly connectorPluginsV2Service = inject(ConnectorPluginsV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly destroyRef: DestroyRef = inject(DestroyRef);

  public get pageTitle(): string {
    switch (this.mode) {
      case 'create-group':
        return 'New Provider';
      case 'edit-group':
        return 'Edit Provider Group';
      case 'create-endpoint':
        return 'New Endpoint';
      case 'edit-endpoint':
        return 'Edit Endpoint';
      default:
        return 'Provider';
    }
  }

  public get showConfiguration(): boolean {
    return this.mode !== 'edit-group';
  }

  public get showSharedConfiguration(): boolean {
    return this.mode === 'create-group' || this.mode === 'edit-group';
  }

  public ngOnInit(): void {
    this.detectMode();

    const apiId = this.activatedRoute.snapshot.params.apiId;

    this.apiService
      .get(apiId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (api: ApiV4) => this.initializeComponent(api),
      });
  }

  public onSave(): void {
    const formValue = this.formGroup.getRawValue();
    const cleanName = formValue.name.trim();
    const configuration = formValue.configuration || {};
    const sharedConfiguration = formValue.sharedConfigurationOverride || {};

    this.apiService
      .get(this.api.id)
      .pipe(
        switchMap(api => {
          const apiV4 = api as ApiV4;
          let updatedApi: UpdateApiV4;

          switch (this.mode) {
            case 'create-group':
              updatedApi = this.createNewProvider(apiV4, cleanName, configuration, sharedConfiguration);
              break;
            case 'edit-group':
              updatedApi = this.updateProviderGroup(apiV4, cleanName, sharedConfiguration);
              break;
            case 'create-endpoint':
              updatedApi = this.createEndpoint(apiV4, cleanName, configuration);
              break;
            case 'edit-endpoint':
              updatedApi = this.updateEndpoint(apiV4, cleanName, configuration);
              break;
          }

          return this.apiService.update(apiV4.id, updatedApi);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'An error occurred.');
          return EMPTY;
        }),
        map(() => {
          this.snackBarService.success(this.getSuccessMessage(cleanName));
          this.router.navigate([this.backPath], { relativeTo: this.activatedRoute });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private detectMode(): void {
    const params = this.activatedRoute.snapshot.params;
    const url = this.activatedRoute.snapshot.url;
    const lastSegment = url?.[url.length - 1]?.path;

    if (params.providerIndex === undefined) {
      this.mode = 'create-group';
      this.backPath = '../../';
    } else {
      this.providerIndex = +params.providerIndex;

      if (lastSegment === 'edit') {
        this.mode = 'edit-group';
        this.backPath = '../../../';
      } else if (lastSegment === 'new') {
        this.mode = 'create-endpoint';
        this.backPath = '../../../';
      } else if (params.endpointIndex !== undefined) {
        this.mode = 'edit-endpoint';
        this.endpointIndex = +params.endpointIndex;
        this.backPath = '../../';
      }
    }
  }

  private initializeComponent(api: ApiV4): void {
    this.api = api;

    const isKubernetesOrigin = api.definitionContext?.origin === 'KUBERNETES';
    const canUpdate = this.permissionService.hasAnyMatching(['api-definition-u']);
    this.isReadOnly = isKubernetesOrigin || !canUpdate;

    if (this.providerIndex !== null) {
      const endpointGroups = api.endpointGroups || [];
      this.provider = endpointGroups[this.providerIndex];

      if (!this.provider) {
        this.snackBarService.error(`Provider at index [ ${this.providerIndex} ] does not exist.`);
        this.router.navigate([this.backPath], { relativeTo: this.activatedRoute });
        return;
      }

      if (this.mode === 'edit-endpoint' && this.endpointIndex !== null) {
        this.endpoint = this.provider.endpoints?.[this.endpointIndex] || null;
        if (!this.endpoint) {
          this.snackBarService.error(`Endpoint at index [ ${this.endpointIndex} ] does not exist.`);
          this.router.navigate([this.backPath], { relativeTo: this.activatedRoute });
          return;
        }
      }
    }

    this.initForm();
    this.loadSchemas();
  }

  private initForm(): void {
    let nameValue = '';
    let configurationValue: any = {};
    let sharedConfigValue: any = {};

    switch (this.mode) {
      case 'edit-group':
        nameValue = this.provider?.name || '';
        sharedConfigValue = this.provider?.sharedConfiguration || {};
        break;
      case 'edit-endpoint':
        nameValue = this.endpoint?.name || '';
        configurationValue = this.endpoint?.configuration || {};
        break;
    }

    this.formGroup = new UntypedFormGroup(
      {
        name: new UntypedFormControl({ value: nameValue, disabled: this.isReadOnly }, [
          Validators.required,
          Validators.pattern(/^[^:]*$/),
          isEndpointNameUniqueAndDoesNotMatchDefaultValue(this.api, this.retrieveDisplayName()),
        ]),
        configuration: new UntypedFormControl({ value: configurationValue, disabled: this.isReadOnly }, [Validators.required]),
        sharedConfigurationOverride: new UntypedFormControl({ value: sharedConfigValue, disabled: this.isReadOnly }, [Validators.required]),
      },
      this.getProviderConsistencyValidators(),
    );

    this.initialFormValue = this.formGroup.getRawValue();
  }

  private retrieveDisplayName() {
    return this.mode === 'edit-group' ? this.provider?.name || '' : this.mode === 'edit-endpoint' ? this.endpoint?.name || '' : '';
  }

  private loadSchemas(): void {
    this.connectorPluginsV2Service
      .getEndpointPluginSchema('llm-proxy')
      .pipe(
        tap(config => {
          this.providerSchema = {
            ...this.providerSchema,
            config: config && GioFormJsonSchemaComponent.isDisplayable(config) ? config : null,
          };
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.connectorPluginsV2Service
      .getEndpointPluginSharedConfigurationSchema('llm-proxy')
      .pipe(
        tap(sharedConfig => {
          this.providerSchema = {
            ...this.providerSchema,
            sharedConfig: sharedConfig && GioFormJsonSchemaComponent.isDisplayable(sharedConfig) ? sharedConfig : null,
          };
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private createNewProvider(api: ApiV4, cleanName: string, configuration: any, sharedConfiguration: any): UpdateApiV4 {
    const newProvider: EndpointGroupV4 = {
      name: cleanName,
      type: 'llm-proxy',
      endpoints: [
        {
          ...EndpointV4Default.byTypeAndGroupName('llm-proxy', cleanName),
          configuration,
          ...(sharedConfiguration ? { sharedConfigurationOverride: sharedConfiguration } : {}),
        },
      ],
      ...(sharedConfiguration ? { sharedConfiguration } : {}),
    };

    const endpointGroups = api.endpointGroups || [];
    return {
      ...api,
      endpointGroups: [...endpointGroups, newProvider],
    };
  }

  private updateProviderGroup(api: ApiV4, cleanName: string, sharedConfiguration: any): UpdateApiV4 {
    if (!this.provider || this.providerIndex === null) {
      throw new Error('Cannot update provider: provider or index is null');
    }

    const updatedProvider: EndpointGroupV4 = {
      ...this.provider,
      name: cleanName,
      ...(sharedConfiguration ? { sharedConfiguration } : {}),
    };

    const endpointGroups = api.endpointGroups || [];
    return {
      ...api,
      endpointGroups: endpointGroups.map((group, i) => (i === this.providerIndex ? updatedProvider : group)),
    };
  }

  private createEndpoint(api: ApiV4, cleanName: string, configuration: any): UpdateApiV4 {
    if (!this.provider || this.providerIndex === null) {
      throw new Error('Cannot create endpoint: provider or index is null');
    }

    const newEndpoint: EndpointV4 = {
      ...EndpointV4Default.byTypeAndGroupName('llm-proxy', cleanName),
      name: cleanName,
      configuration,
      inheritConfiguration: true,
    };

    const updatedProvider: EndpointGroupV4 = {
      ...this.provider,
      endpoints: [...(this.provider.endpoints || []), newEndpoint],
    };

    const endpointGroups = api.endpointGroups || [];
    return {
      ...api,
      endpointGroups: endpointGroups.map((group, i) => (i === this.providerIndex ? updatedProvider : group)),
    };
  }

  private updateEndpoint(api: ApiV4, cleanName: string, configuration: any): UpdateApiV4 {
    if (!this.provider || this.providerIndex === null || this.endpointIndex === null) {
      throw new Error('Cannot update endpoint: provider, provider index, or endpoint index is null');
    }

    const endpoints = this.provider.endpoints || [];
    const updatedEndpoints = endpoints.map((ep, j) => (j === this.endpointIndex ? { ...ep, name: cleanName, configuration } : ep));

    const updatedProvider: EndpointGroupV4 = {
      ...this.provider,
      endpoints: updatedEndpoints,
    };

    const endpointGroups = api.endpointGroups || [];
    return {
      ...api,
      endpointGroups: endpointGroups.map((group, i) => (i === this.providerIndex ? updatedProvider : group)),
    };
  }

  private getProviderConsistencyValidators(): ((control: AbstractControl) => ValidationErrors | null)[] {
    if (this.mode !== 'create-endpoint' && this.mode !== 'edit-endpoint') {
      return [];
    }
    const existingEndpoints = this.provider?.endpoints || [];
    if (existingEndpoints.length === 0) {
      return [];
    }
    // Skip consistency checks when editing the only endpoint in a group
    if (this.mode === 'edit-endpoint' && existingEndpoints.length < 2) {
      return [];
    }
    const expectedAliases = this.collectAliases(existingEndpoints[0]?.configuration?.models);
    return [
      (control: AbstractControl): ValidationErrors | null => {
        const aliases = this.collectAliases(control.value?.configuration?.models);
        if (
          !isEqual(
            Array.from(aliases).sort((a, b) => a.localeCompare(b)),
            Array.from(expectedAliases).sort((a, b) => a.localeCompare(b)),
          )
        ) {
          return { aliasesMismatch: { expected: Array.from(expectedAliases), actual: Array.from(aliases) } };
        }
        return null;
      },
    ];
  }

  private collectAliases(models: any[]): Set<string> {
    const aliases = new Set<string>();
    models?.forEach(model => {
      model?.aliases?.forEach(alias => {
        const normalizedAlias = String(alias).trim();
        if (normalizedAlias) {
          aliases.add(normalizedAlias);
        }
      });
    });
    return aliases;
  }

  private getSuccessMessage(name: string): string {
    switch (this.mode) {
      case 'create-group':
        return `Provider ${name} created!`;
      case 'edit-group':
        return 'Provider group successfully updated!';
      case 'create-endpoint':
        return `Endpoint ${name} created!`;
      case 'edit-endpoint':
        return 'Endpoint successfully updated!';
    }
  }
}
