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
import { FetcherListItem } from './fetcher';

export function fakeFetcherListItem(attributes?: Partial<FetcherListItem>): FetcherListItem {
  const base: FetcherListItem = {
    id: 'id',
    name: 'name',
    description: 'description',
    version: 'version',
    schema:
      '{"type": "object","title": "http","properties": {"url": {"title": "URL","description": "Url to the file you want to fetch","type": "string"}}}',
  };

  return {
    ...base,
    ...attributes,
  };
}

export function fakeFetcherList(): FetcherListItem[] {
  return [
    {
      id: 'bitbucket-fetcher',
      name: 'Bitbucket',
      description: 'The Gravitee.IO Parent POM provides common settings for all Gravitee components.',
      version: '2.0.1',
      schema:
        '{\n    "type": "object",\n    "title": "bitbucket",\n    "properties": {\n        "bitbucketUrl": {\n            "title": "Bitbucket api url",\n            "description": "Bitbucket API url (e.g. https://api.bitbucket.org/2.0)",\n            "type": "string",\n            "default": "https://api.bitbucket.org/2.0"\n        },\n        "useSystemProxy": {\n            "title": "Use system proxy",\n            "description": "Use the system proxy configured by your administrator.",\n            "type": "boolean"\n        },\n        "username": {\n            "title": "Username",\n            "description": "Username or organization",\n            "type": "string"\n        },\n        "repository": {\n            "title": "Repository",\n            "description": "Repository name",\n            "type": "string"\n        },\n        "branchOrTag": {\n            "title": "Ref",\n            "description": "Branch name or tag (e.g. master)",\n            "type": "string",\n            "default": "master"\n        },\n        "filepath": {\n            "title": "Filepath",\n            "description": "The path to the file to fetch (e.g. /docs/main/README.md)",\n            "type": "string"\n        },\n        "login": {\n            "title": "Login",\n            "description": "the user login used in basic authentication. See https://developer.atlassian.com/bitbucket/api/2/reference/meta/authentication#basic-auth",\n            "type": "string"\n        },\n        "password": {\n            "title": "Password",\n            "description": "Use an application password. See https://developer.atlassian.com/bitbucket/api/2/reference/meta/authentication#app-pw",\n            "type": "string",\n            "x-schema-form": {\n                "type": "password"\n            }\n        },\n        "autoFetch": {\n            "title": "Auto Fetch",\n            "description": "Trigger periodic update",\n            "type": "boolean",\n            "default": false\n        },\n        "fetchCron": {\n            "title": "Update frequency",\n            "description": "Define update frequency using Crontab pattern.<BR><B>Note:</B> Platform administrator may have configure a max frequency that you cannot exceed",\n            "type": "string"\n        }\n    },\n    "required": ["bitbucketUrl", "username", "repository", "branchOrTag", "filepath", "login", "password"],\n    "if": {\n        "properties": {\n            "autoFetch": { "const": true }\n        }\n    },\n    "then": { "required": ["fetchCron"] }\n}\n',
    },
    {
      id: 'git-fetcher',
      name: 'GIT',
      description: 'The Gravitee.IO Parent POM provides common settings for all Gravitee components.',
      version: '2.0.0',
      schema:
        '{\n    "type": "object",\n    "title": "git",\n    "properties": {\n        "repository": {\n            "title": "Repository",\n            "description": "Web URL to the git repository",\n            "type": "string"\n        },\n        "branchOrTag": {\n            "title": "Branch or Tag",\n            "description": "Branch or tag to clone",\n            "type": "string",\n            "default": "master"\n        },\n        "path": {\n            "title": "Path",\n            "description": "Path to the file to fetch",\n            "type": "string"\n        },\n        "autoFetch": {\n            "title": "Auto Fetch",\n            "description": "Trigger periodic update",\n            "type": "boolean",\n            "default": false\n        },\n        "fetchCron": {\n            "title": "Update frequency",\n            "description": "Define update frequency using Crontab pattern.<BR><B>Note:</B> Platform administrator may have configure a max frequency that you cannot exceed",\n            "type": "string"\n        }\n    },\n    "required": ["repository", "branchOrTag", "path"],\n    "if": {\n        "properties": {\n            "autoFetch": { "const": true }\n        }\n    },\n    "then": { "required": ["fetchCron"] }\n}\n',
    },
    {
      id: 'github-fetcher',
      name: 'GitHub',
      description: 'The Gravitee.IO Parent POM provides common settings for all Gravitee components.',
      version: '2.0.1',
      schema:
        '{\n    "type": "object",\n    "title": "github",\n    "properties": {\n        "githubUrl": {\n            "title": "GitHub URL",\n            "type": "string",\n            "default": "https://api.github.com"\n        },\n        "useSystemProxy": {\n            "title": "Use system proxy",\n            "description": "Use the system proxy configured by your administrator.",\n            "type": "boolean"\n        },\n        "owner": {\n            "title": "Repository Owner",\n            "description": "Owner of the repository",\n            "type": "string"\n        },\n        "repository": {\n            "title": "Repository",\n            "type": "string"\n        },\n        "branchOrTag": {\n            "title": "Ref",\n            "description": "Branch name, tag or sha1 (e.g. master). If empty, we use the default branch.",\n            "type": "string"\n        },\n        "filepath": {\n            "title": "Filepath",\n            "description": "The path to the file to fetch (e.g. /docs/main/README.md)",\n            "type": "string"\n        },\n        "username": {\n            "title": "Username",\n            "description": "Username used to authenticate the request",\n            "type": "string"\n        },\n        "personalAccessToken": {\n            "title": "Personal Access Token",\n            "description": "Create your personal access token here: https://github.com/settings/tokens",\n            "type": "string"\n        },\n        "autoFetch": {\n            "title": "Auto Fetch",\n            "description": "Trigger periodic update",\n            "type": "boolean",\n            "default": false\n        },\n        "fetchCron": {\n            "title": "Update frequency",\n            "description": "Define update frequency using Crontab pattern.<BR><B>Note:</B> Platform administrator may have configure a max frequency that you cannot exceed",\n            "type": "string"\n        }\n    },\n    "required": ["githubUrl", "owner", "repository"],\n    "if": {\n        "properties": {\n            "autoFetch": { "const": true }\n        }\n    },\n    "then": { "required": ["fetchCron"] }\n}\n',
    },
    {
      id: 'gitlab-fetcher',
      name: 'Gitlab',
      description: 'The Gravitee.IO Parent POM provides common settings for all Gravitee components.',
      version: '2.0.1',
      schema:
        '{\n    "type": "object",\n    "title": "gitlab",\n    "properties": {\n        "apiVersion": {\n            "title": "Gitlab API Version",\n            "description": "The api version to use.",\n            "type": "string",\n            "default": "V4",\n            "enum": ["V3", "V4"]\n        },\n        "gitlabUrl": {\n            "title": "GitLab api url",\n            "description": "GitLab API url (e.g. https://gitlab.com/api/v3)",\n            "type": "string",\n            "default": "https://gitlab.com/api/v4"\n        },\n        "useSystemProxy": {\n            "title": "Use system proxy",\n            "description": "Use the system proxy configured by your administrator.",\n            "type": "boolean"\n        },\n        "namespace": {\n            "title": "Namespace",\n            "description": "Username and groupname",\n            "type": "string"\n        },\n        "project": {\n            "title": "Project",\n            "description": "Project name",\n            "type": "string"\n        },\n        "branchOrTag": {\n            "title": "Ref",\n            "description": "Branch name, tag or sha1 (e.g. master)",\n            "type": "string",\n            "default": "master"\n        },\n        "filepath": {\n            "title": "Filepath",\n            "description": "The path to the file to fetch (e.g. /docs/main/README.md)",\n            "type": "string"\n        },\n        "privateToken": {\n            "title": "Private or Personal Token",\n            "description": "See https://docs.gitlab.com/ce/api/#authentication",\n            "type": "string"\n        },\n        "autoFetch": {\n            "title": "Auto Fetch",\n            "description": "Trigger periodic update",\n            "type": "boolean",\n            "default": false\n        },\n        "fetchCron": {\n            "title": "Update frequency",\n            "description": "Define update frequency using Crontab pattern.<BR><B>Note:</B> Platform administrator may have configure a max frequency that you cannot exceed",\n            "type": "string"\n        }\n    },\n    "required": ["gitlabUrl", "namespace", "project", "branchOrTag", "privateToken"],\n    "if": {\n        "properties": {\n            "autoFetch": { "const": true }\n        }\n    },\n    "then": { "required": ["fetchCron"] }\n}\n',
    },
    {
      id: 'http-fetcher',
      name: 'HTTP',
      description: 'The Gravitee.IO Parent POM provides common settings for all Gravitee components.',
      version: '2.0.2',
      schema:
        '{\n    "type": "object",\n    "title": "http",\n    "properties": {\n        "url": {\n            "title": "URL",\n            "description": "Url to the file you want to fetch",\n            "type": "string"\n        },\n        "useSystemProxy": {\n            "title": "Use system proxy",\n            "description": "Use the system proxy configured by your administrator.",\n            "type": "boolean"\n        },\n        "autoFetch": {\n            "title": "Auto Fetch",\n            "description": "Trigger periodic update",\n            "type": "boolean",\n            "default": false\n        },\n        "fetchCron": {\n            "title": "Update frequency",\n            "description": "Define update frequency using Crontab pattern. Note: Platform administrator may have configure a max frequency that you cannot exceed.",\n            "type": "string"\n        }\n    },\n    "required": ["url"],\n    "if": {\n        "properties": {\n            "autoFetch": { "const": true }\n        }\n    },\n    "then": { "required": ["fetchCron"] }\n}\n',
    },
  ];
}
