# API Search Service Usage

This document explains how to use the `ApiSearchService` from the `gravitee-markdown` library and how to configure the backend base URL.

## Overview

The `ApiSearchService` provides methods to search and retrieve API information from the backend. It uses the `GRAVITEE_MARKDOWN_BASE_URL` injection token to get the backend base URL.

## Configuration

### 1. Provide the Base URL Token

In your parent application, you need to provide the `GRAVITEE_MARKDOWN_BASE_URL` token with the actual backend URL:

```typescript
// In your app.module.ts or standalone component
import { GRAVITEE_MARKDOWN_BASE_URL } from 'gravitee-markdown';

@NgModule({
  // ... other imports
  providers: [
    {
      provide: GRAVITEE_MARKDOWN_BASE_URL,
      useValue: 'https://your-backend-api.com/api/v1'
    }
  ]
})
export class AppModule { }
```

Or in a standalone component:

```typescript
import { Component } from '@angular/core';
import { GRAVITEE_MARKDOWN_BASE_URL } from 'gravitee-markdown';

@Component({
  // ... component configuration
  providers: [
    {
      provide: GRAVITEE_MARKDOWN_BASE_URL,
      useValue: 'https://your-backend-api.com/api/v1'
    }
  ]
})
export class YourComponent { }
```

### 2. Using the Service

```typescript
import { Component, OnInit } from '@angular/core';
import { ApiSearchService, ApiSearchResponse } from 'gravitee-markdown';

@Component({
  selector: 'app-api-list',
  template: `
    <div>
      <h2>APIs</h2>
      <div *ngFor="let api of apis">
        <h3>{{ api.name }}</h3>
        <p>{{ api.description }}</p>
        <p>Version: {{ api.version }}</p>
        <p>State: {{ api.state }}</p>
      </div>
    </div>
  `
})
export class ApiListComponent implements OnInit {
  apis: Api[] = [];

  constructor(private apiSearchService: ApiSearchService) {}

  ngOnInit() {
    // Search for APIs
    this.apiSearchService.search(1, 'all', '', 10).subscribe({
      next: (response: ApiSearchResponse) => {
        this.apis = response.data;
        console.log(`Found ${response.metadata.total} APIs`);
      },
      error: (error) => {
        console.error('Error fetching APIs:', error);
      }
    });
  }

  // Get specific API details
  getApiDetails(apiId: string) {
    this.apiSearchService.details(apiId).subscribe({
      next: (api) => {
        console.log('API details:', api);
      },
      error: (error) => {
        console.error('Error fetching API details:', error);
      }
    });
  }
}
```

## Service Methods

### `search(page, category, q, size)`

Searches for APIs with the following parameters:
- `page`: Page number (default: 1)
- `category`: Category filter (default: 'all')
- `q`: Search query (default: '')
- `size`: Number of results per page (default: 9)

Returns an `Observable<ApiSearchResponse>` with:
- `data`: Array of `Api` objects
- `metadata`: Pagination information

### `details(apiId)`

Retrieves detailed information for a specific API.

Returns an `Observable<Api>` with the API details.

## API Interface

```typescript
interface Api {
  id: string;
  name: string;
  description?: string;
  version: string;
  state: string;
  contextPath: string;
  tags?: string[];
  categories?: string[];
}
```

## Error Handling

The service uses Angular's HttpClient, so errors are handled through the Observable error channel:

```typescript
this.apiSearchService.search().subscribe({
  next: (response) => {
    // Handle success
  },
  error: (error) => {
    // Handle error (network, 4xx, 5xx, etc.)
    console.error('API search failed:', error);
  }
});
```

## Testing

When testing components that use this service, you can provide a mock value:

```typescript
// In your test file
import { GRAVITEE_MARKDOWN_BASE_URL } from 'gravitee-markdown';

TestBed.configureTestingModule({
  providers: [
    {
      provide: GRAVITEE_MARKDOWN_BASE_URL,
      useValue: 'http://localhost:3000/api'
    }
  ]
});
``` 