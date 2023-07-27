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
import {Component, Inject, OnInit, OnDestroy} from '@angular/core';

import { ApiV4, ConnectorPlugin, EndpointGroupV4, EndpointV4 } from '../../../../../entities/management-api-v2';
import {Api} from "../../../../../entities/api";
import {UIRouterState, UIRouterStateParams} from "../../../../../ajs-upgraded-providers";
import {StateService} from "@uirouter/core";
import {ApiV2Service} from "../../../../../services-ngx/api-v2.service";
import {ConnectorPluginsV2Service} from "../../../../../services-ngx/connector-plugins-v2.service";
import {SnackBarService} from "../../../../../services-ngx/snack-bar.service";
import {IconService} from "../../../../../services-ngx/icon.service";
import {FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {isUniq, serviceDiscoveryValidator} from "../../../proxy/endpoints/groups/edit/api-proxy-group-edit.validator";
import {takeUntil, tap} from "rxjs/operators";
import {Subject} from "rxjs";


@Component({
  selector: 'api-endpoint-group',
  template: require('./api-endpoint-group.component.html'),
  styles: [require('./api-endpoint-group.component.scss')],
})
export class ApiEndpointGroupComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public api: ApiV4;
  public generalForm: FormGroup;
  public endpointGroupForm: FormGroup;
  public initialGroupFormValue: unknown;


  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiV2Service,
  ) {}


  public ngOnInit(): void {
    const apiId = this.ajsStateParams.apiId;
    const groupIndex = +this.ajsStateParams.groupIndex;

    this.apiService.get(apiId).pipe(
      tap((api : ApiV4)=> {
        this.api = api;
        this.initForms();
        }
      ),
      takeUntil(this.unsubscribe$)
    ).subscribe();

  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  public initForms(): void {
    const group = { ...this.api.endpointGroups[this.ajsStateParams.groupIndex] };

    this.generalForm = new FormGroup({
      name: new FormControl({
        value: group?.name ?? null,
        disabled: false,
      }),
    });

    this.endpointGroupForm = new FormGroup({
      general: this.generalForm,
    });

    this.initialGroupFormValue = this.endpointGroupForm.getRawValue();
  }

  public onSubmit(): void {

  }

  public reset(): void {

  }
}
