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
import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { ReactiveFormsModule, FormControl, FormGroup } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { GioAvatarModule, GioFormFilePickerModule, GioMonacoEditorModule, GioSaveBarModule, NewFile } from '@gravitee/ui-particles-angular';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatAnchor, MatButton } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, map, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Subject } from 'rxjs';
import { isEqual } from 'lodash';
import { MatOption, MatSelect } from '@angular/material/select';

import { GioFormColorInputModule } from '../../../shared/components/gio-form-color-input/gio-form-color-input.module';
import { PortalHeaderComponent } from '../../components/header/portal-header.component';
import { UiPortalThemeService } from '../../../services-ngx/ui-theme.service';
import { ThemePortalNext, UpdateThemePortalNext } from '../../../entities/management-api-v2';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

export interface ThemeVM {
  logo?: string[];
  favicon?: string[];
  font?: string;
  primaryColor?: string;
  secondaryColor?: string;
  tertiaryColor?: string;
  errorColor?: string;
  pageBackgroundColor?: string;
  cardBackgroundColor?: string;
  customCSS?: string;
}

@Component({
  selector: 'portal-theme',
  templateUrl: './portal-theme.component.html',
  styleUrls: ['./portal-theme.component.scss'],
  imports: [
    CommonModule,
    GioFormFilePickerModule,
    GioSaveBarModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    ReactiveFormsModule,
    MatExpansionModule,
    GioFormColorInputModule,
    GioAvatarModule,
    GioFormFilePickerModule,
    MatAnchor,
    MatButton,
    MatTooltipModule,
    GioMonacoEditorModule,
    PortalHeaderComponent,
    MatSelect,
    MatOption,
  ],
  standalone: true,
})
export class PortalThemeComponent implements OnInit, OnDestroy {
  constructor(
    private readonly uiPortalThemeService: UiPortalThemeService,
    private readonly snackBarService: SnackBarService,
  ) {}

  private initialTheme: ThemePortalNext;
  private defaultValues: ThemeVM;
  private initialValues: ThemeVM;

  public fontFamilies: string[] = [
    'Arial, Helvetica, "Liberation Sans", FreeSans, sans-serif',
    '"Trebuchet MS", Arial, Helvetica, sans-serif',
    '"Lucida Sans", "Lucida Grande", "Lucida Sans Unicode", "Luxi Sans", sans-serif',
    'Tahoma, Geneva, Kalimati, sans-serif',
    'Verdana, DejaVu Sans, Bitstream Vera Sans, Geneva, sans-serif',
    'Impact, Arial Black, sans-serif',
    'Courier, "Courier New", FreeMono, "Liberation Mono", monospace',
    'Monaco, "DejaVu Sans Mono", "Lucida Console", "Andale Mono", monospace',
    'Times, "Times New Roman", "Liberation Serif", FreeSerif, serif',
    'Georgia, "DejaVu Serif", Norasi, serif',
    '"Inter", sans-serif',
    '"Roboto", sans-serif',
  ];

  public portalThemeForm;

  private unsubscribe$: Subject<void> = new Subject<void>();

  formUnchanged: boolean = true;

  ngOnInit() {
    this.uiPortalThemeService
      .getDefaultTheme('PORTAL_NEXT')
      .pipe(
        tap((theme: ThemePortalNext) => {
          this.defaultValues = this.convertThemeToThemeVM(theme);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();

    this.uiPortalThemeService
      .getCurrentTheme('PORTAL_NEXT')
      .pipe(
        tap((theme: ThemePortalNext) => {
          this.initialTheme = theme;
        }),
        map((theme: ThemePortalNext) => this.convertThemeToThemeVM(theme)),
        tap((themeVM: ThemeVM) => {
          this.initialValues = themeVM;
          this.portalThemeForm = new FormGroup({
            logo: new FormControl(this.initialValues.logo),
            favicon: new FormControl(this.initialValues.favicon),
            font: new FormControl(this.initialValues.font),
            primaryColor: new FormControl(this.initialValues.primaryColor),
            secondaryColor: new FormControl(this.initialValues.secondaryColor),
            tertiaryColor: new FormControl(this.initialValues.tertiaryColor),
            errorColor: new FormControl(this.initialValues.errorColor),
            pageBackgroundColor: new FormControl(this.initialValues.pageBackgroundColor),
            cardBackgroundColor: new FormControl(this.initialValues.cardBackgroundColor),
            customCSS: new FormControl(this.initialValues.customCSS),
          });

          this.portalThemeForm.valueChanges
            .pipe(
              tap((value) => {
                this.formUnchanged = isEqual(this.initialValues, value);
              }),
              takeUntil(this.unsubscribe$),
            )
            .subscribe();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  reset() {
    this.portalThemeForm.patchValue(this.initialValues);
  }

  restoreDefaultValues() {
    this.portalThemeForm.patchValue(this.defaultValues);
  }

  submit() {
    const updateTheme = this.convertThemeVMToUpdateTheme(this.portalThemeForm.getRawValue());

    this.uiPortalThemeService
      .updateTheme(updateTheme)
      .pipe(
        tap(() => {
          this.snackBarService.success('Theme successfully saved!');
        }),
        tap((theme: ThemePortalNext) => {
          this.initialTheme = theme;
          this.initialValues = this.convertThemeToThemeVM(theme);
          this.reset();
        }),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? err.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  convertThemeToThemeVM(theme: ThemePortalNext): ThemeVM {
    return {
      logo: theme.logo ? [theme.logo] : undefined,
      favicon: theme.favicon ? [theme.favicon] : undefined,
      font: theme.definition.font.fontFamily,
      primaryColor: theme.definition.color.primary,
      secondaryColor: theme.definition.color.secondary,
      tertiaryColor: theme.definition.color.tertiary,
      errorColor: theme.definition.color.error,
      pageBackgroundColor: theme.definition.color.pageBackground,
      cardBackgroundColor: theme.definition.color.cardBackground,
      customCSS: theme.definition.customCss ?? undefined,
    };
  }

  convertThemeVMToUpdateTheme(themeForm: ThemeVM): UpdateThemePortalNext {
    return {
      id: this.initialTheme.id,
      name: this.initialTheme.name,
      type: 'PORTAL_NEXT',
      enabled: true,
      logo: themeForm.logo ? getBase64(themeForm.logo[0]) : undefined,
      optionalLogo: this.initialTheme.optionalLogo,
      favicon: themeForm.favicon ? getBase64(themeForm.favicon[0]) : undefined,
      definition: {
        color: {
          primary: themeForm.primaryColor,
          secondary: themeForm.secondaryColor,
          tertiary: themeForm.tertiaryColor,
          error: themeForm.errorColor,
          pageBackground: themeForm.pageBackgroundColor,
          cardBackground: themeForm.cardBackgroundColor,
        },
        font: {
          fontFamily: themeForm.font,
        },
        customCss: themeForm.customCSS,
      },
    };
  }
}

function getBase64(file?: NewFile | string): string | undefined | null {
  if (!file) {
    // If no file, return null to remove it
    return null;
  }
  if (!(file instanceof NewFile)) {
    // If file not changed, return undefined to keep it
    return file;
  }

  return file.dataUrl;
}
