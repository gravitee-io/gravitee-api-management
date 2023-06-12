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
import { ChangeDetectorRef, Component, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import { EMPTY, Subject } from 'rxjs';
import { FormControl, FormGroup } from '@angular/forms';
import { catchError, takeUntil } from 'rxjs/operators';
import '@gravitee/ui-components/wc/gv-schema-form-group';

import { PlanResourceTypeService } from './plan-resource-type/plan-resource-type.service';

import { Constants } from '../../../../../entities/Constants';
import { PolicyV2Service } from '../../../../../services-ngx/policy-v2.service';
import { ResourceService } from '../../../../../services-ngx/resource.service';
import { ResourceListItem } from '../../../../../entities/resource/resourceListItem';
import { ApiV2, ApiV4 } from '../../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { PlanSecurityVM } from '../../../../../services-ngx/constants.service';

@Component({
  selector: 'plan-edit-secure-step',
  template: require('./plan-edit-secure-step.component.html'),
  styles: [require('./plan-edit-secure-step.component.scss')],
})
export class PlanEditSecureStepComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public secureForm: FormGroup;

  public securityConfigSchema: unknown;

  private resourceTypes: ResourceListItem[];

  @Input()
  public api: ApiV2 | ApiV4;

  @Input()
  securityType: PlanSecurityVM;
  constructor(
    @Inject('Constants') private readonly constants: Constants,
    private readonly policyService: PolicyV2Service,
    private readonly resourceService: ResourceService,
    private readonly snackBarService: SnackBarService,
    private readonly planOauth2ResourceTypeService: PlanResourceTypeService,
    private readonly changeDetectorRef: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.secureForm = new FormGroup({
      securityConfig: new FormControl({}),
      selectionRule: new FormControl(),
    });

    if (this.securityType.id === 'KEY_LESS') {
      return;
    }

    this.policyService
      .getSchema(this.securityType.policy)
      .pipe(
        takeUntil(this.unsubscribe$),
        catchError((error) => {
          this.snackBarService.error(error.error?.message ?? 'An error occurred while loading security schema.');
          return EMPTY;
        }),
      )
      .subscribe((schema) => {
        this.securityConfigSchema = schema;
        this.changeDetectorRef.detectChanges();
      });

    // Load resources only if API has resources and when user select OAuth2 security type once
    if (this.api?.resources && this.securityType.id === 'OAUTH2') {
      this.resourceService
        .list({ expandSchema: false, expandIcon: true })
        .pipe(
          takeUntil(this.unsubscribe$),
          catchError((error) => {
            this.snackBarService.error(error.error?.message ?? 'An error occurred while loading resources.');
            return EMPTY;
          }),
        )
        .subscribe((resourceTypes) => {
          this.resourceTypes = resourceTypes;
          this.planOauth2ResourceTypeService.setResources(this.api.resources ?? [], resourceTypes);
        });
    }
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
