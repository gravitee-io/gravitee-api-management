/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, input, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-copy-code',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="copy-code-container">
      <div class="code-content">
        <pre><code>{{ text() }}</code></pre>
      </div>
      <button class="copy-button" (click)="copyToClipboard()">
        Copy
      </button>
    </div>
  `,
  styles: [`
    .copy-code-container {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem;
      background: #f5f5f5;
      border-radius: 4px;
      margin: 1rem 0;
    }
    
    .code-content {
      flex: 1;
    }
    
    .code-content pre {
      margin: 0;
      white-space: pre-wrap;
      word-break: break-all;
    }
    
    .copy-button {
      padding: 0.5rem 1rem;
      background: #007bff;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
    }
    
    .copy-button:hover {
      background: #0056b3;
    }
  `]
})
export class CopyCodeComponent {
  text = input<string>('');
  
  copyToClipboard(): void {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(this.text());
    } else {
      // Fallback for older browsers
      const textArea = document.createElement('textarea');
      textArea.value = this.text();
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
    }
  }
} 