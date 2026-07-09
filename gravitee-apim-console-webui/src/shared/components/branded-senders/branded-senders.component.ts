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
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ErrorHandler,
  forwardRef,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatIconModule } from '@angular/material/icon';
import { GioFormTagsInputModule } from '@gravitee/ui-particles-angular';

import { BrandedSender } from '../../../entities/brandedSender';

type BrandedSenderForm = FormGroup<{
  domains: FormControl<string[]>;
  from: FormControl<string>;
  subject: FormControl<string>;
}>;

const SUBJECT_MAX_LENGTH = 255;
// The backend persists the whole list as one serialized parameter with a hard cap (BrandedSenders.MAX_SERIALIZED_LENGTH).
// Mirror it here as an aggregate guard so many individually-valid configurations can't silently overflow it at save time.
const MAX_SERIALIZED_LENGTH = 4000;

// Client-side format checks so the admin gets a helpful inline error (RFC 1035 host name, and a bare address or a
// "Name <addr>" sender). These are UX-only and intentionally lenient — the backend only rejects null entries and
// CR/LF, so this is NOT a mirror of backend validation. The final label allows either an alphabetic TLD or an
// ACE/punycode IDN TLD (e.g. `xn--p1ai`), which contains digits and hyphens. The `i` flag keeps hostnames
// case-insensitive (RFC 4343) so e.g. an `XN--P1AI` TLD is accepted the same in a bare domain and in a sender address.
const TLD = '(?:[a-zA-Z]{2,}|xn--[a-zA-Z0-9-]{1,59})';
const DOMAIN_PATTERN = new RegExp(String.raw`^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+${TLD}$`, 'i');
const EMAIL_PATTERN = new RegExp(String.raw`^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+${TLD}$`, 'i');

/** Rejects a `string[]` where any entry is not a valid, dot-separated host name (case-insensitive). */
const domainsValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const domains: string[] = control.value ?? [];
  const invalid = domains.filter(domain => !DOMAIN_PATTERN.test((domain ?? '').trim().toLowerCase()));
  return invalid.length > 0 ? { invalidDomains: invalid } : null;
};

/** Accepts a bare address (`user@example.com`) or the personal-name form (`Name <user@example.com>`). */
const senderAddressValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value: string = (control.value ?? '').trim();
  if (value === '') {
    return null; // emptiness is handled by Validators.required
  }
  const personalName = /^.*<([^<>]+)>$/.exec(value);
  const address = personalName ? personalName[1].trim() : value;
  return EMAIL_PATTERN.test(address) ? null : { invalidSenderAddress: true };
};

/**
 * Rejects the whole list when its serialized size would exceed the backend's aggregate cap. This is a fail-fast UX
 * guard only — the backend stays the authority (and its rejection is surfaced by the save error handler). It mirrors
 * the backend escaping each ";" to a 6-char sequence before measuring, so the estimate stays conservative.
 */
const serializedSizeValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const json = JSON.stringify(control.value ?? []);
  // Each ";" is escaped to a 6-char sequence server-side (net +5 chars), so count them into the estimate.
  const serializedLength = json.length + (json.match(/;/g)?.length ?? 0) * 5;
  return serializedLength > MAX_SERIALIZED_LENGTH
    ? { maxSerializedLength: { max: MAX_SERIALIZED_LENGTH, actual: serializedLength } }
    : null;
};

/** Rejects the list when the same domain is used in more than one configuration (cross-config uniqueness). */
const duplicateDomainsValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const configurations: BrandedSender[] = control.value ?? [];
  const seen = new Set<string>();
  const duplicates = new Set<string>();
  const trackDomain = (domain: string) => {
    const normalized = (domain ?? '').trim().toLowerCase();
    if (normalized === '') {
      return;
    }
    if (seen.has(normalized)) {
      duplicates.add(normalized);
    }
    seen.add(normalized);
  };
  configurations.forEach(configuration => (configuration.domains ?? []).forEach(trackDomain));
  return duplicates.size > 0 ? { duplicateDomains: [...duplicates] } : null;
};

@Component({
  selector: 'branded-senders',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTooltipModule,
    MatIconModule,
    GioFormTagsInputModule,
  ],
  templateUrl: './branded-senders.component.html',
  styleUrl: './branded-senders.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => BrandedSendersComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => BrandedSendersComponent), multi: true },
  ],
})
export class BrandedSendersComponent implements ControlValueAccessor, Validator {
  private readonly destroyRef = inject(DestroyRef);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly errorHandler = inject(ErrorHandler);

  /** The default sender/subject shown read-only for context (the `EMAIL_FROM` / `EMAIL_SUBJECT` fallback). */
  readonly defaultFrom = input('');
  readonly defaultSubject = input('');

  /**
   * Whether the shown configurations are inherited from the Organization scope (no Environment-level override).
   * Drives the per-configuration "Inherited from Org" badge; only meaningful at Environment scope. A true value does
   * not imply the Organization has a non-empty configuration, so the badge only renders for configurations actually
   * displayed (none when the list is empty).
   */
  readonly inheritedFromOrg = input(false);

  /** Whether to offer the reset-to-inherited action; the parent decides based on scope, permissions and override state. */
  readonly canReset = input(false);

  /** Emitted when the user triggers the reset action; the parent performs the actual reset. */
  readonly reset = output<void>();

  protected readonly subjectMaxLength = SUBJECT_MAX_LENGTH;
  protected readonly configurations = new FormArray<BrandedSenderForm>([], [serializedSizeValidator, duplicateDomainsValidator]);
  protected readonly isDisabled = signal(false);
  // Flipped true on the first user edit and reset on each (programmatic) writeValue. While true, the configurations are
  // diverging into an Environment override, so the "Inherited from Org" badge is hidden until the edit is saved or
  // discarded (both of which trigger a fresh writeValue).
  protected readonly hasUserEdited = signal(false);

  private _onChange: (value: BrandedSender[]) => void = () => undefined;
  private _onTouched: () => void = () => undefined;

  // A CVA propagates its value on every form change; this subscription is the bridge to _onChange. A throw in the
  // callback is caught so it can't terminate this long-lived stream (which would silently stop propagating edits to
  // the parent control); it is reported via Angular's ErrorHandler instead.
  private readonly _propagateValue = this.configurations.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
    // valueChanges only fires on user edits — writeValue rebuilds the array with emitEvent: false — so any emission
    // means the user has diverged from the inherited value.
    this.hasUserEdited.set(true);
    try {
      this._onChange(this.snapshot());
    } catch (error) {
      this.errorHandler.handleError(error);
    }
  });

  // --- ControlValueAccessor ---

  writeValue(value: BrandedSender[] | null): void {
    // A fresh (server-driven) value: the list once again reflects the saved/inherited state, so clear the edited flag.
    this.hasUserEdited.set(false);
    this.configurations.clear({ emitEvent: false });
    (value ?? []).forEach(brandedSender => this.configurations.push(this.newConfiguration(brandedSender), { emitEvent: false }));
    // Freshly-built controls are always enabled; Angular only calls setDisabledState() once at setup, so a later
    // writeValue (e.g. a save-bar Discard/reset) would otherwise re-enable a control that must stay disabled.
    if (this.isDisabled()) {
      this.configurations.disable({ emitEvent: false });
    }
    // The FormArray is not a signal, so a programmatic value change must be flagged for OnPush re-render.
    this.changeDetector.markForCheck();
  }

  registerOnChange(fn: (value: BrandedSender[]) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.isDisabled.set(isDisabled);
    if (isDisabled) {
      this.configurations.disable({ emitEvent: false });
    } else {
      this.configurations.enable({ emitEvent: false });
    }
  }

  // --- Validator ---

  validate(): ValidationErrors | null {
    return this.configurations.valid ? null : { brandedSenders: 'invalid' };
  }

  // --- Template actions ---

  protected addConfiguration(): void {
    this.configurations.push(this.newConfiguration());
    this._onTouched();
  }

  protected removeConfiguration(index: number): void {
    this.configurations.removeAt(index);
    this._onTouched();
  }

  private newConfiguration(brandedSender?: BrandedSender): BrandedSenderForm {
    return new FormGroup({
      domains: new FormControl<string[]>(brandedSender?.domains ?? [], {
        nonNullable: true,
        validators: [Validators.required, domainsValidator],
      }),
      from: new FormControl<string>(brandedSender?.from ?? '', {
        nonNullable: true,
        validators: [Validators.required, senderAddressValidator],
      }),
      subject: new FormControl<string>(brandedSender?.subject ?? '', {
        nonNullable: true,
        validators: [Validators.maxLength(SUBJECT_MAX_LENGTH)],
      }),
    });
  }

  private snapshot(): BrandedSender[] {
    // Persist the normalized form the validators accept, so what is stored matches what was validated and reaches
    // send-time domain matching without stray whitespace (domains are matched case-insensitively, hence lowercased).
    return this.configurations.controls.map(group => ({
      domains: group.controls.domains.value.map(domain => domain.trim().toLowerCase()),
      from: group.controls.from.value.trim(),
      subject: group.controls.subject.value,
    }));
  }
}
