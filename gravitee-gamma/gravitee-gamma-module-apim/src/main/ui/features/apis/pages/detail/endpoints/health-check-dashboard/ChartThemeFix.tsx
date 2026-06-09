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

/**
 * `@gravitee/graphene-charts` renders SVG axis text and gridlines with Tailwind
 * utility classes (`fill-muted-foreground`, `stroke-border`, …). The gamma host
 * loads only graphene-core's prebuilt CSS and has no Tailwind build, so those
 * chart-only utilities are absent and the SVG falls back to black — invisible in
 * dark mode. This maps the missing utilities to graphene design tokens so charts
 * theme correctly in both light and dark. React 19 dedups via `href`/`precedence`.
 */
const CHART_THEME_CSS = `
.fill-muted-foreground{fill:var(--color-muted-foreground);}
.stroke-muted-foreground{stroke:var(--color-muted-foreground);}
.stroke-border{stroke:var(--color-border);}
.stroke-border\\/20{stroke:color-mix(in oklab, var(--color-border) 20%, transparent);}
.fill-box{fill:var(--color-popover);}
`;

export function ChartThemeFix() {
    return (
        <style href="gamma-graphene-charts-shim" precedence="default">
            {CHART_THEME_CSS}
        </style>
    );
}
