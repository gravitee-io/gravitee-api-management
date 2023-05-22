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
import { camelCase } from 'lodash';
import { EMPTY, ReplaySubject, Subject } from 'rxjs';
import { FormControl, FormGroup } from '@angular/forms';
import { catchError, distinctUntilChanged, filter, map, switchMap, take, takeUntil, tap } from 'rxjs/operators';
import '@gravitee/ui-components/wc/gv-schema-form-group';

import { PlanResourceTypeService } from './plan-resource-type/plan-resource-type.service';

import { Constants } from '../../../../../entities/Constants';
import { PlanSecurityType } from '../../../../../entities/plan';
import { PolicyService } from '../../../../../services-ngx/policy.service';
import { ResourceService } from '../../../../../services-ngx/resource.service';
import { ResourceListItem } from '../../../../../entities/resource/resourceListItem';
import { Api as ApiV3 } from '../../../../../entities/api';
import { ApiV4 } from '../../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

const allSecurityTypes = [
  {
    id: PlanSecurityType.OAUTH2,
    name: 'OAuth2',
    policy: 'oauth2',
  },
  {
    id: PlanSecurityType.JWT,
    name: 'JWT',
    policy: 'jwt',
  },
  {
    id: PlanSecurityType.API_KEY,
    name: 'API Key',
    policy: 'api-key',
  },
  {
    id: PlanSecurityType.KEY_LESS,
    name: 'Keyless (public)',
  },
];

@Component({
  selector: 'plan-edit-secure-step',
  template: require('./plan-edit-secure-step.component.html'),
  styles: [require('./plan-edit-secure-step.component.scss')],
})
export class PlanEditSecureStepComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public secureForm: FormGroup;

  public securityTypes = allSecurityTypes.filter((securityType) => {
    return Object.entries(this.constants.env?.settings?.plan?.security ?? {}).map(([key, value]) => {
      return camelCase(securityType.id) === camelCase(key) && value?.enabled;
    });
  });

  public securityConfigSchema$ = new ReplaySubject<unknown>();
  private currentSecurityType: string;

  private resourceTypes: ResourceListItem[];

  @Input()
  public api: ApiV3 | ApiV4;

  constructor(
    @Inject('Constants') private readonly constants: Constants,
    private readonly policyService: PolicyService,
    private readonly resourceService: ResourceService,
    private readonly snackBarService: SnackBarService,
    private readonly planOauth2ResourceTypeService: PlanResourceTypeService,
    private readonly changeDetectorRef: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.secureForm = new FormGroup({
      securityType: new FormControl(),
      securityConfig: new FormControl({}),
      selectionRule: new FormControl(),
    });

    this.secureForm
      .get('securityType')
      .valueChanges.pipe(
        takeUntil(this.unsubscribe$),
        distinctUntilChanged(),
        tap(() => {
          // Only reset security config if security type has changed
          if (this.currentSecurityType !== this.secureForm.get('securityType').value) {
            this.secureForm.get('securityConfig').reset({}, { emitEvent: false });
            this.currentSecurityType = this.secureForm.get('securityType').value;
          }

          this.securityConfigSchema$.next(undefined);
          this.changeDetectorRef.detectChanges();
        }),
        filter((securityType) => securityType && securityType !== PlanSecurityType.KEY_LESS),
        map((securityType) => this.securityTypes.find((type) => type.id === securityType).policy),
        switchMap((securityTypePolicy) =>
          this.policyService.getSchema(securityTypePolicy).pipe(
            catchError((error) => {
              this.snackBarService.error(error.error?.message ?? 'An error occurs while loading security schema.');
              return EMPTY;
            }),
          ),
        ),
      )
      .subscribe((schema) => {
        this.securityConfigSchema$.next(schema);
        this.changeDetectorRef.detectChanges();
      });

    if (this.api?.resources) {
      // Load resources only if API has resources and when user select OAuth2 security type once
      this.secureForm
        .get('securityType')
        .valueChanges.pipe(
          takeUntil(this.unsubscribe$),
          filter((securityType) => {
            return securityType && securityType === PlanSecurityType.OAUTH2;
          }),
          take(1),
          switchMap(() =>
            this.resourceService.list({ expandSchema: false, expandIcon: true }).pipe(
              catchError((error) => {
                this.snackBarService.error(error.error?.message ?? 'An error occurs while loading resources.');
                return EMPTY;
              }),
            ),
          ),
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
