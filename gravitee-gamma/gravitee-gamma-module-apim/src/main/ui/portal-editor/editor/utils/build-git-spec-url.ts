/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import type { GitSpecSourceFields } from '../../portals/types/spec-source.types';

function normalizeRepositoryPath(pathname: string): string {
    return pathname
        .replace(/^\//, '')
        .replace(/\/$/, '')
        .replace(/\.git$/, '');
}

function stripRepositoryBrowserPath(pathname: string): string {
    const gitLabPathIndex = pathname.search(/\/-\/(tree|blob)\//);
    if (gitLabPathIndex !== -1) {
        return pathname.slice(0, gitLabPathIndex);
    }

    const browserPathIndex = pathname.search(/\/(tree|blob)\//);
    if (browserPathIndex !== -1) {
        return pathname.slice(0, browserPathIndex);
    }

    return pathname;
}

function parseGitHubRepository(repositoryUrl: string): { owner: string; repository: string } | null {
    try {
        const parsed = new URL(repositoryUrl.trim());
        if (parsed.hostname !== 'github.com' && parsed.hostname !== 'www.github.com') {
            return null;
        }

        const parts = stripRepositoryBrowserPath(normalizeRepositoryPath(parsed.pathname)).split('/').filter(Boolean);
        if (parts.length < 2) {
            return null;
        }

        return { owner: parts[0], repository: parts[1] };
    } catch {
        return null;
    }
}

function parseGitLabRepository(repositoryUrl: string): { host: string; projectPath: string } | null {
    try {
        const parsed = new URL(repositoryUrl.trim());
        const projectPath = stripRepositoryBrowserPath(normalizeRepositoryPath(parsed.pathname));
        if (!projectPath) {
            return null;
        }

        return { host: parsed.host, projectPath };
    } catch {
        return null;
    }
}

export function buildGitSpecUrl(
    type: 'GITHUB' | 'GITLAB',
    { repositoryUrl, branch, filepath }: GitSpecSourceFields,
): string {
    const trimmedBranch = branch.trim();
    const trimmedPath = filepath.trim();

    if (type === 'GITHUB') {
        const repository = parseGitHubRepository(repositoryUrl);
        if (!repository) {
            return '';
        }

        return `https://raw.githubusercontent.com/${repository.owner}/${repository.repository}/${trimmedBranch}/${trimmedPath}`;
    }

    const repository = parseGitLabRepository(repositoryUrl);
    if (!repository) {
        return '';
    }

    return `https://${repository.host}/${repository.projectPath}/-/raw/${trimmedBranch}/${trimmedPath}`;
}
