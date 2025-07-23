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
import { Component, input, computed, ContentChildren, QueryList, AfterContentInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardActionsComponent } from './card-actions.component';

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="app-card" [class]="cardClasses()" [style]="cardStyles()">
      <!-- Card Header -->
      <div class="card-header" *ngIf="title()" [class]="centered() ? 'card-header-centered' : ''">
        <h3 class="card-title">{{ title() }}</h3>
      </div>
      
      <!-- Card Content -->
      <div class="card-content" [class]="getCardContentClasses()">
        <ng-content></ng-content>
      </div>
    </div>
  `,
  styles: [`
    .app-card {
      display: flex;
      flex-direction: column;
      background-color: #ffffff;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      overflow: hidden;
      transition: all 0.2s ease;
    }

    .card-header {
      padding: 16px 20px 0 20px;
    }

    .card-title {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: #333333;
      line-height: 1.4;
    }

    .card-content {
      padding: 20px;
      flex: 1;
      color: #666666;
      line-height: 1.6;
    }

    .card-header-centered {
      text-align: center;
    }

    .card-content-centered {
      text-align: center;
    }

    .card-content-with-actions-centered {
      justify-content: center;
    }

    .card-actions-centered {
      text-align: center;
      justify-content: center;
    }   

    /* Elevation variants */
    .app-card.elevation-1 {
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24);
    }

    .app-card.elevation-2 {
      box-shadow: 0 3px 6px rgba(0, 0, 0, 0.16), 0 3px 6px rgba(0, 0, 0, 0.23);
    }

    .app-card.elevation-3 {
      box-shadow: 0 10px 20px rgba(0, 0, 0, 0.19), 0 6px 6px rgba(0, 0, 0, 0.23);
    }

    .app-card.elevation-4 {
      box-shadow: 0 14px 28px rgba(0, 0, 0, 0.25), 0 10px 10px rgba(0, 0, 0, 0.22);
    }

    .app-card.elevation-5 {
      box-shadow: 0 19px 38px rgba(0, 0, 0, 0.30), 0 15px 12px rgba(0, 0, 0, 0.22);
    }
  `]
})
export class CardComponent implements AfterContentInit {
  // Content inputs
  title = input<string>('');
  
  // Layout inputs
  centered = input<boolean>(false);
  
  // Styling inputs
  borderRadius = input<string>('8px');
  backgroundColor = input<string>('#ffffff');
  borderColor = input<string>('#e0e0e0');
  borderWidth = input<string>('1px');
  elevation = input<1 | 2 | 3 | 4 | 5>(2);
  
  // Content children
  @ContentChildren(CardActionsComponent) cardActions!: QueryList<CardActionsComponent>;
  
  // Computed properties
  hasCardActions = computed(() => this.cardActions.length > 0);
  
  ngAfterContentInit() {
    // This method is required by AfterContentInit interface
    // The computed property will automatically update when cardActions changes
  }
  
  getCardContentClasses(): string {
    const classes: string[] = [];
    
    if (this.centered()) {
      classes.push('card-content-centered');
    }
    
    if (this.hasCardActions() && this.centered()) {
      classes.push('card-content-with-actions-centered');
    }
    
    return classes.join(' ');
  }
  
  // Computed properties
  cardClasses = computed(() => {
    const classes = ['app-card'];
    if (this.elevation()) {
      classes.push(`elevation-${this.elevation()}`);
    }
    return classes.join(' ');
  });
  
  cardStyles = computed(() => {
    const styles: Record<string, string> = {};
    
    if (this.borderRadius()) {
      styles['border-radius'] = this.borderRadius();
    }
    
    if (this.backgroundColor()) {
      styles['background-color'] = this.backgroundColor();
    }
    
    if (this.borderColor()) {
      styles['border-color'] = this.borderColor();
    }
    
    if (this.borderWidth()) {
      styles['border-width'] = this.borderWidth();
    }
    
    return Object.entries(styles)
      .map(([key, value]) => `${key}: ${value}`)
      .join('; ');
  });
  
} 