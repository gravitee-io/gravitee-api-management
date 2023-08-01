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
import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { StateService } from '@uirouter/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Subject, Subscription } from 'rxjs';

import { ApiV4, EndpointGroupV4 } from '../../../../../entities/management-api-v2';
import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { isUniq } from '../../../proxy/endpoints/groups/edit/api-proxy-group-edit.validator';

@Component({
  selector: 'api-endpoint-group',
  template: require('./api-endpoint-group.component.html'),
  styles: [require('./api-endpoint-group.component.scss')],
})
export class ApiEndpointGroupComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();
  private SUCCESSFUL_ENDPOINT_CONFIGURATION_SAVE_MESSAGE = 'Configuration successfully saved!';

  public api: ApiV4;
  public generalForm: FormGroup;
  public endpointGroupForm: FormGroup;
  public initialGroupFormValue: unknown;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit(): void {
    const apiId = this.ajsStateParams.apiId;

    this.apiService
      .get(apiId)
      .pipe(
        tap((api: ApiV4) => {
          this.api = api;
          this.initForms();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  public initForms(): void {
    const group = { ...this.api.endpointGroups[this.ajsStateParams.groupIndex] };

    this.generalForm = new FormGroup({
      name: new FormControl(
        {
          value: group?.name ?? null,
          disabled: false,
        },
        [
          Validators.required,
          Validators.pattern(/^[^:]*$/),
          isUniq(
            this.api.endpointGroups.reduce((acc, group) => [...acc, group.name], []),
            group?.name,
          ),
        ],
      ),
      loadBalancerType: new  FormControl({ value: group?.loadBalancer?.type ?? null, disabled: false }, [Validators.required]),
    });

    this.endpointGroupForm = new FormGroup({
      general: this.generalForm,
    });

    this.initialGroupFormValue = this.endpointGroupForm.getRawValue();
  }

  public onSubmit(): Subscription {
    const groupIndex = this.ajsStateParams.groupIndex;
    const { loadBalancer, services, sharedConfiguration, endpoints, type } = this.api.endpointGroups[groupIndex];

    const updatedEndpointGroup: EndpointGroupV4 = {
      loadBalancer: {type: this.generalForm.get('loadBalancerType').value},
      name: this.generalForm.get('name').value,
      services: services,
      sharedConfiguration: sharedConfiguration,
      type: type,
      endpoints: endpoints,
    };

    return this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          this.updateApiObject(api, updatedEndpointGroup);
          return this.apiService.update(api.id, api);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.snackBarService.success(this.SUCCESSFUL_ENDPOINT_CONFIGURATION_SAVE_MESSAGE)),
        tap(() =>
          // Redirect to endpoints page
          this.ajsState.go(
            'management.apis.ng.endpoint-group', // TODO: convert router strings to objects
            { apiId: this.api.id, groupName: this.generalForm.get('name').value },
            { reload: true },
          ),
        ),
      )
      .subscribe();
  }

  public reset(): void {
    this.ngOnInit();
  }

  private updateApiObject(api: ApiV4, updatedEndpointGroupObject: EndpointGroupV4): void {
    this.hasAnEndpointGroupIndex()
      ? api.endpointGroups.splice(this.ajsStateParams.groupIndex, 1, updatedEndpointGroupObject)
      : api.endpointGroups.push(updatedEndpointGroupObject);
  }

  private hasAnEndpointGroupIndex(): boolean {
    return this.ajsStateParams.groupIndex !== -1;
  }
}
