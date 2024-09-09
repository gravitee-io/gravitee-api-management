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
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { ReactiveFormsModule, Validators, FormControl, FormGroup, FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { GioFormSelectionInlineModule, GioFormSlideToggleModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatRadioButton, MatRadioGroup } from '@angular/material/radio';
import { EMPTY } from 'rxjs';
import { MatOption } from '@angular/material/autocomplete';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { catchError, startWith, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { GioRoleModule } from '../../../shared/components/gio-role/gio-role.module';
import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { BannerButton, PortalSettings } from '../../../entities/portal/portalSettings';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { BannerRadioButtonComponent } from '../../components/banner-radio-button/banner-radio-button.component';
import { PortalHeaderComponent } from '../../components/header/portal-header.component';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

interface ButtonForm {
  enabled: FormControl<boolean>;
  label: FormControl<string>;
  visibility: FormControl<string>;
  target: FormControl<string>;
}

interface BannerForm {
  enabled: FormControl<boolean>;
  titleText: FormControl<string>;
  subTitleText: FormControl<string>;
  primaryButton: FormGroup<ButtonForm>;
  secondaryButton: FormGroup<ButtonForm>;
}

@Component({
  selector: 'portal-banner',
  templateUrl: './portal-banner.component.html',
  styleUrls: ['./portal-banner.component.scss'],
  imports: [
    CommonModule,
    GioSaveBarModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    MatRadioButton,
    MatRadioGroup,
    GioRoleModule,
    FormsModule,
    MatOption,
    GioFormSlideToggleModule,
    MatSlideToggle,
    NgOptimizedImage,
    BannerRadioButtonComponent,
    PortalHeaderComponent,
    GioFormSelectionInlineModule,
  ],
  standalone: true,
})
export class PortalBannerComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  settings: PortalSettings;
  form: FormGroup<BannerForm>;
  formInitialValues;

  constructor(
    private readonly portalSettingsService: PortalSettingsService,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit(): void {
    this.portalSettingsService
      .get()
      .pipe(
        tap((portalSettings) => {
          this.settings = portalSettings;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.initialize();
      });
  }

  private initialize() {
    this.form = new FormGroup<BannerForm>({
      enabled: new FormControl<boolean>(this.settings.portalNext.banner?.enabled ?? false, [Validators.required]),
      titleText: new FormControl<string>(this.settings.portalNext.banner?.title, [Validators.required]),
      subTitleText: new FormControl<string>(this.settings.portalNext.banner?.subtitle, [Validators.required]),
      primaryButton: this.initializeButtonForm(this.settings.portalNext.banner?.primaryButton),
      secondaryButton: this.initializeButtonForm(this.settings.portalNext.banner?.secondaryButton),
    });

    this.formInitialValues = this.form.getRawValue();

    this.handleButtonFormEnabledChanges(this.form.controls.primaryButton, this.form.getRawValue().primaryButton.enabled);
    this.handleButtonFormEnabledChanges(this.form.controls.secondaryButton, this.form.getRawValue().secondaryButton.enabled);

    const isReadOnly = !this.permissionService.hasAnyMatching(['environment-settings-u']);
    if (isReadOnly) {
      this.form.disable();
    }
  }

  reset() {
    this.form.reset();
    this.initialize();
  }

  submit() {
    const updatedSettingsPayload: PortalSettings = {
      ...this.settings,
      portalNext: {
        ...this.settings.portalNext,
        banner: {
          enabled: this.form.controls.enabled.value,
          title: this.form.controls.titleText.value,
          subtitle: this.form.controls.subTitleText.value,
          primaryButton: this.prepareButtonForUpdate(this.form.controls.primaryButton),
          secondaryButton: this.prepareButtonForUpdate(this.form.controls.secondaryButton),
        },
      },
    };

    this.portalSettingsService
      .save(updatedSettingsPayload)
      .pipe(
        tap(() => this.snackBarService.success('Settings successfully updated!')),
        catchError(() => {
          this.snackBarService.error('An error occurred during the Settings update');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.form.reset();
        this.ngOnInit();
      });
  }

  private initializeButtonForm(button: BannerButton | undefined): FormGroup<ButtonForm> {
    return new FormGroup<ButtonForm>({
      enabled: new FormControl<boolean>(button?.enabled ?? false, [Validators.required]),
      label: new FormControl<string>(button?.label ?? ''),
      visibility: new FormControl<string>(button?.visibility ?? 'PUBLIC'),
      target: new FormControl<string>(button?.target ?? ''),
    });
  }

  private handleButtonFormEnabledChanges(buttonForm: FormGroup<ButtonForm>, startWithEnabledValue: boolean): void {
    buttonForm.controls.enabled.valueChanges
      .pipe(
        startWith(startWithEnabledValue),
        tap((_) => {
          if (buttonForm.controls.enabled.value) {
            buttonForm.controls.target.addValidators(Validators.required);
            buttonForm.controls.label.addValidators(Validators.required);
            buttonForm.controls.visibility.addValidators(Validators.required);
          } else {
            buttonForm.controls.target.clearValidators();
            buttonForm.controls.label.clearValidators();
            buttonForm.controls.visibility.clearValidators();
          }
          buttonForm.controls.target.updateValueAndValidity();
          buttonForm.controls.label.updateValueAndValidity();
          buttonForm.controls.visibility.updateValueAndValidity();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private prepareButtonForUpdate(buttonForm: FormGroup<ButtonForm>): BannerButton {
    const rawValue = buttonForm.getRawValue();
    return {
      enabled: rawValue.enabled,
      target: rawValue.target,
      visibility: rawValue.visibility === 'PRIVATE' ? 'PRIVATE' : 'PUBLIC',
      label: rawValue.label,
      type: 'EXTERNAL',
    };
  }
}
