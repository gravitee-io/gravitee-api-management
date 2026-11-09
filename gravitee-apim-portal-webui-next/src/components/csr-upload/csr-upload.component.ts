/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { Component, forwardRef, signal } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-csr-upload',
  standalone: true,
  imports: [FormsModule, MatFormFieldModule, MatIconModule, MatInputModule],
  templateUrl: './csr-upload.component.html',
  styleUrl: './csr-upload.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CsrUploadComponent),
      multi: true,
    },
  ],
})
export class CsrUploadComponent implements ControlValueAccessor {
  readonly value = signal('');
  readonly fileName = signal<string | null>(null);
  readonly disabled = signal(false);
  readonly fileError = signal<string | null>(null);

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(value: string | null): void {
    this.value.set(value ?? '');
    if (!value) {
      this.fileName.set(null);
    }
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  onTextareaChange(value: string): void {
    this.value.set(value);
    this.fileError.set(null);
    this.onChange(value);
    this.onTouched();
  }

  async onFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    const extension = file.name.includes('.') ? `.${file.name.split('.').pop()?.toLowerCase()}` : '';
    if (!['.csr', '.req', '.pem'].includes(extension)) {
      this.fileError.set($localize`:@@csrUploadInvalidExtension:Please choose a .csr, .req, or .pem file.`);
      input.value = '';
      return;
    }

    try {
      const content = await this.readFileAsText(file);
      this.value.set(content);
      this.fileName.set(file.name);
      this.fileError.set(null);
      this.onChange(content);
      this.onTouched();
    } catch {
      this.fileError.set($localize`:@@csrUploadReadError:Could not read the selected file. Please try again.`);
    }

    input.value = '';
  }

  private readFileAsText(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = () => reject(reader.error);
      reader.readAsText(file);
    });
  }
}
