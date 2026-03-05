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

import { GioFormColorInputModule } from '../../components/gio-form-color-input/gio-form-color-input.module';
import { PortalHeaderComponent } from '../../components/header/portal-header.component';
import { UiPortalThemeService } from '../../services/ui-theme.service';
import { ThemePortalNext, UpdateThemePortalNext } from '../../entities';
import { SnackBarService } from '../../services/snack-bar.service';
import { GioPermissionService } from '../../components/gio-permission/gio-permission.service';
import { NewPortalBadgeComponent } from '../../components/portal-badge/new-portal-badge/new-portal-badge.component';

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

  portalThemeForm = new FormGroup({
    logo: new FormControl<string[]>([], { nonNullable: true }),
    favicon: new FormControl<string[]>([], { nonNullable: true }),
    font: new FormControl<string>('', { nonNullable: true }),
    primaryColor: new FormControl<string>('', { nonNullable: true }),
    secondaryColor: new FormControl<string>('', { nonNullable: true }),
    tertiaryColor: new FormControl<string>('', { nonNullable: true }),
    errorColor: new FormControl<string>('', { nonNullable: true }),
    pageBackgroundColor: new FormControl<string>('', { nonNullable: true }),
    cardBackgroundColor: new FormControl<string>('', { nonNullable: true }),
    customCSS: new FormControl<string>('', { nonNullable: true }),
  });

  isReadOnly = true;
  showMonacoEditor = true;

  isFormUnchanged$: Signal<boolean> = computed(() => isEqual(this.formValue$(), this.initialFormValue$()));
  isFormSubmitDisabled$: Signal<boolean> = computed(() => this.isFormNotValid$() || this.isFormUnchanged$());

  private formValue$: Signal<ThemeVM> = toSignal(this.portalThemeForm.valueChanges, { initialValue: {} as ThemeVM });
  private isFormNotValid$: Signal<boolean> = toSignal(this.portalThemeForm.statusChanges.pipe(map(status => status !== 'VALID')), {
    initialValue: true,
  });

  private initialFormValue$: WritableSignal<ThemeVM> = signal({});
  private initialTheme!: ThemePortalNext;
  private defaultValues!: ThemeVM;
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
        map(theme => theme as ThemePortalNext),
        tap(theme => {
          this.defaultValues = this.convertThemeToThemeVM(theme);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.uiPortalThemeService
      .getCurrentTheme('PORTAL_NEXT')
      .pipe(
        map(theme => theme as ThemePortalNext),
        tap(theme => {
          this.initialTheme = theme;
          this.portalThemeForm.setValue({
            logo: theme.logo ? [theme.logo] : [],
            favicon: theme.favicon ? [theme.favicon] : [],
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
        map(theme => theme as ThemePortalNext),
        tap(() => {
          this.snackBarService.success('Theme successfully saved!');
        }),
        tap(theme => {
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
      logo: themeForm.logo ? getBase64(themeForm.logo[0]) ?? '' : '',
      optionalLogo: this.initialTheme.optionalLogo,
      favicon: themeForm.favicon ? getBase64(themeForm.favicon[0]) ?? '' : '',
      definition: {
        color: {
          primary: themeForm.primaryColor ?? '',
          secondary: themeForm.secondaryColor ?? '',
          tertiary: themeForm.tertiaryColor ?? '',
          error: themeForm.errorColor ?? '',
          pageBackground: themeForm.pageBackgroundColor ?? '',
          cardBackground: themeForm.cardBackgroundColor ?? '',
        },
        font: {
          fontFamily: themeForm.font ?? '',
        },
        customCss: themeForm.customCSS,
      },
    };
  }
}

function getBase64(file?: NewFile | string): string | undefined | null {
  if (!file) return null;
  if (!(file instanceof NewFile)) return file;
  return file.dataUrl;
}
