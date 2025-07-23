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

@Component({
  selector: 'app-grid',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="app-grid" [class]="gridClasses()" [style]="gridStyles()">
      <ng-content></ng-content>
    </div>
  `,
  styles: [`
    .app-grid {
      display: grid;
      gap: 16px;
      width: 100%;
    }

    /* Mobile first - single column */
    .app-grid {
      grid-template-columns: 1fr;
    }

    /* Tablet - 2 columns */
    @media (min-width: 768px) {
      .app-grid.columns-2,
      .app-grid.columns-3,
      .app-grid.columns-4 {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    /* Desktop - responsive columns based on input */
    @media (min-width: 1024px) {
      .app-grid.columns-2 {
        grid-template-columns: repeat(2, 1fr);
      }
      
      .app-grid.columns-3 {
        grid-template-columns: repeat(3, 1fr);
      }
      
      .app-grid.columns-4 {
        grid-template-columns: repeat(4, 1fr);
      }
      
      .app-grid.columns-5 {
        grid-template-columns: repeat(5, 1fr);
      }
      
      .app-grid.columns-6 {
        grid-template-columns: repeat(6, 1fr);
      }
    }

    /* Custom gap sizes */
    .app-grid.gap-small {
      gap: 8px;
    }

    .app-grid.gap-medium {
      gap: 16px;
    }

    .app-grid.gap-large {
      gap: 24px;
    }

    .app-grid.gap-xl {
      gap: 32px;
    }

    /* Alignment options */
    .app-grid.align-start {
      align-items: start;
    }

    .app-grid.align-center {
      align-items: center;
    }

    .app-grid.align-end {
      align-items: end;
    }

    .app-grid.align-stretch {
      align-items: stretch;
    }
  `]
})
export class GridComponent {
  // Layout inputs
  columns = input<2 | 3 | 4 | 5 | 6>(3);
  gap = input<'small' | 'medium' | 'large' | 'xl'>('medium');
  align = input<'start' | 'center' | 'end' | 'stretch'>('stretch');
  
  // Styling inputs
  backgroundColor = input<string>('transparent');
  padding = input<string>('0');
  borderRadius = input<string>('0');
  
  // Computed properties
  gridClasses = computed(() => {
    const classes = ['app-grid'];
    
    // Column classes
    classes.push(`columns-${this.columns()}`);
    
    // Gap classes
    classes.push(`gap-${this.gap()}`);
    
    // Alignment classes
    classes.push(`align-${this.align()}`);
    
    return classes.join(' ');
  });
  
  gridStyles = computed(() => {
    const styles: Record<string, string> = {};
    
    if (this.backgroundColor() && this.backgroundColor() !== 'transparent') {
      styles['background-color'] = this.backgroundColor();
    }
    
    if (this.padding()) {
      styles['padding'] = this.padding();
    }
    
    if (this.borderRadius()) {
      styles['border-radius'] = this.borderRadius();
    }
    
    return Object.entries(styles)
      .map(([key, value]) => `${key}: ${value}`)
      .join('; ');
  });
} 