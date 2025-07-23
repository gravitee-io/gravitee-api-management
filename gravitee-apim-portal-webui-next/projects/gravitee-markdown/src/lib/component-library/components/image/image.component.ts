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
  selector: 'app-image',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="image-container" [class]="containerClasses()" [style]="containerStyles()">
      <img 
        [src]="src()" 
        [alt]="alt()"
        [class]="imageClasses()"
        [style]="imageStyles()"
        (error)="onImageError()"
        (load)="onImageLoad()"
      />
    </div>
  `,
  styles: [`
    .image-container {
      display: block;
      margin: 1rem 0;
    }
    
    .image-container.centered {
      text-align: center;
    }
    
    .image-container.centered img {
      margin: 0 auto;
    }
    
    .image-container img {
      max-width: 100%;
      height: auto;
      display: block;
    }
    
    .image-container img.rounded {
      border-radius: 8px;
    }
    
    .image-container img.rounded-sm {
      border-radius: 4px;
    }
    
    .image-container img.rounded-lg {
      border-radius: 12px;
    }
    
    .image-container img.rounded-full {
      border-radius: 50%;
    }
    
    .image-container img.error {
      border: 2px dashed #dc3545;
      background-color: #f8d7da;
      min-height: 100px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #721c24;
      font-size: 14px;
    }
  `]
})
export class ImageComponent {
  // Input properties
  src = input<string>('');
  alt = input<string>('');
  centered = input<boolean>(false);
  rounded = input<'none' | 'sm' | 'md' | 'lg' | 'full'>('none');
  maxWidth = input<string>('');
  maxHeight = input<string>('');
  width = input<string>('');
  height = input<string>('');
  
  // Computed properties
  containerClasses = computed(() => {
    const classes = ['image-container'];
    if (this.centered()) {
      classes.push('centered');
    }
    return classes.join(' ');
  });
  
  imageClasses = computed(() => {
    const classes = [];
    if (this.rounded() !== 'none') {
      classes.push(`rounded-${this.rounded()}`);
    }
    return classes.join(' ');
  });
  
  containerStyles = computed(() => {
    const styles: Record<string, string> = {};
    return Object.entries(styles)
      .map(([key, value]) => `${key}: ${value}`)
      .join('; ');
  });
  
  imageStyles = computed(() => {
    const styles: Record<string, string> = {};
    
    if (this.maxWidth()) {
      styles['max-width'] = this.maxWidth();
    }
    
    if (this.maxHeight()) {
      styles['max-height'] = this.maxHeight();
    }
    
    if (this.width()) {
      styles['width'] = this.width();
    }
    
    if (this.height()) {
      styles['height'] = this.height();
    }
    
    return Object.entries(styles)
      .map(([key, value]) => `${key}: ${value}`)
      .join('; ');
  });
  
  // Event handlers
  onImageError(): void {
    console.warn('Failed to load image:', this.src());
  }
  
  onImageLoad(): void {
    // Image loaded successfully
  }
} 