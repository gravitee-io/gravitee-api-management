import { ChangeDetectorRef, Component, DestroyRef, OnInit } from "@angular/core";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatOption, MatSelect } from "@angular/material/select";
import { GioFormJsonSchemaModule } from "@gravitee/ui-particles-angular";
import {
  AbstractControl,
  ControlContainer, FormArray, FormBuilder, FormControl,
  FormGroup,
  FormGroupDirective,
  ReactiveFormsModule,
  UntypedFormControl, Validators
} from "@angular/forms";
import { MatCardModule } from "@angular/material/card";
import { NgForOf, NgIf, NgOptimizedImage } from "@angular/common";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { MatButton } from "@angular/material/button";
import { take } from "rxjs/operators";
import { RouterLink } from "@angular/router";
import { MatInput } from "@angular/material/input";
import { MatMenu, MatMenuItem, MatMenuTrigger } from "@angular/material/menu";

import { NotifierService } from "../../../../services-ngx/notifier.service";
import { GeneralFormValue } from "../components/runtime-alert-create-general";
import { DampeningMode, DampeningModesNames, DurationTimeUnit } from "../../../../entities/alert";


@Component({
  selector: 'runtime-alert-create-notifications',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    NgForOf,
    GioFormJsonSchemaModule,
    MatFormFieldModule,
    MatOption,
    MatSelect,
    MatCardModule,
    MatButton,
    RouterLink,
    MatMenu,
    MatMenuItem,
    NgOptimizedImage,
    MatMenuTrigger,
    MatInput,
    NgIf
  ],
  templateUrl: './runtime-alert-create-notifications.component.html',
  styleUrl: './runtime-alert-create-notifications.component.scss',
  viewProviders: [{ provide: ControlContainer, useExisting: FormGroupDirective }]
})
export class RuntimeAlertCreateNotificationsComponent implements OnInit {
  public parentForm: FormGroup;
  public dampeningForm: FormGroup;
  public notificationsForm: FormArray;
  public isDampeningVisible = true;
  public dampeningModes = DampeningMode.MODES;
  public timeUnits = DurationTimeUnit.TIME_UNITS;
  public channels = [
    { label: "E-mail", value: "email-notifier" },
    { label: "Slack", value: "slack-notifier" },
    { label: "System e-mail", value: "default-email" },
    { label: "Webhook", value: "webhook-notifier" },
  ];

  constructor(
    private readonly destroyRef: DestroyRef,
    private readonly notifierService: NotifierService,
    private readonly formGroupDirective: FormGroupDirective,
    private readonly formBuilder: FormBuilder,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.parentForm = this.formGroupDirective.form;
    this.notificationsForm = this.parentForm?.get('notificationsForm') as FormArray;
    this.defineDampeningForm();
  }

  defineDampeningForm(): void {
    this.dampeningForm = this.parentForm?.get('dampeningForm') as FormGroup;
    this.dampeningForm.addControl('mode', new UntypedFormControl(''));
    this.dampeningModeChangesSub();
    this.dampeningForm.controls.mode.setValue(DampeningModesNames.STRICT_COUNT);
  }

  public dampeningModeChangesSub () {
    this.dampeningForm.controls.mode.valueChanges
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (mode: DampeningModesNames) => {
          this.seedDampeningControls(mode);
        }
      })
  }


  get trueEvaluationsControl() {
    return this.dampeningForm.get('trueEvaluations');

  }
  get totalEvaluationsControl() {
    return this.dampeningForm.get('totalEvaluations');

  }
  get durationControl() {
    return this.dampeningForm.get('duration');

  }
  get timeUnitControl() {
    return this.dampeningForm.get('timeUnit');
  }


  seedDampeningControls(mode: DampeningModesNames) {
    this.dampeningForm.removeControl('trueEvaluations');
    this.dampeningForm.removeControl('totalEvaluations');
    this.dampeningForm.removeControl('duration');
    this.dampeningForm.removeControl('timeUnit');

    this.changeDetectorRef.detectChanges();

    switch (mode) {
      case DampeningModesNames.STRICT_COUNT: // N consecutive true evaluations
        this.dampeningForm.addControl('trueEvaluations', new FormControl(1, [Validators.required, Validators.min(1), Validators.max(100)]));
        break;

      case DampeningModesNames.RELAXED_COUNT: // N true evaluations out of M total evaluations
        this.dampeningForm.addControl('trueEvaluations', new FormControl(1, [Validators.required, Validators.min(1), Validators.max(100)]));
        this.dampeningForm.addControl('totalEvaluations', new UntypedFormControl(null));
        break;

      case DampeningModesNames.STRICT_TIME: // Only true evaluations for at least T time
        this.dampeningForm.addControl('duration', new UntypedFormControl(null));
        this.dampeningForm.addControl('timeUnit', new UntypedFormControl(null));
        break;

      case DampeningModesNames.RELAXED_TIME: // N true evaluations in T time
        this.dampeningForm.addControl('trueEvaluations', new UntypedFormControl(1));
        this.dampeningForm.addControl('duration', new UntypedFormControl(null));
        this.dampeningForm.addControl('timeUnit', new UntypedFormControl(null));
        break;
    }
  }

  public addNotification(type: string) {
    this.notifierService.getSchema(type)
      .pipe(take(1))
      .subscribe({
        next: (schema: GeneralFormValue) => {
          const notificationsGroup = this.formBuilder.group({
            type,
            configuration: {},
            schema,
          });
          this.notificationsForm.push(notificationsGroup);
        }
      })
  }

  public deleteNotification(index: number) {
    this.notificationsForm.removeAt(index);
  }

  public getGroup(control: AbstractControl) {
    return control as FormGroup;
  }
}
