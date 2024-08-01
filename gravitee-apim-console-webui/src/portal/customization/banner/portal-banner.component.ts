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
import { GioFormSlideToggleModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatRadioButton, MatRadioGroup } from '@angular/material/radio';
import { EMPTY } from 'rxjs';
import { MatOption } from '@angular/material/autocomplete';
import { MatSelect } from '@angular/material/select';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { catchError, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { GioRoleModule } from '../../../shared/components/gio-role/gio-role.module';
import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { PortalSettings } from '../../../entities/portal/portalSettings';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { BannerRadioButtonComponent } from '../../components/banner-radio-button/banner-radio-button.component';
import { PortalHeaderComponent } from '../../components/header/portal-header.component';

interface BannerForm {
  enabled: FormControl<boolean>;
  titleText: FormControl<string>;
  subTitleText: FormControl<string>;
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
    MatSelect,
    GioFormSlideToggleModule,
    MatSlideToggle,
    NgOptimizedImage,
    BannerRadioButtonComponent,
    PortalHeaderComponent,
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
    });
    this.formInitialValues = this.form.getRawValue();
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
      .subscribe(() => this.ngOnInit());
  }
}
