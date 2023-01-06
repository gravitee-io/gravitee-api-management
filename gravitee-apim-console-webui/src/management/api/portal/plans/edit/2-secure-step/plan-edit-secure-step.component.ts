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
import { Component, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import { camelCase } from 'lodash';
import { EMPTY, Subject } from 'rxjs';
import { FormControl, FormGroup } from '@angular/forms';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import '@gravitee/ui-components/wc/gv-schema-form-group';

import { Constants } from '../../../../../../entities/Constants';
import { PlanSecurityType } from '../../../../../../entities/plan/plan';
import { PolicyService } from '../../../../../../services-ngx/policy.service';
import { ResourceService } from '../../../../../../services-ngx/resource.service';
import { ResourceListItem } from '../../../../../../entities/resource/resourceListItem';
import { Api } from '../../../../../../entities/api';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';

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

  public securityConfigSchema: unknown;

  private resourceTypes: ResourceListItem[];

  @Input()
  public api: Api;

  constructor(
    @Inject('Constants') private readonly constants: Constants,
    private readonly policyService: PolicyService,
    private readonly resourceService: ResourceService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.secureForm = new FormGroup({
      securityTypes: new FormControl(),
      securityConfig: new FormControl({}),
      selectionRule: new FormControl(),
    });

    this.secureForm
      .get('securityTypes')
      .valueChanges.pipe(
        takeUntil(this.unsubscribe$),
        tap(() => (this.securityConfigSchema = undefined)),
        filter((securityType) => securityType && securityType !== PlanSecurityType.KEY_LESS),
        map((securityType) => this.securityTypes.find((type) => type.id === securityType).policy),
        switchMap((securityTypePolicy) => this.policyService.getSchema(securityTypePolicy)),
        catchError((error) => {
          this.snackBarService.error(error.error?.message ?? 'An error occurs while loading security schema.');
          return EMPTY;
        }),
      )
      .subscribe((schema) => {
        this.secureForm.get('securityConfig').reset({});
        this.securityConfigSchema = schema;
      });

    this.resourceService
      .list({ expandSchema: false, expandIcon: true })
      .pipe(
        takeUntil(this.unsubscribe$),
        catchError((error) => {
          this.snackBarService.error(error.error?.message ?? 'An error occurs while loading resources.');
          return EMPTY;
        }),
      )
      .subscribe((resources) => {
        this.resourceTypes = resources;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSecurityConfigError($event) {
    // Set error at the end of js task. Otherwise it will be reset on value change
    setTimeout(() => {
      this.secureForm.get('securityConfig').setErrors($event.detail ? { error: true } : null);
    }, 0);
  }

  onFetchResources(event) {
    if (this.resourceTypes && this.api?.resources) {
      const { currentTarget, regexTypes } = event.detail;
      const options = this.api.resources
        .filter((resource) => regexTypes == null || new RegExp(regexTypes).test(resource.type))
        .map((resource) => {
          const resourceType = this.resourceTypes.find((type) => type.id === resource.type);
          const row = document.createElement('gv-row');
          const picture = resourceType.icon ? resourceType.icon : null;
          (row as any).item = { picture, name: resource.name };
          return {
            element: row,
            value: resource.name,
            id: resource.type,
          };
        });

      currentTarget.options = options;
    }
  }
}
