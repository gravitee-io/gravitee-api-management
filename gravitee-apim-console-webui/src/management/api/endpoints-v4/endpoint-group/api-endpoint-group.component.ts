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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { filter, map, switchMap, takeUntil } from 'rxjs/operators';
import { Observable, Subject } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { Api, ApiV4, EndpointGroupV4, UpdateApiV4 } from '../../../../entities/management-api-v2';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { isUniqueAndDoesNotMatchDefaultValue } from '../../../../shared/utils';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { getMatchingDlqEntrypoints, updateDlqEntrypoint } from '../api-endpoint-v4-matching-dlq';

@Component({
  selector: 'api-endpoint-group',
  template: require('./api-endpoint-group.component.html'),
  styles: [require('./api-endpoint-group.component.scss')],
})
export class ApiEndpointGroupComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();
  private SUCCESSFUL_ENDPOINT_CONFIGURATION_SAVE_MESSAGE = 'Configuration successfully saved!';

  public api: ApiV4;
  public initialApi: ApiV4;
  public isReadOnly: boolean;
  public generalForm: FormGroup;
  public groupForm: FormGroup;
  public configurationForm: FormGroup;

  public initialGroupFormValue: any;
  public endpointGroup: EndpointGroupV4;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private readonly matDialog: MatDialog,
  ) {}

  public ngOnInit(): void {
    const apiId = this.ajsStateParams.apiId;

    this.apiService
      .get(apiId)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: (api: ApiV4) => this.initializeComponent(api),
      });
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  public onSubmit(): void {
    this.updateApi()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: (api) => {
          this.initializeComponent(api as ApiV4);
          this.snackBarService.success(this.SUCCESSFUL_ENDPOINT_CONFIGURATION_SAVE_MESSAGE);
        },
        error: (error) => {
          this.snackBarService.error(error.message);
        },
      });
  }

  /**
   * Initialize the component
   *
   * @param api the API object
   * @private
   */
  private initializeComponent(api: ApiV4): void {
    this.api = api;
    this.initialApi = this.api;
    this.endpointGroup = this.api.endpointGroups[this.ajsStateParams.groupIndex];

    this.isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-r']) || api.definitionContext?.origin === 'KUBERNETES';

    const group = { ...this.api.endpointGroups[this.ajsStateParams.groupIndex] };

    this.generalForm = new FormGroup({
      name: new FormControl(
        {
          value: group?.name ?? null,
          disabled: this.isReadOnly,
        },
        [
          Validators.required,
          Validators.pattern(/^[^:]*$/),
          isUniqueAndDoesNotMatchDefaultValue(
            this.api.endpointGroups.reduce((acc, group) => [...acc, group.name], []),
            group?.name,
          ),
        ],
      ),
      loadBalancerType: new FormControl({ value: group?.loadBalancer?.type ?? null, disabled: false }, [Validators.required]),
    });

    this.configurationForm = new FormGroup({
      groupConfiguration: new FormControl({
        value: group?.sharedConfiguration ?? {},
        disabled: this.isReadOnly,
      }),
    });

    this.groupForm = new FormGroup({
      general: this.generalForm,
      configuration: this.configurationForm,
    });

    this.initialGroupFormValue = this.groupForm.getRawValue();
  }

  /**
   * Update the current API object with the values from the forms on all tabs
   *
   * @param api the API object
   * @private
   */
  private updateApiObjectWithFormData(api: ApiV4): UpdateApiV4 {
    const updatedEndpointGroups = [...api.endpointGroups];
    updatedEndpointGroups[this.ajsStateParams.groupIndex] = {
      ...this.api.endpointGroups[this.ajsStateParams.groupIndex],
      name: this.generalForm.getRawValue().name,
      loadBalancer: {
        type: this.generalForm.getRawValue().loadBalancerType,
      },
      sharedConfiguration: this.configurationForm.getRawValue().groupConfiguration,
    };

    return {
      ...api,
      endpointGroups: updatedEndpointGroups,
    };
  }

  private updateApi(): Observable<Api> {
    const newGroupName = this.generalForm.getRawValue().name.trim();
    const matchingDlqEntrypoint = getMatchingDlqEntrypoints(this.api, this.endpointGroup.name);
    if (this.endpointGroup.name !== newGroupName && matchingDlqEntrypoint.length > 0) {
      return this.matDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
          width: '500px',
          data: {
            title: 'Rename Endpoint Group',
            content: `Some entrypoints use this group as Dead letter queue. They will be modified to reference the new name.`,
            confirmButton: 'Update',
          },
          role: 'alertdialog',
          id: 'updateEndpointGroupNameDlqConfirmDialog',
        })
        .afterClosed()
        .pipe(
          filter((confirm) => confirm === true),
          switchMap(() => this.apiService.get(this.api.id)),
          map((api: ApiV4) => {
            updateDlqEntrypoint(api, matchingDlqEntrypoint, newGroupName);
            return api;
          }),
          switchMap((api: ApiV4) => this.apiService.update(this.api.id, this.updateApiObjectWithFormData(api))),
        );
    } else {
      return this.apiService
        .get(this.api.id)
        .pipe(switchMap((api: ApiV4) => this.apiService.update(this.api.id, this.updateApiObjectWithFormData(api))));
    }
  }
}
