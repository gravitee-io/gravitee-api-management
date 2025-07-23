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
import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

export type ButtonVariant = 'filled' | 'outlined' | 'text';
export type ButtonType = 'internal' | 'external';

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <a 
      [href]="href()" 
      [target]="target()"
      [rel]="rel()"
      class="app-button"
      [class]="buttonClasses()"
      [style]="buttonStyles()"
    >
      <ng-content></ng-content>
    </a>
  `,
  styles: [`
    .app-button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 8px 12px;
      font-size: 14px;
      font-weight: 500;
      text-decoration: none;
      cursor: pointer;
      transition: all 0.2s ease;
      border: none;
      outline: none;
      line-height: 1.2;
      white-space: nowrap;
      user-select: none;
      border: 1px solid transparent;
    }

    .app-button:hover {
        text-decoration: none;
    }

    .app-button.filled {
      background-color: #1976d2;
      color: #ffffff;
      border-radius: 4px;
      text-transform: none;
    }

    .app-button.filled:hover {
      background-color: #1565c0;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    }

    .app-button.outlined {
      background-color: transparent;
      color: #1976d2;
      border-color: #1976d2;
      border-radius: 4px;
      text-transform: none;
    }

    .app-button.outlined:hover {
      background-color: rgba(25, 118, 210, 0.04);
      border-color: #1565c0;
      color: #1565c0;
    }

    .app-button.text {
      background-color: transparent;
      color: #1976d2;
      border-radius: 4px;
      text-transform: none;
    }

    .app-button.text:hover {
      background-color: rgba(25, 118, 210, 0.04);
      color: #1565c0;
    }

    .app-button:focus {
      outline: 2px solid #1976d2;
      outline-offset: 2px;
    }

    .app-button:active {
      transform: translateY(1px);
    }
  `]
})
export class ButtonComponent {
  // Content inputs
  text = input<string>('');
  
  // Link inputs
  href = input<string>('');
  type = input<ButtonType>('internal');
  
  // Styling inputs
  variant = input<ButtonVariant>('filled');
  borderRadius = input<string>('4px');
  backgroundColor = input<string>('');
  textColor = input<string>('');
  textTransform = input<string>('none');
  
  // Computed properties
  target = computed(() => this.type() === 'external' ? '_blank' : '_self');
  rel = computed(() => this.type() === 'external' ? 'noopener noreferrer' : '');
  
  buttonClasses = computed(() => {
    const classes = ['app-button'];
    if (this.variant()) {
      classes.push(this.variant());
    }
    return classes.join(' ');
  });
  
  buttonStyles = computed(() => {
    const styles: Record<string, string> = {};
    
    if (this.borderRadius()) {
      styles['border-radius'] = this.borderRadius();
    }
    
    if (this.backgroundColor()) {
      styles['background-color'] = this.backgroundColor();
    }
    
    if (this.textColor()) {
      styles['color'] = this.textColor();
    }
    
    if (this.textTransform()) {
      styles['text-transform'] = this.textTransform();
    }
    
    return Object.entries(styles)
      .map(([key, value]) => `${key}: ${value}`)
      .join('; ');
  });
} 