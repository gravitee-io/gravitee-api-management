/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gamma.rest.infra.adapter;

/**
 * The currency the platform prices LLM usage in, served with every built-in cost. The rule: AIM's bundled price lists
 * price every model in USD and a custom rate may not diverge from the registry's currency
 * ({@code PricingOverrides.resolveCurrency}, gravitee-gamma-module-aim); budgets are stored in micro-dollars
 * ({@code AiWorkspaceBudgetReader}); the Portal API documents budgets in US dollars ({@code portal-openapi.yaml}).
 *
 * <p>The gap: a model added by hand with no published rate may be priced in EUR or GBP, and the cost documents carry a
 * bare amount either way, so such costs are served as USD. This constant gives way to a per-document currency once
 * stored costs carry their own (OBS-188).
 */
final class PlatformCurrency {

    static final String CODE = "USD";

    private PlatformCurrency() {}
}
