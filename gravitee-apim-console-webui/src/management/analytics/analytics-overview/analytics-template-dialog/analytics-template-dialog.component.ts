/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';


export interface Template {
    id: string;
    name: string;
    shortDescription: string;
    longDescription: string;
    thumbnailUrl: string;
    previewUrl: string;
}

const TEMPLATES: Template[] = [
    {
        id: 'MCP',
        name: 'Quick summary dashboard',
        shortDescription: 'Provides a high-level overview of key metrics and performance insights at a glance.',
        longDescription: "High-level overview of your organization's API traffic and performance. This dashboard highlights top services, routes, and consumers across your organization with time-based trends for key performance metrics. It surfaces critical bottlenecks in the slowest services, routes, and consumers, helping you identify areas that may need optimization.",
        thumbnailUrl: '/Users/samirbouhassoun/.gemini/antigravity/brain/13fc1f02-04e3-4f07-9987-8cbbcf988791/mcp_template_preview_1768419346018.png',
        previewUrl: '/Users/samirbouhassoun/.gemini/antigravity/brain/13fc1f02-04e3-4f07-9987-8cbbcf988791/mcp_template_preview_1768419346018.png',
    },
    {
        id: 'Proxy',
        name: 'API Proxy dashboard',
        shortDescription: 'Monitors standard API traffic, including request counts and status distribution.',
        longDescription: 'Standard API performance monitoring. Track your API health with detailed charts on response codes, throughput, and top-performing APIs. Identify potential issues before they impact your users.',
        thumbnailUrl: '/Users/samirbouhassoun/.gemini/antigravity/brain/13fc1f02-04e3-4f07-9987-8cbbcf988791/proxy_template_preview_1768419362612.png',
        previewUrl: '/Users/samirbouhassoun/.gemini/antigravity/brain/13fc1f02-04e3-4f07-9987-8cbbcf988791/proxy_template_preview_1768419362612.png',
    },
    {
        id: 'LLM',
        name: 'AI gateway dashboard',
        shortDescription: 'Monitors AI gateway performance including traffic, latency, and errors.',
        longDescription: 'Deep insights into your AI gateway traffic. Monitor token usage, cost analysis, and provider performance (OpenAI, Anthropic, etc.). Ensure your AI infrastructure is scalable and efficient.',
        thumbnailUrl: '/Users/samirbouhassoun/.gemini/antigravity/brain/13fc1f02-04e3-4f07-9987-8cbbcf988791/llm_template_preview_1768419376919.png',
        previewUrl: '/Users/samirbouhassoun/.gemini/antigravity/brain/13fc1f02-04e3-4f07-9987-8cbbcf988791/llm_template_preview_1768419376919.png',
    },
];

@Component({
    selector: 'analytics-template-dialog',
    templateUrl: './analytics-template-dialog.component.html',
    styleUrls: ['./analytics-template-dialog.component.scss'],
    standalone: false,
})
export class AnalyticsTemplateDialogComponent {
    public templates = TEMPLATES;
    public selectedTemplate: Template = TEMPLATES[0];

    constructor(public dialogRef: MatDialogRef<AnalyticsTemplateDialogComponent>) { }

    public onSelect(template: Template): void {
        this.selectedTemplate = template;
    }

    public onGenerate(): void {
        this.dialogRef.close(this.selectedTemplate.id);
    }

    public onCancel(): void {
        this.dialogRef.close();
    }
}
