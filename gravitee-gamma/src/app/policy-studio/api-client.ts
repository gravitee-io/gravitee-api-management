import type { ApiV4, PolicyPlugin } from './types';

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly body: string,
  ) {
    super(`API error ${status}: ${body}`);
    this.name = 'ApiError';
  }
}

function isMockEnabled(): boolean {
  try {
    return localStorage.getItem('mocks') !== 'false';
  } catch {
    return true;
  }
}

function isChaosEnabled(): boolean {
  try {
    return localStorage.getItem('chaos') === 'true';
  } catch {
    return false;
  }
}

async function applyChaos(): Promise<void> {
  if (!isChaosEnabled()) return;

  const delay = 1000 + Math.random() * 1000;
  await new Promise((resolve) => setTimeout(resolve, delay));

  if (Math.random() < 0.1) {
    throw new ApiError(500, 'Chaos: simulated server error');
  }
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  if (isMockEnabled()) {
    return getMockResponse<T>(path, init);
  }

  await applyChaos();

  const res = await fetch(`/management${path}`, {
    credentials: 'include',
    ...init,
  });

  if (!res.ok) {
    throw new ApiError(res.status, await res.text());
  }

  return res.json();
}

// --- Mock data ---

const MOCK_POLICIES: PolicyPlugin[] = [
  {
    id: 'rate-limiting',
    name: 'Rate Limiting',
    description: 'Apply rate limiting to incoming requests',
    category: 'Security',
    flowPhaseCompatibility: { HTTP_PROXY: ['request'] },
  },
  {
    id: 'jwt',
    name: 'JWT',
    description: 'Validate and parse JWT tokens',
    category: 'Security',
    flowPhaseCompatibility: { HTTP_PROXY: ['request'] },
  },
  {
    id: 'transform-headers',
    name: 'Transform Headers',
    description: 'Add, remove, or modify request/response headers',
    category: 'Transformation',
    flowPhaseCompatibility: { HTTP_PROXY: ['request', 'response'] },
  },
  {
    id: 'json-to-xml',
    name: 'JSON to XML',
    description: 'Transform JSON payload to XML',
    category: 'Transformation',
    flowPhaseCompatibility: { HTTP_PROXY: ['response'] },
  },
  {
    id: 'ip-filtering',
    name: 'IP Filtering',
    description: 'Allow or deny access based on IP address',
    category: 'Security',
    flowPhaseCompatibility: { HTTP_PROXY: ['request'] },
  },
  {
    id: 'mock',
    name: 'Mock',
    description: 'Return a mock response without calling the backend',
    category: 'Others',
    flowPhaseCompatibility: { HTTP_PROXY: ['request'] },
  },
  {
    id: 'cache',
    name: 'Cache',
    description: 'Cache API responses',
    category: 'Performance',
    flowPhaseCompatibility: { HTTP_PROXY: ['request', 'response'] },
  },
  {
    id: 'assign-content',
    name: 'Assign Content',
    description: 'Set or modify the request/response body',
    category: 'Transformation',
    flowPhaseCompatibility: { HTTP_PROXY: ['request', 'response'] },
  },
  // Corrupted entry — missing fields (anti-happy-path)
  {
    id: 'broken-policy',
    name: '',
    description: undefined,
    category: undefined,
    flowPhaseCompatibility: undefined,
  },
];

const MOCK_API: ApiV4 = {
  id: 'mock-api-id',
  name: 'Pet Store API',
  type: 'PROXY',
  definitionVersion: 'V4',
  listeners: [{ type: 'HTTP', entrypoints: [{ type: 'http-proxy' }] }],
  flows: [
    {
      name: 'All requests',
      enabled: true,
      selectors: [{ type: 'HTTP', path: '/', pathOperator: 'STARTS_WITH', methods: ['GET', 'POST'] }],
      request: [
        {
          id: 'step-1',
          name: 'Rate Limit',
          enabled: true,
          policy: 'rate-limiting',
          configuration: { rate: { limit: 100, periodTime: 1, periodTimeUnit: 'SECONDS' } },
        },
        {
          id: 'step-2',
          name: 'JWT Validation',
          enabled: true,
          policy: 'jwt',
          configuration: { resolverParameter: 'Authorization' },
        },
      ],
      response: [
        {
          id: 'step-3',
          name: 'Add CORS Headers',
          enabled: true,
          policy: 'transform-headers',
          configuration: { addHeaders: [{ name: 'Access-Control-Allow-Origin', value: '*' }] },
        },
      ],
    },
    {
      name: 'Admin endpoints',
      enabled: true,
      selectors: [{ type: 'HTTP', path: '/admin', pathOperator: 'STARTS_WITH' }],
      request: [
        {
          id: 'step-4',
          name: 'IP Filter',
          enabled: true,
          policy: 'ip-filtering',
          configuration: { allowedIps: ['192.168.1.0/24'] },
        },
      ],
      response: [],
    },
    {
      name: 'Disabled flow',
      enabled: false,
      selectors: [{ type: 'HTTP', path: '/legacy', pathOperator: 'STARTS_WITH' }],
      request: [],
      response: [],
    },
    // Corrupted flow — missing fields (anti-happy-path)
    {
      name: undefined,
      enabled: true,
      selectors: undefined,
      request: [
        {
          id: 'step-5',
          name: undefined,
          enabled: true,
          policy: 'broken-policy',
          configuration: {},
        },
      ],
      response: undefined as unknown as [],
    },
  ],
};

const MOCK_SCHEMAS: Record<string, object> = {
  'rate-limiting': {
    type: 'object',
    properties: {
      rate: {
        type: 'object',
        properties: {
          limit: { type: 'integer', title: 'Max requests', default: 100 },
          periodTime: { type: 'integer', title: 'Period', default: 1 },
          periodTimeUnit: { type: 'string', title: 'Unit', enum: ['SECONDS', 'MINUTES', 'HOURS'] },
        },
      },
    },
  },
  jwt: {
    type: 'object',
    properties: {
      resolverParameter: { type: 'string', title: 'Resolver parameter' },
      publicKeyResolver: { type: 'string', title: 'Public key resolver', enum: ['GIVEN_KEY', 'GATEWAY_KEYS', 'JWKS_URL'] },
    },
  },
  'transform-headers': {
    type: 'object',
    properties: {
      addHeaders: {
        type: 'array',
        title: 'Headers to add',
        items: {
          type: 'object',
          properties: {
            name: { type: 'string', title: 'Name' },
            value: { type: 'string', title: 'Value' },
          },
        },
      },
    },
  },
};

async function getMockResponse<T>(path: string, init?: RequestInit): Promise<T> {
  // Simulate network delay
  await new Promise((resolve) => setTimeout(resolve, 200 + Math.random() * 300));

  await applyChaos();

  const method = init?.method?.toUpperCase() ?? 'GET';

  // GET /v2/apis/:apiId
  if (/^\/v2\/apis\/[\w-]+$/.test(path) && method === 'GET') {
    return MOCK_API as T;
  }

  // PUT /v2/apis/:apiId
  if (/^\/v2\/apis\/[\w-]+$/.test(path) && method === 'PUT') {
    return MOCK_API as T;
  }

  // GET /org/v2/plugins/policies
  if (path === '/org/v2/plugins/policies') {
    return MOCK_POLICIES as T;
  }

  // GET /org/v2/plugins/policies/:id/schema
  const schemaMatch = path.match(/^\/org\/v2\/plugins\/policies\/([\w-]+)\/schema/);
  if (schemaMatch) {
    const policyId = schemaMatch[1];
    return (MOCK_SCHEMAS[policyId] ?? { type: 'object', properties: {} }) as T;
  }

  throw new ApiError(404, `Mock: no handler for ${method} ${path}`);
}
