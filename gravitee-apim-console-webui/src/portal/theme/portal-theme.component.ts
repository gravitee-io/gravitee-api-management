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
import { Component, computed, DestroyRef, inject, OnInit, signal, Signal, WritableSignal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { ReactiveFormsModule, FormControl, FormGroup } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { GioAvatarModule, GioFormFilePickerModule, GioMonacoEditorModule, GioSaveBarModule, NewFile } from '@gravitee/ui-particles-angular';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, map, tap } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { MatSelectModule } from '@angular/material/select';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { isEqual } from 'lodash';

import { GioFormColorInputModule } from '../../shared/components/gio-form-color-input/gio-form-color-input.module';
import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { UiPortalThemeService } from '../../services-ngx/ui-theme.service';
import { ThemePortalNext, UpdateThemePortalNext } from '../../entities/management-api-v2';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { NewPortalBadgeComponent } from '../components/portal-badge/new-portal-badge/new-portal-badge.component';

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
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatExpansionModule,
    MatIconModule,
    MatTooltipModule,
    MatButtonModule,
    MatSelectModule,
    GioFormColorInputModule,
    GioAvatarModule,
    GioFormFilePickerModule,
    GioSaveBarModule,
    GioMonacoEditorModule,
    PortalHeaderComponent,
    NewPortalBadgeComponent,
  ],
  standalone: true,
})
export class PortalThemeComponent implements OnInit {
  fontFamilies: string[] = [
    'Arial, Helvetica, "Liberation Sans", FreeSans, sans-serif',
    'Courier, "Courier New", FreeMono, "Liberation Mono", monospace',
    '"DM Mono", monospace',
    '"DM Sans", sans-serif',
    'Georgia, "DejaVu Serif", Norasi, serif',
    'Impact, Arial Black, sans-serif',
    '"Inter", sans-serif',
    '"Lucida Sans", "Lucida Grande", "Lucida Sans Unicode", "Luxi Sans", sans-serif',
    'Monaco, "DejaVu Sans Mono", "Lucida Console", "Andale Mono", monospace',
    '"Montserrat", sans-serif',
    '"Roboto", sans-serif',
    'Tahoma, Geneva, Kalimati, sans-serif',
    'Times, "Times New Roman", "Liberation Serif", FreeSerif, serif',
    '"Trebuchet MS", Arial, Helvetica, sans-serif',
    'Verdana, DejaVu Sans, Bitstream Vera Sans, Geneva, sans-serif',
  ];

  portalThemeForm: FormGroup<{
    logo: FormControl<string[]>;
    favicon: FormControl<string[]>;
    font: FormControl<string>;
    primaryColor: FormControl<string>;
    secondaryColor: FormControl<string>;
    tertiaryColor: FormControl<string>;
    errorColor: FormControl<string>;
    pageBackgroundColor: FormControl<string>;
    cardBackgroundColor: FormControl<string>;
    customCSS: FormControl<string>;
  }> = new FormGroup({
    logo: new FormControl<string[]>([]),
    favicon: new FormControl<string[]>([]),
    font: new FormControl<string>(''),
    primaryColor: new FormControl<string>(''),
    secondaryColor: new FormControl<string>(''),
    tertiaryColor: new FormControl<string>(''),
    errorColor: new FormControl<string>(''),
    pageBackgroundColor: new FormControl<string>(''),
    cardBackgroundColor: new FormControl<string>(''),
    customCSS: new FormControl<string>(''),
  });

  isReadOnly: boolean = true;
  showMonacoEditor = true;

  isFormUnchanged$: Signal<boolean> = computed(() => isEqual(this.formValue$(), this.initialFormValue$()));
  isFormSubmitDisabled$: Signal<boolean> = computed(() => this.isFormNotValid$() || this.isFormUnchanged$());

  private formValue$: Signal<ThemeVM> = toSignal(this.portalThemeForm.valueChanges);
  private isFormNotValid$: Signal<boolean> = toSignal(this.portalThemeForm.statusChanges.pipe(map(status => status !== 'VALID')), {
    initialValue: true,
  });

  private initialFormValue$: WritableSignal<ThemeVM> = signal({});
  private initialTheme: ThemePortalNext;
  private defaultValues: ThemeVM;
  private destroyRef = inject(DestroyRef);

  constructor(
    private readonly uiPortalThemeService: UiPortalThemeService,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit() {
    this.isReadOnly = !this.permissionService.hasAnyMatching(['environment-theme-u']);
    if (this.isReadOnly) {
      this.portalThemeForm.disable();
    }

    this.uiPortalThemeService
      .getDefaultTheme('PORTAL_NEXT')
      .pipe(
        tap((theme: ThemePortalNext) => {
          this.defaultValues = this.convertThemeToThemeVM(theme);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.uiPortalThemeService
      .getCurrentTheme('PORTAL_NEXT')
      .pipe(
        tap((theme: ThemePortalNext) => {
          this.initialTheme = theme;
          this.portalThemeForm.setValue({
            logo: theme.logo ? [theme.logo] : undefined,
            favicon: theme.favicon ? [theme.favicon] : undefined,
            font: theme.definition.font.fontFamily,
            primaryColor: theme.definition.color.primary,
            secondaryColor: theme.definition.color.secondary,
            tertiaryColor: theme.definition.color.tertiary,
            errorColor: theme.definition.color.error,
            pageBackgroundColor: theme.definition.color.pageBackground,
            cardBackgroundColor: theme.definition.color.cardBackground,
            customCSS: theme.definition.customCss ?? '',
          });
          this.initialFormValue$.set(this.portalThemeForm.getRawValue());
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  reset() {
    this.portalThemeForm.reset(this.initialFormValue$());
  }

  restoreDefaultValues() {
    this.portalThemeForm.patchValue(this.defaultValues);
    this.showMonacoEditor = false;
    setTimeout(() => {
      this.showMonacoEditor = true;
    }, 0);
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
          this.initialFormValue$.set(this.convertThemeToThemeVM(theme));
          this.reset();
        }),
        catchError(err => {
          this.snackBarService.error(err.error?.message ?? err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private convertThemeToThemeVM(theme: ThemePortalNext): ThemeVM {
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
      customCSS: theme.definition.customCss ?? '',
    };
  }

  private convertThemeVMToUpdateTheme(themeForm: ThemeVM): UpdateThemePortalNext {
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
