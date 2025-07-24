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
import { Api, ApiSearchResponse } from '../api-search.service';

export const MOCK_APIS: Api[] = [
  {
    id: 'api-1',
    name: 'Payment Processing API',
    description: 'Secure payment processing and transaction management for e-commerce applications.',
    version: '2.1.0',
    state: 'published',
    contextPath: '/payment-api',
    tags: ['payment', 'ecommerce', 'security'],
    categories: ['finance', 'payments']
  },
  {
    id: 'api-2',
    name: 'User Management API',
    description: 'Complete user authentication, authorization, and profile management system.',
    version: '1.5.2',
    state: 'published',
    contextPath: '/user-api',
    tags: ['authentication', 'users', 'security'],
    categories: ['identity', 'management']
  },
  {
    id: 'api-3',
    name: 'Notification Service API',
    description: 'Real-time notification delivery via email, SMS, and push notifications.',
    version: '3.0.1',
    state: 'published',
    contextPath: '/notification-api',
    tags: ['notifications', 'email', 'sms'],
    categories: ['communication', 'messaging']
  },
  {
    id: 'api-4',
    name: 'Analytics Dashboard API',
    description: 'Comprehensive analytics and reporting for business intelligence and data visualization.',
    version: '1.8.0',
    state: 'created',
    contextPath: '/analytics-api',
    tags: ['analytics', 'dashboard', 'reporting'],
    categories: ['business-intelligence', 'data']
  },
  {
    id: 'api-5',
    name: 'File Storage API',
    description: 'Cloud-based file storage and management with advanced security features.',
    version: '2.3.4',
    state: 'published',
    contextPath: '/storage-api',
    tags: ['storage', 'files', 'cloud'],
    categories: ['storage', 'infrastructure']
  },
  {
    id: 'api-6',
    name: 'Machine Learning API',
    description: 'AI and machine learning services for predictive analytics and data processing.',
    version: '1.0.0',
    state: 'unpublished',
    contextPath: '/ml-api',
    tags: ['ai', 'machine-learning', 'analytics'],
    categories: ['artificial-intelligence', 'data-science']
  }
];

export const MOCK_API_SEARCH_RESPONSE: ApiSearchResponse = {
  data: MOCK_APIS,
  metadata: {
    total: MOCK_APIS.length,
    page: 1,
    size: MOCK_APIS.length
  }
};

export const getMockApiSearchResponse = (
  page: number = 1,
  category: string = 'all',
  q: string = '',
  size: number = 9
): ApiSearchResponse => {
  let filteredApis = [...MOCK_APIS];

  // Apply category filter
  if (category && category !== 'all') {
    filteredApis = filteredApis.filter(api => 
      api.categories?.includes(category)
    );
  }

  // Apply search query filter
  if (q && q.trim()) {
    const query = q.toLowerCase();
    filteredApis = filteredApis.filter(api =>
      api.name.toLowerCase().includes(query) ||
      api.description?.toLowerCase().includes(query) ||
      api.tags?.some(tag => tag.toLowerCase().includes(query))
    );
  }

  // Apply pagination
  const startIndex = (page - 1) * size;
  const endIndex = startIndex + size;
  const paginatedApis = filteredApis.slice(startIndex, endIndex);

  return {
    data: paginatedApis,
    metadata: {
      total: filteredApis.length,
      page,
      size
    }
  };
};

export const getMockApiDetails = (apiId: string): Api | null => {
  return MOCK_APIS.find(api => api.id === apiId) || null;
}; 