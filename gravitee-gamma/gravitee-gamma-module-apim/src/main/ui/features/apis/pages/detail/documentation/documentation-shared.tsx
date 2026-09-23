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
import {
    ArrowRightIcon,
    BitbucketIcon,
    BookOpenIcon,
    CircleCheckIcon,
    DownloadIcon,
    FileTextIcon,
    FolderOpenIcon,
    GitBranchIcon,
    GithubIcon,
    GitlabIcon,
    GlobeIcon,
    PencilIcon,
    type LucideIcon,
} from '@gravitee/graphene-core/icons';
import type { ComponentType } from 'react';

import { FeatureTile } from '../../../../../shared/components';
import type { PageSourceType, PageType } from '../../../types/documentation';
import { SUPPORTED_FOR_EDIT } from '../../../types/documentation';
import { pageTypeTitle } from '../../../utils/documentationFormatters';
import asyncApiLogo from './logos/logo_asyncapi.svg';
import markdownLogo from './logos/logo_markdown.svg';
import openApiLogo from './logos/logo_openapi.svg';

export const PAGE_TYPES = SUPPORTED_FOR_EDIT;

type PageTypeIcon = ComponentType<{ className?: string }>;

export const PAGE_TYPE_ICON: Record<(typeof PAGE_TYPES)[number], PageTypeIcon> = {
    MARKDOWN: markdownLogo as unknown as PageTypeIcon,
    SWAGGER: openApiLogo as unknown as PageTypeIcon,
    ASYNCAPI: asyncApiLogo as unknown as PageTypeIcon,
};

export const FETCHER_ICONS: Record<string, LucideIcon> = {
    'bitbucket-fetcher': BitbucketIcon,
    'git-fetcher': GitBranchIcon,
    'github-fetcher': GithubIcon,
    'gitlab-fetcher': GitlabIcon,
    'http-fetcher': GlobeIcon,
};

export const SOURCE_OPTIONS: {
    value: PageSourceType;
    title: string;
    description: string;
    Icon: LucideIcon;
}[] = [
    {
        value: 'FILL',
        title: 'Fill in the content myself',
        description: 'Author Markdown or a spec in the editor.',
        Icon: PencilIcon,
    },
    {
        value: 'IMPORT',
        title: 'Import from file',
        description: 'Upload a local .md, .yaml, or .json file.',
        Icon: DownloadIcon,
    },
    {
        value: 'EXTERNAL',
        title: 'Link to External Source',
        description: 'Fetch content from Bitbucket, Git, GitHub, GitLab, or HTTP.',
        Icon: GitBranchIcon,
    },
];

export const NAME_MAX = 64;
export const EDITOR_HEIGHT = 480;

export function typeLabel(type: PageType | undefined): string {
    return pageTypeTitle(type);
}

export function editorLanguage(type: PageType | undefined, content: string): string {
    if (type === 'MARKDOWN') return 'markdown';
    return content.trim().startsWith('{') ? 'json' : 'yaml';
}

export function escapeHtml(value: string): string {
    return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function inlineMarkdown(value: string): string {
    return value
        .replace(/`([^`]+)`/g, '<code class="bg-muted rounded px-1 font-mono text-xs">$1</code>')
        .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
        .replace(
            /\[([^\]]+)\]\((https?:[^)]+)\)/g,
            '<a href="$2" class="text-primary underline" target="_blank" rel="noreferrer">$1</a>',
        );
}

function formatMarkdownBlock(block: string): string {
    const trimmed = block.trim();
    if (!trimmed) return '';
    if (trimmed.startsWith('### ')) return `<h3 class="mt-3 text-base font-semibold">${inlineMarkdown(trimmed.slice(4))}</h3>`;
    if (trimmed.startsWith('## ')) return `<h2 class="mt-4 text-lg font-semibold">${inlineMarkdown(trimmed.slice(3))}</h2>`;
    if (trimmed.startsWith('# ')) return `<h1 class="text-xl font-semibold">${inlineMarkdown(trimmed.slice(2))}</h1>`;
    if (trimmed === '---') return '<hr class="border-border my-3" />';
    const lines = trimmed.split('\n');
    if (lines.every(line => line.startsWith('- ') || line.startsWith('* '))) {
        const items = lines.map(line => `<li>${inlineMarkdown(line.slice(2))}</li>`).join('');
        return `<ul class="my-2 list-disc space-y-1 pl-5">${items}</ul>`;
    }
    return `<p class="my-2 leading-relaxed">${inlineMarkdown(lines.join('<br />'))}</p>`;
}

export function markdownToHtml(markdown: string): string {
    const escaped = escapeHtml(markdown);
    const chunks: { type: 'code' | 'text'; value: string }[] = [];
    const fence = /```[^\n]*\n([\s\S]*?)```/g;
    let last = 0;
    let match = fence.exec(escaped);
    while (match) {
        if (match.index > last) chunks.push({ type: 'text', value: escaped.slice(last, match.index) });
        chunks.push({ type: 'code', value: match[1] ?? '' });
        last = match.index + match[0].length;
        match = fence.exec(escaped);
    }
    if (last < escaped.length) chunks.push({ type: 'text', value: escaped.slice(last) });
    if (chunks.length === 0) chunks.push({ type: 'text', value: escaped });
    return chunks
        .map(chunk => {
            if (chunk.type === 'code') {
                return `<pre class="bg-muted overflow-x-auto rounded-md p-3 font-mono text-xs"><code>${chunk.value}</code></pre>`;
            }
            return chunk.value
                .split(/\n{2,}/)
                .map(block => formatMarkdownBlock(block))
                .join('');
        })
        .join('');
}

export function fileAccept(type: PageType | undefined): string {
    if (type === 'MARKDOWN') return '.md,.markdown,.txt';
    return '.yaml,.yml,.json';
}

export function TypeIcon({ type, className }: { type: PageType | undefined; className?: string }) {
    if (type === 'FOLDER' || !type || !(type in PAGE_TYPE_ICON)) {
        return <FolderOpenIcon className={className} aria-hidden />;
    }
    const Icon = PAGE_TYPE_ICON[type as (typeof PAGE_TYPES)[number]] as PageTypeIcon | string;
    if (typeof Icon === 'string') {
        return <img src={Icon} alt="" className={className} aria-hidden />;
    }
    return <Icon className={className} aria-hidden />;
}

function FlowNode({ Icon, label }: { Icon: LucideIcon; label: string }) {
    return (
        <div className="flex flex-col items-center text-center">
            <div className="rounded-lg bg-muted p-2">
                <Icon className="size-4 text-muted-foreground" aria-hidden />
            </div>
            <p className="mt-1 text-xs font-medium">{label}</p>
        </div>
    );
}

function ComparisonLine({ label, variant }: { label: string; variant: 'positive' | 'negative' }) {
    return (
        <li className="flex items-center gap-1 text-xs text-muted-foreground">
            {variant === 'positive' ? (
                <CircleCheckIcon className="size-3 shrink-0 text-success" aria-hidden />
            ) : (
                <span className="size-1.5 shrink-0 rounded-full bg-muted-foreground/40" aria-hidden />
            )}
            {label}
        </li>
    );
}

export function DocumentationEmptyLanding() {
    return (
        <div className="rounded-xl border">
            <div className="space-y-6 p-6">
                <div>
                    <h2 className="text-base font-semibold">Why add documentation?</h2>
                    <p className="mt-1 text-xs text-muted-foreground">
                        Author folders and pages on this API, then publish them into a folder on Next Gen Portal Settings →
                        Navigation. Creating a page here does not publish it.
                    </p>
                </div>

                <div className="flex flex-row items-stretch gap-4">
                    <div className="flex-1 space-y-3 rounded-xl border p-4">
                        <p className="text-xs font-semibold text-muted-foreground">Without API documentation</p>
                        <div className="flex items-center justify-center gap-2">
                            <FlowNode Icon={FileTextIcon} label="API" />
                            <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                            <FlowNode Icon={BookOpenIcon} label="Portal" />
                        </div>
                        <ul className="space-y-1">
                            <ComparisonLine label="Consumers only see what the designer writes in Portal settings" variant="negative" />
                            <ComparisonLine label="Guides live outside the API they describe" variant="negative" />
                            <ComparisonLine label="OpenAPI and AsyncAPI specs are easy to lose track of" variant="negative" />
                        </ul>
                    </div>
                    <div className="flex shrink-0 items-center justify-center">
                        <ArrowRightIcon className="size-5 text-primary" aria-hidden />
                    </div>
                    <div className="flex-1 space-y-3 rounded-xl border border-primary/20 bg-primary/5 p-4" style={{ borderWidth: 2 }}>
                        <p className="text-xs font-semibold text-primary">With API documentation</p>
                        <div className="flex items-center justify-center gap-2">
                            <FlowNode Icon={FolderOpenIcon} label="Folders" />
                            <ArrowRightIcon className="size-4 shrink-0 text-primary/70" aria-hidden />
                            <FlowNode Icon={FileTextIcon} label="Pages" />
                            <ArrowRightIcon className="size-4 shrink-0 text-primary/70" aria-hidden />
                            <FlowNode Icon={BookOpenIcon} label="Ready for portal" />
                        </div>
                        <ul className="space-y-1">
                            <ComparisonLine label="Markdown, OpenAPI, and AsyncAPI on the API itself" variant="positive" />
                            <ComparisonLine label="Folders keep guides and specs organized" variant="positive" />
                            <ComparisonLine label="Publish a folder or root page into a Navigation folder" variant="positive" />
                        </ul>
                    </div>
                </div>

                <div className="flex flex-row gap-4 border-t pt-5">
                    <div className="flex-1">
                        <FeatureTile
                            Icon={FolderOpenIcon}
                            title="Folders"
                            description="Group related pages the same way Classic Documentation Pages uses a tree."
                        />
                    </div>
                    <div className="flex-1">
                        <FeatureTile
                            Icon={FileTextIcon}
                            title="Page types"
                            description="Markdown for guides, OpenAPI for HTTP contracts, AsyncAPI for events."
                        />
                    </div>
                    <div className="flex-1">
                        <FeatureTile
                            Icon={BookOpenIcon}
                            title="Publish when ready"
                            description="Select a Navigation folder to publish this API and its documentation on the Next Gen Portal."
                        />
                    </div>
                </div>
            </div>
        </div>
    );
}
