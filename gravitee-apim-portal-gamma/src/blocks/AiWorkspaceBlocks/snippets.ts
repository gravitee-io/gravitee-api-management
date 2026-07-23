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
export type SnippetLanguage = 'curl' | 'python' | 'javascript' | 'java' | 'go';

export const SNIPPET_LANGUAGE_LABELS: Record<SnippetLanguage, string> = {
    curl: 'cURL',
    python: 'Python',
    javascript: 'JavaScript',
    java: 'Java',
    go: 'Go',
};

export const SNIPPET_LANGUAGE_ORDER: SnippetLanguage[] = ['curl', 'python', 'javascript', 'java', 'go'];

export function parseLanguages(raw: string | undefined): SnippetLanguage[] {
    if (!raw) {
        return SNIPPET_LANGUAGE_ORDER;
    }
    const requested = raw
        .split(',')
        .map(entry => entry.trim().toLowerCase())
        .filter((entry): entry is SnippetLanguage => (SNIPPET_LANGUAGE_ORDER as string[]).includes(entry));
    return requested.length > 0 ? SNIPPET_LANGUAGE_ORDER.filter(lang => requested.includes(lang)) : SNIPPET_LANGUAGE_ORDER;
}

interface SnippetContext {
    readonly endpoint: string;
    readonly key: string;
    readonly model: string;
}

export function buildSnippet(language: SnippetLanguage, ctx: SnippetContext): string {
    const { endpoint, key, model } = ctx;

    switch (language) {
        case 'curl':
            return `curl ${endpoint}/chat/completions \\
  -H "Authorization: Bearer ${key}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "model": "${model}",
    "messages": [
      { "role": "user", "content": "Hello from Gravitee AI!" }
    ]
  }'`;

        case 'python':
            return `from openai import OpenAI

client = OpenAI(
    base_url="${endpoint}",
    api_key="${key}",
)

response = client.chat.completions.create(
    model="${model}",
    messages=[{"role": "user", "content": "Hello from Gravitee AI!"}],
)

print(response.choices[0].message.content)`;

        case 'javascript':
            return `import OpenAI from "openai";

const client = new OpenAI({
  baseURL: "${endpoint}",
  apiKey: "${key}",
});

const response = await client.chat.completions.create({
  model: "${model}",
  messages: [{ role: "user", content: "Hello from Gravitee AI!" }],
});

console.log(response.choices[0].message.content);`;

        case 'java':
            return `OpenAIClient client = OpenAIOkHttpClient.builder()
    .baseUrl("${endpoint}")
    .apiKey("${key}")
    .build();

ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
    .model("${model}")
    .addUserMessage("Hello from Gravitee AI!")
    .build();

ChatCompletion completion = client.chat().completions().create(params);
System.out.println(completion.choices().get(0).message().content());`;

        case 'go':
            return `client := openai.NewClient(
    option.WithBaseURL("${endpoint}"),
    option.WithAPIKey("${key}"),
)

completion, err := client.Chat.Completions.New(context.TODO(), openai.ChatCompletionNewParams{
    Model: openai.F("${model}"),
    Messages: openai.F([]openai.ChatCompletionMessageParamUnion{
        openai.UserMessage("Hello from Gravitee AI!"),
    }),
})
if err != nil {
    panic(err)
}
fmt.Println(completion.Choices[0].Message.Content)`;

        default:
            return '';
    }
}
