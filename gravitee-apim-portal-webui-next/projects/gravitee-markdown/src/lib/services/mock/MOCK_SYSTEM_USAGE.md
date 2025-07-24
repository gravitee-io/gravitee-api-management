# Mock System Usage

This document explains how to use the mock system in the `gravitee-markdown` library to switch between mock and real backend calls.

## Overview

The library provides a flexible mock system that allows the parent application to easily switch between mock data and real backend calls. This is useful for:

- Development and testing without a backend
- Demo environments
- Offline development
- Consistent data for UI development

## Configuration

### 1. Enable Mock Mode

To enable mock mode, provide the `GRAVITEE_MARKDOWN_MOCK_MODE` token:

```typescript
// In your app.module.ts or standalone component
import { 
  GRAVITEE_MARKDOWN_BASE_URL, 
  GRAVITEE_MARKDOWN_MOCK_MODE 
} from 'gravitee-markdown';

@NgModule({
  // ... other imports
  providers: [
    {
      provide: GRAVITEE_MARKDOWN_MOCK_MODE,
      useValue: true // Enable mock mode
    }
  ]
})
export class AppModule { }
```

### 2. Disable Mock Mode (Real Backend)

To use real backend calls, set the mock mode to false and provide a valid base URL:

```typescript
import { 
  GRAVITEE_MARKDOWN_BASE_URL, 
  GRAVITEE_MARKDOWN_MOCK_MODE 
} from 'gravitee-markdown';

@Component({
  // ... component configuration
  providers: [
    {
      provide: GRAVITEE_MARKDOWN_MOCK_MODE,
      useValue: false // Disable mock mode
    },
    {
      provide: GRAVITEE_MARKDOWN_BASE_URL,
      useValue: 'https://your-backend-api.com/api/v1'
    }
  ]
})
export class YourComponent { }
```

## Mock Data

The library includes realistic mock data for APIs:

### Sample Mock APIs
- **Payment Processing API** - E-commerce payment processing
- **User Management API** - Authentication and user management
- **Notification Service API** - Email, SMS, and push notifications
- **Analytics Dashboard API** - Business intelligence and reporting
- **File Storage API** - Cloud file storage and management
- **Machine Learning API** - AI and ML services

### Mock Data Features
- Realistic API names and descriptions
- Various API states (published, created, unpublished)
- Tags and categories for filtering
- Version information
- Context paths

## Using the Factory Pattern

The library uses a factory pattern to automatically switch between mock and real services:

```typescript
import { ApiSearchFactory } from 'gravitee-markdown';

@Component({
  // ... component configuration
})
export class YourComponent {
  constructor(private apiSearchFactory: ApiSearchFactory) {}

  loadApis() {
    // The factory automatically returns mock or real service based on configuration
    this.apiSearchFactory.getService().search(1, 'all', '', 5)
      .subscribe({
        next: (response) => {
          console.log('APIs loaded:', response.data);
        },
        error: (error) => {
          console.error('Error loading APIs:', error);
        }
      });
  }
}
```

## Environment-Based Configuration

You can configure mock mode based on your environment:

### Development Environment
```typescript
// environment.ts
export const environment = {
  production: false,
  mockMode: true,
  apiBaseUrl: 'http://localhost:3000/api'
};

// app.module.ts
import { environment } from './environments/environment';

@NgModule({
  providers: [
    {
      provide: GRAVITEE_MARKDOWN_MOCK_MODE,
      useValue: environment.mockMode
    },
    {
      provide: GRAVITEE_MARKDOWN_BASE_URL,
      useValue: environment.apiBaseUrl
    }
  ]
})
export class AppModule { }
```

### Production Environment
```typescript
// environment.prod.ts
export const environment = {
  production: true,
  mockMode: false,
  apiBaseUrl: 'https://api.yourcompany.com/v1'
};
```

## Testing with Mock Data

### Unit Tests
```typescript
import { TestBed } from '@angular/core/testing';
import { ApiSearchFactory } from 'gravitee-markdown';
import { GRAVITEE_MARKDOWN_MOCK_MODE } from 'gravitee-markdown';

describe('ApiSearchFactory', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: GRAVITEE_MARKDOWN_MOCK_MODE,
          useValue: true // Always use mock in tests
        }
      ]
    });
  });

  it('should return mock service when mock mode is enabled', () => {
    const factory = TestBed.inject(ApiSearchFactory);
    const service = factory.getService();
    
    service.search(1, 'all', '', 5).subscribe(response => {
      expect(response.data.length).toBeGreaterThan(0);
      expect(response.data[0].name).toBeDefined();
    });
  });
});
```

### Integration Tests
```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LatestApisComponent } from 'gravitee-markdown';
import { GRAVITEE_MARKDOWN_MOCK_MODE } from 'gravitee-markdown';

describe('LatestApisComponent', () => {
  let component: LatestApisComponent;
  let fixture: ComponentFixture<LatestApisComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LatestApisComponent],
      providers: [
        {
          provide: GRAVITEE_MARKDOWN_MOCK_MODE,
          useValue: true
        }
      ]
    });
    
    fixture = TestBed.createComponent(LatestApisComponent);
    component = fixture.componentInstance;
  });

  it('should load mock APIs', () => {
    fixture.detectChanges();
    
    // Wait for async operations
    fixture.whenStable().then(() => {
      expect(component.apis().length).toBeGreaterThan(0);
    });
  });
});
```

## Customizing Mock Data

You can extend the mock data by creating your own mock service:

```typescript
import { Injectable } from '@angular/core';
import { Observable, of, delay } from 'rxjs';
import { ApiSearchService, Api, ApiSearchResponse } from 'gravitee-markdown';

@Injectable({
  providedIn: 'root',
})
export class CustomMockApiSearchService implements Pick<ApiSearchService, 'search' | 'details'> {
  
  private customMockApis: Api[] = [
    {
      id: 'custom-api-1',
      name: 'Custom API',
      description: 'Your custom API description',
      version: '1.0.0',
      state: 'published',
      contextPath: '/custom-api',
      tags: ['custom', 'api'],
      categories: ['custom']
    }
  ];

  search(page = 1, category: string = 'all', q: string = '', size = 9): Observable<ApiSearchResponse> {
    return of({
      data: this.customMockApis,
      metadata: {
        total: this.customMockApis.length,
        page,
        size
      }
    }).pipe(delay(300));
  }

  details(apiId: string): Observable<Api> {
    const api = this.customMockApis.find(a => a.id === apiId);
    if (!api) {
      throw new Error(`API with ID '${apiId}' not found`);
    }
    return of(api).pipe(delay(200));
  }
}
```

## Performance Considerations

### Mock Mode Benefits
- No network requests
- Consistent response times
- No backend dependencies
- Faster development cycles

### Real Mode Benefits
- Real data and states
- Actual API behavior
- Integration testing
- Production-like environment

## Error Handling

Both mock and real services handle errors consistently:

```typescript
this.apiSearchFactory.getService().search(1, 'all', '', 5)
  .subscribe({
    next: (response) => {
      // Handle successful response
      console.log('APIs loaded:', response.data);
    },
    error: (error) => {
      // Handle errors (works for both mock and real services)
      console.error('Error loading APIs:', error);
      
      if (error.status === 404) {
        console.log('No APIs found');
      } else if (error.status === 500) {
        console.log('Server error');
      }
    }
  });
```

## Migration Guide

### From Direct Service Usage
**Before:**
```typescript
constructor(private apiSearchService: ApiSearchService) {}

loadApis() {
  this.apiSearchService.search(1, 'all', '', 5).subscribe(/* ... */);
}
```

**After:**
```typescript
constructor(private apiSearchFactory: ApiSearchFactory) {}

loadApis() {
  this.apiSearchFactory.getService().search(1, 'all', '', 5).subscribe(/* ... */);
}
```

### Configuration Migration
**Before:**
```typescript
providers: [
  {
    provide: GRAVITEE_MARKDOWN_BASE_URL,
    useValue: 'https://api.example.com'
  }
]
```

**After:**
```typescript
providers: [
  {
    provide: GRAVITEE_MARKDOWN_MOCK_MODE,
    useValue: false // or true for mock mode
  },
  {
    provide: GRAVITEE_MARKDOWN_BASE_URL,
    useValue: 'https://api.example.com'
  }
]
```

This mock system provides a flexible and maintainable way to switch between mock and real backend calls, making development and testing much more efficient. 