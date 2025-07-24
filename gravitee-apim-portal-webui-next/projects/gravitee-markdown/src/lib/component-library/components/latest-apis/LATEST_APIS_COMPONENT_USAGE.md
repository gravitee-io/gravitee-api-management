# Latest APIs Component Usage

This document explains how to use the `LatestApisComponent` from the `gravitee-markdown` library.

## Overview

The `LatestApisComponent` displays a grid of APIs using the `ApiSearchService` and renders each API in a card with an action button. The component supports customization of the number of APIs displayed, styling, and button behavior.

## Basic Usage

```typescript
import { Component } from '@angular/core';
import { LatestApisComponent } from 'gravitee-markdown';

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [LatestApisComponent],
  template: `
    <app-latest-apis></app-latest-apis>
  `
})
export class HomepageComponent {}
```

## Advanced Usage with Customization

```typescript
import { Component } from '@angular/core';
import { LatestApisComponent } from 'gravitee-markdown';

@Component({
  selector: 'app-custom-apis',
  standalone: true,
  imports: [LatestApisComponent],
  template: `
    <app-latest-apis
      title="Featured APIs"
      subtitle="Discover our most popular APIs"
      [maxApis]="3"
      category="featured"
      searchQuery="popular"
      [cardElevation]="2"
      cardBackgroundColor="#f8f9fa"
      actionButtonText="Explore API"
      actionButtonVariant="outlined"
      actionButtonType="internal"
    ></app-latest-apis>
  `
})
export class CustomApisComponent {}
```

## Input Properties

### Content Inputs
- `title`: The main title for the component (default: 'Latest APIs')
- `subtitle`: Optional subtitle text

### Configuration Inputs
- `maxApis`: Maximum number of APIs to display (default: 5)
- `category`: API category filter (default: 'all')
- `searchQuery`: Search query for filtering APIs (default: '')

### Card Styling Inputs
- `cardElevation`: Card shadow elevation (0-5, default: 1)
- `cardBackgroundColor`: Card background color (default: '#ffffff')
- `cardBorderRadius`: Card border radius (default: '8px')

### Action Button Inputs
- `actionButtonText`: Text for the action button (default: 'View Details')
- `actionButtonVariant`: Button style variant ('filled', 'outlined', 'text', default: 'filled')
- `actionButtonType`: Button link type ('internal', 'external', default: 'internal')

## Component Features

### Responsive Grid Layout
The component uses CSS Grid with auto-fit columns that adapt to different screen sizes:
- Desktop: Multiple columns with minimum 300px width
- Mobile: Single column layout

### API Information Display
Each API card shows:
- API name as the card title
- Description (if available)
- Version and state information
- Tags (if available)
- Action button

### State Styling
API states are color-coded:
- **Published**: Green background
- **Created**: Orange background  
- **Unpublished**: Red background

### Loading and Error States
The component handles different states:
- **Loading**: Shows "Loading APIs..." message
- **Error**: Shows error message with details
- **Empty**: Shows "No APIs found." when no APIs are returned

## Customizing the API Details URL

The component generates URLs for API details using the `getApiDetailsUrl()` method. You can customize this by extending the component:

```typescript
import { LatestApisComponent } from 'gravitee-markdown';

@Component({
  selector: 'app-custom-latest-apis',
  template: `<app-latest-apis></app-latest-apis>`
})
export class CustomLatestApisComponent extends LatestApisComponent {
  override getApiDetailsUrl(apiId: string): string {
    // Custom URL generation logic
    return `/custom-api-path/${apiId}`;
  }
}
```

## Integration with ApiSearchService

The component automatically uses the `ApiSearchService` and respects the `GRAVITEE_MARKDOWN_BASE_URL` configuration. Make sure to provide the base URL token in your application:

```typescript
// In your app.module.ts or component
{
  provide: GRAVITEE_MARKDOWN_BASE_URL,
  useValue: 'https://your-backend-api.com/api/v1'
}
```

## Styling Customization

The component includes comprehensive CSS styling that can be overridden:

```scss
// Custom styles for the latest-apis component
app-latest-apis {
  .latest-apis-title {
    color: #your-brand-color;
  }
  
  .api-state {
    border-radius: 4px;
  }
  
  .api-tag {
    background-color: #your-tag-color;
  }
}
```

## Examples

### Simple Usage
```html
<app-latest-apis></app-latest-apis>
```

### Customized for Featured APIs
```html
<app-latest-apis
  title="Featured APIs"
  [maxApis]="4"
  category="featured"
  [cardElevation]="3"
  actionButtonText="Learn More"
  actionButtonVariant="outlined"
></app-latest-apis>
```

### Search-Specific APIs
```html
<app-latest-apis
  title="Payment APIs"
  subtitle="Secure payment processing solutions"
  [maxApis]="6"
  searchQuery="payment"
  actionButtonText="View API"
  actionButtonType="external"
></app-latest-apis>
```

## Testing

When testing components that use `LatestApisComponent`, ensure you provide the necessary dependencies:

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LatestApisComponent } from 'gravitee-markdown';
import { GRAVITEE_MARKDOWN_BASE_URL } from 'gravitee-markdown';

describe('LatestApisComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LatestApisComponent],
      providers: [
        {
          provide: GRAVITEE_MARKDOWN_BASE_URL,
          useValue: 'http://localhost:3000/api'
        }
      ]
    });
  });
});
``` 