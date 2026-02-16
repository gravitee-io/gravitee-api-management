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
import { combineLatest, EMPTY } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GioFormJsonSchemaComponent, GioJsonSchema } from '@gravitee/ui-particles-angular';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4, EndpointGroupV4, EndpointV4Default, UpdateApiV4 } from '../../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../../services-ngx/connector-plugins-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { isEndpointNameUniqueAndDoesNotMatchDefaultValue } from '../api-endpoint-v4-unique-name';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';

interface ProviderForm {
  name: FormControl<string>;
  configuration: FormControl<any>;
  sharedConfigurationOverride: FormControl<any>;
}

@Component({
  selector: 'api-llm-provider',
  templateUrl: './api-llm-provider.component.html',
  styleUrls: ['./api-llm-provider.component.scss'],
  standalone: false,
})
export class ApiLlmProviderComponent implements OnInit {
  private providerIndex: number | null = null;
  public provider: EndpointGroupV4 | null = null;
  public isEditMode = false;
  public isReadOnly = false;
  public formGroup: FormGroup<ProviderForm> = new FormGroup<ProviderForm>({
    name: new FormControl<string>('', { validators: [Validators.required, Validators.pattern(/^[^:]*$/)], nonNullable: true }),
    configuration: new FormControl<any>({}, { validators: [Validators.required], nonNullable: true }),
    sharedConfigurationOverride: new FormControl<any>({}, { validators: [Validators.required], nonNullable: true }),
  });
  public providerSchema: { config: GioJsonSchema | null; sharedConfig: GioJsonSchema | null };
  public isLoading = false;
  private api: ApiV4;

  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly apiService = inject(ApiV2Service);
  private readonly connectorPluginsV2Service = inject(ConnectorPluginsV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly destroyRef: DestroyRef = inject(DestroyRef);

  public ngOnInit(): void {
    this.isLoading = true;
    const apiId = this.activatedRoute.snapshot.params.apiId;
    const providerIndexParam = this.activatedRoute.snapshot.params.providerIndex;

    this.isEditMode = providerIndexParam !== undefined && providerIndexParam !== null;
    if (this.isEditMode) {
      this.providerIndex = +providerIndexParam;
    }

    this.apiService
      .get(apiId)
      .pipe(
        switchMap(api => {
          this.api = api as ApiV4;
          const apiV4 = api as ApiV4;

          const isKubernetesOrigin = apiV4.definitionContext?.origin === 'KUBERNETES';
          const canUpdate = this.permissionService.hasAnyMatching(['api-definition-u']);
          this.isReadOnly = isKubernetesOrigin || !canUpdate;

          if (this.isEditMode && this.providerIndex !== null) {
            const endpointGroups = apiV4.endpointGroups || [];
            this.provider = endpointGroups[this.providerIndex];

            if (!this.provider) {
              this.snackBarService.error(`Provider at index [ ${this.providerIndex} ] does not exist.`);
              this.router.navigate(['../../'], { relativeTo: this.activatedRoute });
              return EMPTY;
            }
          }

          return combineLatest([
            this.connectorPluginsV2Service.getEndpointPluginSchema('llm-proxy'),
            this.connectorPluginsV2Service.getEndpointPluginSharedConfigurationSchema('llm-proxy'),
          ]);
        }),
        tap(([config, sharedConfig]) => {
          this.providerSchema = {
            config: config && GioFormJsonSchemaComponent.isDisplayable(config) ? config : null,
            sharedConfig: sharedConfig && GioFormJsonSchemaComponent.isDisplayable(sharedConfig) ? sharedConfig : null,
          };
          this.setupForm();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => (this.isLoading = false));
  }

  public onSave() {
    const formValue = this.formGroup.getRawValue();
    const cleanName = formValue.name.trim();
    const configuration = formValue.configuration || {};
    const sharedConfiguration = formValue.sharedConfigurationOverride || {};

    this.apiService
      .get(this.api.id)
      .pipe(
        switchMap(api => {
          const apiV4 = api as ApiV4;
          const updatedApi =
            this.isEditMode && this.provider && this.providerIndex !== null
              ? this.updateExistingProvider(apiV4, cleanName, configuration, sharedConfiguration)
              : this.createNewProvider(apiV4, cleanName, configuration, sharedConfiguration);

          return this.apiService.update(apiV4.id, updatedApi);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'An error occurred.');
          return EMPTY;
        }),
        map(() => {
          const successMessage = this.isEditMode ? 'Provider successfully updated!' : `Provider ${formValue.name.trim()} created!`;
          this.snackBarService.success(successMessage);
          this.router.navigate(['../../'], { relativeTo: this.activatedRoute });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private updateExistingProvider(api: ApiV4, cleanName: string, configuration: any, sharedConfiguration: any): UpdateApiV4 {
    if (!this.provider || this.providerIndex === null) {
      throw new Error('Cannot update provider: provider or index is null');
    }

    const endpoints = this.provider.endpoints || [];
    const updatedProvider: EndpointGroupV4 = {
      ...this.provider,
      name: cleanName,
      ...(sharedConfiguration ? { sharedConfiguration } : {}),
      endpoints: endpoints.map(endpoint => ({
        ...endpoint,
        name: `${cleanName} default endpoint`,
        configuration,
        sharedConfigurationOverride: sharedConfiguration,
      })),
    };

    const endpointGroups = api.endpointGroups || [];
    return {
      ...api,
      endpointGroups: endpointGroups.map((group, i) => (i === this.providerIndex ? updatedProvider : group)),
    };
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

  private setupForm(): void {
    const nameControl = this.formGroup.get('name');
    if (nameControl) {
      const defaultValue = this.provider?.name || '';
      const uniqueValidator = isEndpointNameUniqueAndDoesNotMatchDefaultValue(this.api, defaultValue);

      if (uniqueValidator) {
        const existingValidator = nameControl.validator;
        nameControl.setValidators(
          existingValidator ? [existingValidator, uniqueValidator] : [Validators.required, Validators.pattern(/^[^:]*$/), uniqueValidator],
        );
        nameControl.updateValueAndValidity();
      }
    }

    if (this.isEditMode && this.provider) {
      const endpoint = this.provider.endpoints?.[0];
      this.formGroup.patchValue({
        name: this.provider.name || '',
        configuration: endpoint?.configuration || {},
        // LLM don’t have the ability to override shared configuration
        sharedConfigurationOverride: this.provider?.sharedConfiguration || {},
      });
    }

    if (this.isReadOnly) {
      this.formGroup.disable();
    }
  }
}
