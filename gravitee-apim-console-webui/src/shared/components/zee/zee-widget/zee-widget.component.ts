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
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';

import { ZeeGenerateRequest, ZeeResourceAdapter, ZeeResourceType } from '../zee.model';
import { ZeeService } from '../zee.service';

type ZeeState = 'idle' | 'loading' | 'preview' | 'error';

/** Allowed file extensions for context uploads (UX check; backend re-validates). */
const ALLOWED_EXTENSIONS = new Set(['json', 'yaml', 'yml', 'txt', 'md']);

/** Maximum files per request (must match backend MAX_FILES = 5). */
const MAX_FILES = 5;

/** Maximum prompt length in UTF-16 code units (must match backend MAX_PROMPT_LENGTH = 2000). */
export const MAX_PROMPT_LENGTH = 2000;

@Component({
  selector: 'zee-widget',
  templateUrl: './zee-widget.component.html',
  styleUrls: ['./zee-widget.component.scss'],
  standalone: false,
})
export class ZeeWidgetComponent {
  @Input() resourceType!: ZeeResourceType;
  @Input() adapter!: ZeeResourceAdapter;
  @Input() contextData?: Record<string, any>;
  /** Max file size in MB that is enforced in the UI (default matches backend 5 MB hard limit). */
  @Input() maxFileSizeMb = 5;
  @Output() accepted = new EventEmitter<any>();
  @Output() rejected = new EventEmitter<void>();

  readonly MAX_PROMPT_LENGTH = MAX_PROMPT_LENGTH;
  isOpen = false;
  state: ZeeState = 'idle';
  prompt = '';
  files: File[] = [];
  generatedResource: any = null;
  errorMessage = '';
  fileValidationErrors: string[] = [];

  togglePanel(): void {
    this.isOpen = !this.isOpen;
  }

  constructor(
    private readonly zeeService: ZeeService,
    private readonly snackBar: MatSnackBar,
  ) {}

  get isSubmitDisabled(): boolean {
    return !this.prompt.trim() || this.prompt.length > MAX_PROMPT_LENGTH;
  }

  onSubmit(): void {
    if (this.isSubmitDisabled) {
      return;
    }
    this.state = 'loading';
    const request: ZeeGenerateRequest = {
      resourceType: this.resourceType,
      prompt: this.prompt,
      contextData: this.contextData,
    };
    this.zeeService.generate(request, this.files).subscribe({
      next: (res) => {
        this.generatedResource = res.generated;
        this.state = 'preview';
      },
      error: (err: HttpErrorResponse | { status?: number; message?: string }) => {
        if ((err as HttpErrorResponse).status === 429) {
          this.snackBar.open("Zee is cooling down — you've hit the request limit. Please wait a minute and try again.", 'Dismiss', {
            duration: 6000,
          });
          this.state = 'idle';
        } else {
          this.errorMessage = (err as any)?.message ?? 'Generation failed';
          this.state = 'error';
        }
      },
    });
  }

  onAccept(): void {
    const payload = this.adapter.transform(this.generatedResource, this.contextData);
    this.accepted.emit(payload);
    this.reset();
  }

  onReject(): void {
    this.rejected.emit();
    this.reset();
  }

  reset(): void {
    this.state = 'idle';
    this.prompt = '';
    this.files = [];
    this.generatedResource = null;
    this.errorMessage = '';
    this.fileValidationErrors = [];
    this.isOpen = false;
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.addFiles(Array.from(input.files));
    }
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer?.files) {
      this.addFiles(Array.from(event.dataTransfer.files));
    }
  }

  removeFile(index: number): void {
    this.files = this.files.filter((_, i) => i !== index);
    // Re-clear count errors since removing a file may resolve them
    this.fileValidationErrors = this.fileValidationErrors.filter((e) => !e.startsWith('Too many files'));
  }

  /**
   * Validates incoming files against type, size, and total count (existing + new).
   * Only valid files are appended; errors are surfaced via `fileValidationErrors`.
   */
  addFiles(incoming: File[]): void {
    const maxBytes = this.maxFileSizeMb * 1024 * 1024;
    const errors: string[] = [];
    const valid: File[] = [];

    for (const file of incoming) {
      const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
      if (!ALLOWED_EXTENSIONS.has(ext)) {
        errors.push(`"${file.name}" is not an allowed type. Allowed: json, yaml, yml, txt, md.`);
        continue;
      }
      if (file.size > maxBytes) {
        errors.push(`"${file.name}" exceeds the ${this.maxFileSizeMb} MB limit.`);
        continue;
      }
      valid.push(file);
    }

    const afterAdd = this.files.length + valid.length;
    if (afterAdd > MAX_FILES) {
      errors.push(`Too many files. You can attach at most ${MAX_FILES} files (currently have ${this.files.length}).`);
      // Append only as many as the limit allows
      const slots = MAX_FILES - this.files.length;
      this.files = [...this.files, ...valid.slice(0, slots)];
    } else {
      this.files = [...this.files, ...valid];
    }

    this.fileValidationErrors = errors;
  }
}
