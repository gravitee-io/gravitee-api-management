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
import { ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { UntypedFormArray, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { cloneDeep, isEmpty } from 'lodash';
import { combineLatest, EMPTY, Observable, Subject } from 'rxjs';
import { catchError, distinctUntilChanged, shareReplay, takeUntil, tap } from 'rxjs/operators';
import { GioLicenseService } from '@gravitee/ui-particles-angular';
import { ActivatedRoute, Router } from '@angular/router';

import { Environment } from '../../../entities/environment/environment';
import { GroupMapping, IdentityProvider, RoleMapping } from '../../../entities/identity-provider';
import { EnvironmentService } from '../../../services-ngx/environment.service';
import { GroupService } from '../../../services-ngx/group.service';
import { IdentityProviderService } from '../../../services-ngx/identity-provider.service';
import { RoleService } from '../../../services-ngx/role.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { ApimFeature } from '../../../shared/components/gio-license/gio-license-data';

export interface ProviderConfiguration {
  name: string;
  getFormGroups(): Record<string, UntypedFormGroup>;
}
@Component({
  selector: 'org-settings-identity-provider',
  styleUrls: ['./org-settings-identity-provider.component.scss'],
  templateUrl: './org-settings-identity-provider.component.html',
})
export class OrgSettingsIdentityProviderComponent implements OnInit, OnDestroy {
  isLoading = true;

  identityProviderFormGroup: UntypedFormGroup;

  mode: 'new' | 'edit' = 'new';

  // Used for the edit mode
  initialIdentityProviderValue: IdentityProvider | null = null;

  openidConnectSsoLicenseOptions = { feature: ApimFeature.APIM_OPENID_CONNECT_SSO };
  hasOpenidConnectSsoLock$: Observable<boolean>;

  @ViewChild('providerConfiguration', { static: false })
  set providerConfiguration(providerPart: ProviderConfiguration | undefined) {
    // only if providerPart changed
    if (providerPart && this._providerPartName !== providerPart.name) {
      this._providerPartName = providerPart.name;
      this.addProviderFormGroups(providerPart.getFormGroups());
    }
  }
  private _providerPartName: string;

  identityProviderType: IdentityProvider['type'] | null = null;

  groups$ = this.groupService.listByOrganization().pipe(shareReplay(1));

  organizationRoles$ = this.roleService.list('ORGANIZATION').pipe(shareReplay(1));

  environments$ = this.environmentService.list().pipe(shareReplay(1));
  allEnvironments: Environment[];

  environmentRoles$ = this.roleService.list('ENVIRONMENT').pipe(shareReplay(1));

  environmentTableDisplayedColumns = ['name', 'description', 'actions'];

  private unsubscribe$ = new Subject<boolean>();

  private identityProviderFormControlKeys: string[] = [];

  constructor(
    private readonly identityProviderService: IdentityProviderService,
    private readonly groupService: GroupService,
    private readonly roleService: RoleService,
    private readonly environmentService: EnvironmentService,
    private readonly snackBarService: SnackBarService,
    private readonly changeDetectorRef: ChangeDetectorRef,
    private readonly licenseService: GioLicenseService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.hasOpenidConnectSsoLock$ = this.licenseService.isMissingFeature$(this.openidConnectSsoLicenseOptions.feature);
    this.identityProviderFormGroup = new UntypedFormGroup({
      type: new UntypedFormControl(),
      enabled: new UntypedFormControl(true),
      name: new UntypedFormControl(null, [Validators.required, Validators.maxLength(50), Validators.minLength(2)]),
      description: new UntypedFormControl(),
      emailRequired: new UntypedFormControl(true),
      syncMappings: new UntypedFormControl(false),
    });

    this.identityProviderFormGroup
      .get('type')
      .valueChanges.pipe(distinctUntilChanged(), takeUntil(this.unsubscribe$))
      .subscribe((type) => {
        this.identityProviderType = type;
        this.identityProviderFormGroup.markAsUntouched();
      });

    if (this.activatedRoute.snapshot.params.id) {
      this.mode = 'edit';

      combineLatest([this.identityProviderService.get(this.activatedRoute.snapshot.params.id), this.environments$])
        .pipe(
          tap(([identityProvider, environments]) => {
            this.identityProviderType = identityProvider.type;
            this.initialIdentityProviderValue = cloneDeep(identityProvider);

            this.identityProviderFormGroup.addControl('groupMappings', new UntypedFormArray([]), { emitEvent: false });
            identityProvider.groupMappings.forEach((groupMapping) => this.addGroupMappingToIdentityProviderFormGroup(groupMapping, false));

            this.allEnvironments = environments;
            this.identityProviderFormGroup.addControl('roleMappings', new UntypedFormArray([]), { emitEvent: false });
            identityProvider.roleMappings.forEach((roleMapping) => this.addRoleMappingToIdentityProviderFormGroup(roleMapping, false));

            this.isLoading = false;
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
    } else {
      this.mode = 'new';
      this.identityProviderFormGroup.get('type').setValue('GRAVITEEIO_AM');
      this.isLoading = false;
    }
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  addProviderFormGroups(formGroups: Record<string, UntypedFormGroup>) {
    if (this.isLoading) {
      return;
    }

    // clean previous form group
    if (!isEmpty(this.identityProviderFormControlKeys)) {
      this.identityProviderFormControlKeys.forEach((key) => {
        this.identityProviderFormGroup.removeControl(key);
      });

      this.identityProviderFormControlKeys = [];
    }

    // add provider form group
    if (this.identityProviderFormGroup && !isEmpty(formGroups)) {
      Object.entries(formGroups).forEach(([key, formGroup]) => {
        this.identityProviderFormControlKeys.push(key);
        this.identityProviderFormGroup.addControl(key, formGroup, { emitEvent: false });
      });
    }

    // For the edit mode
    // Initializes the form value when the sub-form linked to the idP type is added
    if (this.mode === 'edit') {
      this.identityProviderFormGroup.patchValue(this.initialIdentityProviderValue, { emitEvent: false });
      this.identityProviderFormGroup.markAsPristine();
      this.identityProviderFormGroup.markAsUntouched();
      this.changeDetectorRef.detectChanges();
    }
  }

  onSubmit() {
    if (this.identityProviderFormGroup.invalid) {
      return;
    }

    const formSettingsValue = this.identityProviderFormGroup.getRawValue();

    const upsertIdentityProvider$ =
      this.mode === 'new'
        ? this.identityProviderService.create(formSettingsValue)
        : this.identityProviderService.update({ ...this.initialIdentityProviderValue, ...formSettingsValue });

    upsertIdentityProvider$
      .pipe(
        tap(() => {
          this.snackBarService.success('Identity provider successfully saved!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((identityProvider) => {
        if (this.mode === 'new') {
          this.router.navigate(['../', identityProvider.id], { relativeTo: this.activatedRoute });
        } else {
          this.resetComponent();
        }
      });
  }

  addGroupMappingToIdentityProviderFormGroup(groupMapping?: GroupMapping, emitEvent = true) {
    const groupMappings = this.identityProviderFormGroup.get('groupMappings') as UntypedFormArray;
    groupMappings.push(
      new UntypedFormGroup({
        condition: new UntypedFormControl(groupMapping?.condition ?? null, [Validators.required]),
        groups: new UntypedFormControl(groupMapping?.groups ?? [], [Validators.required]),
      }),
      { emitEvent },
    );
    if (emitEvent) {
      this.identityProviderFormGroup.markAsDirty();
    }
  }

  removeGroupMappingFromIdentityProviderFormGroup(index: number) {
    const groupMappings = this.identityProviderFormGroup.get('groupMappings') as UntypedFormArray;
    groupMappings.removeAt(index);
    this.identityProviderFormGroup.markAsDirty();
  }

  addRoleMappingToIdentityProviderFormGroup(roleMapping?: RoleMapping, emitEvent = true) {
    const roleMappings = this.identityProviderFormGroup.get('roleMappings') as UntypedFormArray;
    roleMappings.push(
      new UntypedFormGroup({
        condition: new UntypedFormControl(roleMapping?.condition ?? null, [Validators.required]),
        organizations: new UntypedFormControl(roleMapping?.organizations ?? [], [Validators.required]),
        // new form group with environment.id as key and Environment[] as FormControl
        environments: new UntypedFormGroup({
          ...this.allEnvironments.reduce(
            (prev, environment) => ({
              ...prev,
              [environment.id]: new UntypedFormControl(roleMapping?.environments[environment.id] ?? [], [Validators.required]),
            }),
            {},
          ),
        }),
      }),
      { emitEvent },
    );
    if (emitEvent) {
      this.identityProviderFormGroup.markAsDirty();
    }
  }

  removeRoleMappingFromIdentityProviderFormGroup(index: number) {
    const groupMappings = this.identityProviderFormGroup.get('roleMappings') as UntypedFormArray;
    groupMappings.removeAt(index);
    this.identityProviderFormGroup.markAsDirty();
  }

  onFormReset() {
    const groupMappings = this.identityProviderFormGroup.get('groupMappings') as UntypedFormArray;
    groupMappings.clear();
    this.initialIdentityProviderValue.groupMappings.forEach((groupMapping) =>
      this.addGroupMappingToIdentityProviderFormGroup(groupMapping, false),
    );
  }

  // reset component to initial state
  private resetComponent(): void {
    this.isLoading = true;
    this.initialIdentityProviderValue = null;

    // reset sub form property to force new call of addProviderFormGroups in order to patchValue the form with new idP get
    this._providerPartName = null;
    this.identityProviderType = null;

    this.ngOnInit();
  }
}
