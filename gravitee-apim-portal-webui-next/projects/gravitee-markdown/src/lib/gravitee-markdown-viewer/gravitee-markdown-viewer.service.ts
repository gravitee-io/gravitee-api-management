/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Injectable } from '@angular/core';
import hljs from 'highlight.js';
import { marked, Renderer, RendererObject } from 'marked';
import { gfmHeadingId } from 'marked-gfm-heading-id';
import { markedHighlight } from 'marked-highlight';

const ANCHOR_CLASSNAME = 'anchor';

@Injectable()
export class GraviteeMarkdownViewerService {
  constructor() {
    marked.use(gfmHeadingId());
    marked.use(
      markedHighlight({
        langPrefix: 'hljs language-',
        highlight(_code, language) {
          const validLanguage = hljs.getLanguage(language) ? language : 'plaintext';
          return hljs.highlight(validLanguage, { language: validLanguage }).value;
        },
      }),
    );
    marked.setOptions({
      breaks: true,
      gfm: true,
    });
  }

  public getRenderer(): RendererObject {
    const defaultRenderer = new Renderer();
    
    return {
      image(href, title, text) {
        return defaultRenderer.image(href, title, text);
      },
      link(href, title, text) {
        if (href.startsWith('#')) {
          return `<a class="${ANCHOR_CLASSNAME}" href="${href}">${text}</a>`;
        }

        if (href?.startsWith('/#!/')) {
          const trimmedHref = href.substring(3);
          return defaultRenderer.link(trimmedHref, title, text);
        }

        return defaultRenderer.link(href, title, text);
      },
      html: (html: string, block?: boolean): string => {
        // Since we preprocess the content, the HTML renderer just needs to handle normal HTML
        return defaultRenderer.html(html, block);
      }
    };
  }


  public render(content: string): string {
    console.log('=== RENDER METHOD CALLED ===');
    console.log('Original content length:', content.length);
    console.log('Original content:', content);
    console.log('Content ends with:', content.slice(-100));
    
    // Pre-process the content to handle markdownContent before passing to marked.js
    const processedContent = this.preprocessMarkdownContent(content);
    console.log('Processed content:', processedContent);
    
    marked.use({ renderer: this.getRenderer() });
    const result = marked(processedContent) as string;
    
    console.log('=== RENDER RESULT ===');
    console.log('Result length:', result.length);
    console.log('Result:', result);
    
    return result;
  }

  private preprocessMarkdownContent(content: string): string {
    console.log('=== PREPROCESSING MARKDOWN CONTENT ===');
    
    // Use regex to find and replace content between tags with markdownContent attribute
    const markdownContentRegex = /<(\w+)([^>]*markdownContent[^>]*)>([\s\S]*?)<\/\1>/gi;
    
    const processedContent = content.replace(markdownContentRegex, (match, tagName, attributes, content) => {
      console.log('Found markdownContent tag:', tagName);
      console.log('Full match:', match);
      console.log('Raw content:', JSON.stringify(content));
      
      // Clean up the markdown content by removing extra whitespace and normalizing indentation
      let markdownContent: string = content.trim();
      
      console.log('Before processing:', JSON.stringify(markdownContent));
      
      // Remove leading whitespace from each line to normalize indentation
      const lines = markdownContent.split('\n');
      markdownContent = lines.map(line => {
        if (line.trim().length === 0) return line; // Keep empty lines as is
        const trimmed = line.trim();
        console.log('Original line:', JSON.stringify(line), 'Trimmed:', JSON.stringify(trimmed));
        return trimmed;
      }).join('\n');
      
      console.log(`Found markdown content in <${tagName}>:`, JSON.stringify(markdownContent));
      
      if (markdownContent) {
        // Render the markdown content
        const renderedMarkdown = marked(markdownContent) as string;
        console.log('Rendered markdown:', renderedMarkdown);
        
        // Clean up any unwanted code blocks that might have been inserted
        const cleanedMarkdown = renderedMarkdown.replace(/<pre><code>plaintext\s*<\/code><\/pre>/g, '');
        console.log('Cleaned markdown:', cleanedMarkdown);
        
        // Return the original tag with rendered markdown content
        return `<${tagName}${attributes}>${cleanedMarkdown}</${tagName}>`;
      }
      
      return match; // Return original if no content
    });
    
    console.log('Preprocessing complete. Original length:', content.length, 'Processed length:', processedContent.length);
    return processedContent;
  }
}
