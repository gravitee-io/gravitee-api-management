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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { EMPTY, Subject } from 'rxjs';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthenticationStrategyService } from '../../../../services-ngx/authentication-strategy.service';
import { ClientRegistrationProvidersService } from '../../../../services-ngx/client-registration-providers.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { AuthenticationStrategy, AuthenticationStrategyType } from '../../../../entities/authentication-strategy/authenticationStrategy';
import { ClientRegistrationProvider } from '../../../../entities/client-registration-provider/clientRegistrationProvider';

@Component({
  selector: 'authentication-strategy',
  templateUrl: './authentication-strategy.component.html',
  styleUrls: ['./authentication-strategy.component.scss'],
  standalone: false,
})
export class AuthenticationStrategyComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<void>();
  updateMode = false;
  strategyForm: UntypedFormGroup;
  formInitialValues: unknown;
  providers: ClientRegistrationProvider[] = [];

  strategyTypes: { label: string; value: AuthenticationStrategyType }[] = [
    { label: 'Dynamic Client Registration (DCR)', value: 'DCR' },
    { label: 'Key Authentication', value: 'KEY_AUTH' },
    { label: 'Self-managed OIDC', value: 'SELF_MANAGED_OIDC' },
  ];

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly authenticationStrategyService: AuthenticationStrategyService,
    private readonly clientRegistrationProvidersService: ClientRegistrationProvidersService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  ngOnInit(): void {
    this.clientRegistrationProvidersService
      .list()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(providers => {
        this.providers = providers;
      });

    this.strategyForm = new UntypedFormGroup({
      name: new UntypedFormControl('', [Validators.required]),
      display_name: new UntypedFormControl(''),
      description: new UntypedFormControl(''),
      type: new UntypedFormControl('DCR', [Validators.required]),
      client_registration_provider_id: new UntypedFormControl(''),
      scopes: new UntypedFormControl([]),
      auth_methods: new UntypedFormControl([]),
      credential_claims: new UntypedFormControl(''),
      auto_approve: new UntypedFormControl(false),
      hide_credentials: new UntypedFormControl(false),
    });

    const strategyId = this.activatedRoute.snapshot.params['strategyId'];
    if (strategyId) {
      this.updateMode = true;
      this.authenticationStrategyService
        .get(strategyId)
        .pipe(takeUntil(this.unsubscribe$))
        .subscribe(strategy => {
          this.strategyForm.patchValue(strategy);
          this.formInitialValues = this.strategyForm.getRawValue();
        });
    } else {
      this.formInitialValues = this.strategyForm.getRawValue();
    }
  }

  onSubmit(): void {
    const formValue = this.strategyForm.getRawValue();

    if (this.updateMode) {
      const strategyId = this.activatedRoute.snapshot.params['strategyId'];
      const strategy: AuthenticationStrategy = { ...formValue, id: strategyId };
      this.authenticationStrategyService
        .update(strategy)
        .pipe(
          tap(() => {
            this.snackBarService.success('Authentication strategy updated.');
            this.router.navigate(['..'], { relativeTo: this.activatedRoute });
          }),
          catchError(({ error }) => {
            this.snackBarService.error(error.message);
            return EMPTY;
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
    } else {
      this.authenticationStrategyService
        .create(formValue)
        .pipe(
          tap(() => {
            this.snackBarService.success('Authentication strategy created.');
            this.router.navigate(['..'], { relativeTo: this.activatedRoute });
          }),
          catchError(({ error }) => {
            this.snackBarService.error(error.message);
            return EMPTY;
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
    }
  }

  get requiresProvider(): boolean {
    const type = this.strategyForm?.get('type')?.value;
    return type === 'DCR' || type === 'SELF_MANAGED_OIDC';
  }
}
