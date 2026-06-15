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
import { HttpClient } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import { MatIcon } from '@angular/material/icon';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { CopyCodeComponent } from '../../../components/copy-code/copy-code.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { SubscriptionMetadataContent } from '../../../entities/subscription';
import { AiProductService } from '../../../services/ai-product.service';
import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { SubscriptionService } from '../../../services/subscription.service';
import { toTitleCase } from '../../../utils/common.utils';

/** Languages offered in the quick-start switcher. */
type SnippetLanguage = 'curl' | 'python' | 'javascript';

interface LanguageOption {
  id: SnippetLanguage;
  label: string;
}

/** One selectable LLM proxy: its name, the gateway URL to call, and the models it serves. */
interface ProxyOption {
  name: string;
  url: string;
  models: string[];
}

/** Response of the OpenAI-compatible GET {endpoint}/models route. */
interface ModelsResponse {
  data?: { id?: string }[];
  models?: { name?: string }[];
}

@Component({
  selector: 'app-ai-product-details',
  standalone: true,
  imports: [LoaderComponent, CopyCodeComponent, MatIcon, MatButton, MatButtonToggle, MatButtonToggleGroup],
  templateUrl: './ai-product-details.component.html',
  styleUrl: './ai-product-details.component.scss',
})
export default class AiProductDetailsComponent {
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly aiProductService = inject(AiProductService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly http = inject(HttpClient);
  private readonly activatedRoute = inject(ActivatedRoute);

  readonly languages: LanguageOption[] = [
    { id: 'curl', label: 'cURL' },
    { id: 'python', label: 'Python' },
    { id: 'javascript', label: 'JavaScript' },
  ];

  /** User picks: which proxy, which model, which language. Each falls back to the first option. */
  selectedProxyName = signal<string | null>(null);
  selectedModel = signal<string | null>(null);
  selectedLanguage = signal<SnippetLanguage>('curl');
  copied = signal(false);

  private readonly params = toSignal(this.activatedRoute.params, { initialValue: {} as Record<string, string> });
  readonly subscriptionId = computed(() => this.params()['subscriptionId'] ?? '');

  /** Subscription with its API keys — everything is fetched on load, no router-state dependence. */
  private readonly subscriptionResource = rxResource({
    params: () => (this.subscriptionId() ? { id: this.subscriptionId() } : undefined),
    stream: ({ params }) => this.subscriptionService.get(params.id),
  });

  /** Product metadata (name, endpoint, plan names) comes from the product subscription listing. */
  private readonly listResource = rxResource({
    stream: () => this.subscriptionService.list({ referenceType: 'API_PRODUCT', statuses: null, size: 100 }),
  });

  /** Real models + components served by this product (from the catalog endpoint) — the reliable source. */
  private readonly productResource = rxResource({
    params: () => {
      const referenceId = this.subscription()?.reference_id;
      return referenceId ? { id: referenceId } : undefined;
    },
    stream: ({ params }) => this.aiProductService.get(params.id),
  });

  isLoading = computed(() => this.subscriptionResource.isLoading() || this.listResource.isLoading());

  subscription = computed(() => this.subscriptionResource.value());

  private productMeta = computed<SubscriptionMetadataContent | undefined>(() => {
    const referenceId = this.subscription()?.reference_id;
    return referenceId ? this.listResource.value()?.metadata?.[referenceId] : undefined;
  });

  productName = computed(() => this.productMeta()?.name ?? 'AI Product');
  productDescription = computed(() => this.productMeta()?.description ?? '');
  endpoint = computed(() => this.productMeta()?.entrypoints?.[0]?.target ?? '');

  planName = computed(() => {
    const sub = this.subscription();
    return sub ? (this.listResource.value()?.metadata?.[sub.plan]?.name ?? sub.plan) : '';
  });
  status = computed(() => toTitleCase(this.subscription()?.status ?? ''));
  apiKey = computed(() => this.subscription()?.keys?.find(key => !key.revoked_at)?.key ?? '');

  /** The user's personal token budget, set by the admin when they were approved. */
  tokenLimit = computed(() => this.metadataNumber('tokenLimit'));

  /** The user's personal request rate limit (requests/minute). */
  rateLimit = computed(() => this.metadataNumber('rateLimit'));

  /** Gateway base (origin) derived from the product endpoint, used to build each proxy's URL. */
  private gatewayBase = computed(() => {
    const endpoint = this.endpoint();
    if (!endpoint) {
      return '';
    }
    try {
      return new URL(endpoint).origin;
    } catch {
      return endpoint.replace(/\/[^/]*\/?$/, '');
    }
  });

  /** Models served by the product: live from the gateway, falling back to models seeded in the description. */
  private readonly modelsResource = rxResource({
    params: () => {
      const endpoint = this.endpoint();
      const apiKey = this.apiKey();
      return endpoint && apiKey ? { endpoint, apiKey } : undefined;
    },
    stream: ({ params }) =>
      this.http.get<ModelsResponse>(`${params.endpoint}/models`, { headers: { 'X-Gravitee-Api-Key': params.apiKey } }).pipe(
        map(response => this.parseModels(response)),
        catchError(() => of<string[]>([])),
      ),
  });

  private productModels = computed<string[]>(() => {
    const productModels = this.productResource.value()?.models ?? [];
    if (productModels.length > 0) {
      return productModels;
    }
    const liveModels = this.modelsResource.value() ?? [];
    if (liveModels.length > 0) {
      return liveModels;
    }
    return this.modelsFromDescription();
  });

  /**
   * Selectable proxies. When the product exposes its components we list each LLM proxy with its own
   * endpoint + models; otherwise we synthesise a single proxy from the product endpoint so the page
   * still works (older products without component metadata).
   */
  proxyOptions = computed<ProxyOption[]>(() => {
    const base = this.gatewayBase();
    const components = this.productResource.value()?.components ?? [];
    if (components.length > 0) {
      return components.map(component => ({
        name: component.name ?? 'Proxy',
        url: `${base}${component.path ?? ''}`.replace(/\/$/, ''),
        models: component.models ?? [],
      }));
    }
    return [{ name: this.productName(), url: this.endpoint(), models: this.productModels() }];
  });

  /** The proxy the user has selected, defaulting to the first one. */
  activeProxy = computed<ProxyOption | undefined>(() => {
    const options = this.proxyOptions();
    return options.find(option => option.name === this.selectedProxyName()) ?? options[0];
  });

  /** Models available for the active proxy (its own list, or the product-wide list as a fallback). */
  activeModels = computed<string[]>(() => {
    const proxyModels = this.activeProxy()?.models ?? [];
    return proxyModels.length > 0 ? proxyModels : this.productModels();
  });

  /** The model the user has selected, defaulting to the first one of the active proxy. */
  activeModel = computed<string>(() => {
    const models = this.activeModels();
    const selected = this.selectedModel();
    return selected && models.includes(selected) ? selected : (models[0] ?? '<model>');
  });

  /** The endpoint URL the snippets target — the active proxy's URL. */
  activeEndpoint = computed<string>(() => this.activeProxy()?.url || this.endpoint() || '<gateway-endpoint>');

  /** The single snippet shown, rebuilt whenever proxy / model / language changes. */
  activeSnippet = computed<string>(() =>
    this.buildSnippet(this.activeEndpoint(), this.activeModel(), this.selectedLanguage(), this.apiKey() || '<your-api-key>'),
  );

  constructor() {
    this.breadcrumbService.set([
      { id: 'ai-products', label: $localize`:@@aiProductsBreadcrumb:AI Products`, url: '/dashboard/ai-products' },
    ]);
    this.destroyRef.onDestroy(() => this.breadcrumbService.clear());
  }

  selectProxy(name: string): void {
    this.selectedProxyName.set(name);
    // The chosen proxy may not serve the previously-selected model; reset so it falls back to the first.
    this.selectedModel.set(null);
  }

  selectModel(model: string): void {
    this.selectedModel.set(model);
  }

  selectLanguage(language: SnippetLanguage): void {
    this.selectedLanguage.set(language);
  }

  copyActiveSnippet(): void {
    navigator.clipboard?.writeText(this.activeSnippet()).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }

  private buildSnippet(endpoint: string, model: string, language: SnippetLanguage, apiKey: string): string {
    switch (language) {
      case 'python':
        return `from openai import OpenAI

client = OpenAI(
    base_url="${endpoint}",
    api_key="unused",
    default_headers={"X-Gravitee-Api-Key": "${apiKey}"},
)

response = client.chat.completions.create(
    model="${model}",
    messages=[{"role": "user", "content": "Hello!"}],
)
print(response.choices[0].message.content)`;
      case 'javascript':
        return `const response = await fetch('${endpoint}/chat/completions', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-Gravitee-Api-Key': '${apiKey}',
  },
  body: JSON.stringify({
    model: '${model}',
    messages: [{ role: 'user', content: 'Hello!' }],
  }),
});
const data = await response.json();
console.log(data.choices[0].message.content);`;
      default:
        return `curl ${endpoint}/chat/completions \\
  -H "Content-Type: application/json" \\
  -H "X-Gravitee-Api-Key: ${apiKey}" \\
  -d '{
    "model": "${model}",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'`;
    }
  }

  private metadataNumber(key: string): string | null {
    const raw = this.subscription()?.metadata?.[key];
    const value = raw ? Number(raw) : NaN;
    return Number.isFinite(value) ? value.toLocaleString() : null;
  }

  private parseModels(response: ModelsResponse): string[] {
    const fromData = (response.data ?? []).map(model => model.id).filter((id): id is string => Boolean(id));
    if (fromData.length > 0) {
      return fromData;
    }
    return (response.models ?? []).map(model => model.name).filter((name): name is string => Boolean(name));
  }

  /** Demo fallback: the admin can seed `{"models": ["gpt-4o-mini"]}` in the product description. */
  private modelsFromDescription(): string[] {
    try {
      const parsed: unknown = JSON.parse(this.productDescription());
      if (parsed && typeof parsed === 'object' && Array.isArray((parsed as { models?: unknown }).models)) {
        return ((parsed as { models: unknown[] }).models ?? []).filter((model): model is string => typeof model === 'string');
      }
    } catch {
      // description is plain text — no seeded models
    }
    return [];
  }
}
