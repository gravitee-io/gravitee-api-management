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
import { Component, input, computed, effect, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MarkdownService } from '../../../services/markdown.service';

@Component({
  selector: 'app-grid-cell',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="app-grid-cell" [class]="cellClasses()" [style]="cellStyles()">

        <ng-content></ng-content>

    </div>
  `,
  styles: [`
    .app-grid-cell {
      display: flex;
      flex-direction: column;
      min-height: 0;
      overflow: hidden;
    }

    .hide {
      display: none;
    }

    /* Cell sizing options */
    .app-grid-cell.span-1 {
      grid-column: span 1;
    }

    .app-grid-cell.span-2 {
      grid-column: span 2;
    }

    .app-grid-cell.span-3 {
      grid-column: span 3;
    }

    .app-grid-cell.span-4 {
      grid-column: span 4;
    }

    /* Responsive span adjustments */
    @media (max-width: 767px) {
      .app-grid-cell.span-2,
      .app-grid-cell.span-3,
      .app-grid-cell.span-4 {
        grid-column: span 1;
      }
    }

    @media (min-width: 768px) and (max-width: 1023px) {
      .app-grid-cell.span-3,
      .app-grid-cell.span-4 {
        grid-column: span 2;
      }
    }

    /* Cell styling */
    .app-grid-cell.padded {
      padding: 16px;
    }

    .app-grid-cell.padded-small {
      padding: 8px;
    }

    .app-grid-cell.padded-large {
      padding: 24px;
    }

    .app-grid-cell.bordered {
      border: 1px solid #e0e0e0;
      border-radius: 8px;
    }

    .app-grid-cell.shadowed {
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      border-radius: 8px;
    }

    .app-grid-cell.elevated {
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
      border-radius: 8px;
    }

    /* Background variants */
    .app-grid-cell.bg-light {
      background-color: #f8f9fa;
    }

    .app-grid-cell.bg-white {
      background-color: #ffffff;
    }

    .app-grid-cell.bg-primary {
      background-color: #007bff;
      color: #ffffff;
    }

    .app-grid-cell.bg-success {
      background-color: #28a745;
      color: #ffffff;
    }

    .app-grid-cell.bg-warning {
      background-color: #ffc107;
      color: #212529;
    }

    .app-grid-cell.bg-danger {
      background-color: #dc3545;
      color: #ffffff;
    }

    .app-grid-cell.bg-info {
      background-color: #17a2b8;
      color: #ffffff;
    }
  `]
})
export class GridCellComponent {
  // Content inputs
  markdown = input<boolean>(true);
  
  // Layout inputs
  span = input<1 | 2 | 3 | 4>(1);
  
  // Styling inputs
  padding = input<'none' | 'small' | 'medium' | 'large'>('medium');
  border = input<boolean>(false);
  shadow = input<'none' | 'small' | 'elevated'>('none');
  backgroundColor = input<'none' | 'light' | 'white' | 'primary' | 'success' | 'warning' | 'danger' | 'info'>('none');
  
  // Custom styling
  customBackgroundColor = input<string>('');
  customBorderColor = input<string>('');
  customBorderRadius = input<string>('');
  
  // Markdown rendering properties
  renderedContent!: string;
  
  constructor(
    private markdownService: MarkdownService,
    private elementRef: ElementRef
  ) {
    // effect(() => {
      // if (this.markdown()) {
      //   console.log('effect');
      //   this.renderMarkdownContent();
      // }
    // });
  }
  
  private renderMarkdownContent() {
    console.log(this.elementRef.nativeElement);
    console.log(document.querySelector('.hide'));
    
    const contentHide = this.elementRef.nativeElement.querySelector('.hide');
    const innerText = contentHide?.innerText || '';

    const content = this.elementRef.nativeElement.textContent || '';
    if (innerText.trim()) {
      const renderedContent = this.markdownService.render(innerText, '', '');
      const parser = new DOMParser();
      const document = parser.parseFromString(renderedContent, 'text/html');
      this.renderedContent = document.body.outerHTML;
    } else {
      this.renderedContent = '';
    }
  }
  
  // Computed properties
  cellClasses = computed(() => {
    const classes = ['app-grid-cell'];
    
    // Span classes
    classes.push(`span-${this.span()}`);
    
    // Padding classes
    if (this.padding() === 'small') {
      classes.push('padded-small');
    } else if (this.padding() === 'medium') {
      classes.push('padded');
    } else if (this.padding() === 'large') {
      classes.push('padded-large');
    }
    
    // Border classes
    if (this.border()) {
      classes.push('bordered');
    }
    
    // Shadow classes
    if (this.shadow() === 'small') {
      classes.push('shadowed');
    } else if (this.shadow() === 'elevated') {
      classes.push('elevated');
    }
    
    // Background classes
    if (this.backgroundColor() !== 'none') {
      classes.push(`bg-${this.backgroundColor()}`);
    }
    
    return classes.join(' ');
  });
  
  cellStyles = computed(() => {
    const styles: Record<string, string> = {};
    
    if (this.customBackgroundColor()) {
      styles['background-color'] = this.customBackgroundColor();
    }
    
    if (this.customBorderColor()) {
      styles['border-color'] = this.customBorderColor();
    }
    
    if (this.customBorderRadius()) {
      styles['border-radius'] = this.customBorderRadius();
    }
    
    return Object.entries(styles)
      .map(([key, value]) => `${key}: ${value}`)
      .join('; ');
  });

  // ngAfterViewInit() {
  //   if (this.markdown()) {
  //       this.renderMarkdownContent();
  //     }
  // }
} 