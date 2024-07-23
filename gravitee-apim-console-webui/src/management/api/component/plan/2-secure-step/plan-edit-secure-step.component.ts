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
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { catchError, takeUntil } from 'rxjs/operators';
import '@gravitee/ui-components/wc/gv-schema-form-group';

import { Constants } from '../../../../../entities/Constants';
import { PolicyV2Service } from '../../../../../services-ngx/policy-v2.service';
import { ResourceService } from '../../../../../services-ngx/resource.service';
import { ResourceListItem } from '../../../../../entities/resource/resourceListItem';
import { ApiFederated, ApiV2, ApiV4 } from '../../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { PlanMenuItemVM } from '../../../../../services-ngx/constants.service';
import { ResourceTypeService } from '../../../../../shared/components/form-json-schema-extended/resource-type.service';

@Component({
  selector: 'plan-edit-secure-step',
  templateUrl: './plan-edit-secure-step.component.html',
  styleUrls: ['./plan-edit-secure-step.component.scss'],
})
export class PlanEditSecureStepComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public secureForm: UntypedFormGroup;

  public securityConfigSchema: unknown;

  private resourceTypes: ResourceListItem[];

  @Input()
  public api: ApiV2 | ApiV4 | ApiFederated;

  @Input()
  securityType: PlanMenuItemVM;

  constructor(
    @Inject(Constants) private readonly constants: Constants,
    private readonly policyService: PolicyV2Service,
    private readonly resourceService: ResourceService,
    private readonly snackBarService: SnackBarService,
    private readonly resourceTypeService: ResourceTypeService,
    private readonly changeDetectorRef: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.secureForm = new UntypedFormGroup({
      securityConfig: new UntypedFormControl({}),
      selectionRule: new UntypedFormControl(),
    });

    if (['KEY_LESS', 'PUSH'].includes(this.securityType.planFormType)) {
      return;
    }

    this.policyService
      .getSchema(this.securityType.policy)
      .pipe(
        catchError((error) => {
          this.snackBarService.error(error.error?.message ?? 'An error occurred while loading security schema.');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((schema) => {
        this.securityConfigSchema = schema;
        this.changeDetectorRef.detectChanges();
      });

    // Set resources only if API has resources and when user select OAuth2 security type once
    if (this.api?.resources && this.securityType.planFormType === 'OAUTH2') {
      this.resourceTypeService.setResources(this.api.resources ?? []);
    }
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
