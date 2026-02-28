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

import { ZeeGenerateRequest, ZeeResourceAdapter, ZeeResourceType } from '../zee.model';
import { ZeeService } from '../zee.service';

type ZeeState = 'idle' | 'loading' | 'preview' | 'error';

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
  @Output() accepted = new EventEmitter<any>();
  @Output() rejected = new EventEmitter<void>();

  state: ZeeState = 'idle';
  prompt = '';
  files: File[] = [];
  generatedResource: any = null;
  errorMessage = '';

  constructor(private readonly zeeService: ZeeService) {}

  onSubmit(): void {
    if (!this.prompt.trim()) {
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
      error: (err) => {
        this.errorMessage = err?.message ?? 'Generation failed';
        this.state = 'error';
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
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.files = [...this.files, ...Array.from(input.files)];
    }
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer?.files) {
      this.files = [...this.files, ...Array.from(event.dataTransfer.files)];
    }
  }

  removeFile(index: number): void {
    this.files = this.files.filter((_, i) => i !== index);
  }
}
