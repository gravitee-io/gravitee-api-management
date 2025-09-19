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
    // Pre-process the content to handle markdownContent before passing to marked.js
    const processedContent = this.preprocessMarkdownContent(content);
    
    marked.use({ renderer: this.getRenderer() });
    const result = marked(processedContent) as string;
    
    return result;
  }

  private preprocessMarkdownContent(content: string): string {
    let processedContent = content;
    
    // Process gmd-block components using a more reliable approach
    console.log('=== PROCESSING GMD-BLOCK COMPONENTS ===');
    console.log('Input content:', JSON.stringify(content));
    
    // Find all gmd-block opening tags
    const gmdBlockOpenRegex = /<gmd-block([^>]*)>/g;
    const matches: Array<{startIndex: number, endIndex: number, attributes: string}> = [];
    
    let match;
    while ((match = gmdBlockOpenRegex.exec(content)) !== null) {
      const startIndex = match.index;
      const attributes = match[1];
      
      // Find the corresponding closing tag
      const afterOpenTag = content.substring(startIndex + match[0].length);
      const closingTagIndex = afterOpenTag.indexOf('</gmd-block>');
      
      if (closingTagIndex !== -1) {
        const endIndex = startIndex + match[0].length + closingTagIndex + '</gmd-block>'.length;
        const contentBetween = afterOpenTag.substring(0, closingTagIndex);
        
        console.log('Found gmd-block:');
        console.log('  Start index:', startIndex);
        console.log('  End index:', endIndex);
        console.log('  Attributes:', JSON.stringify(attributes));
        console.log('  Content:', JSON.stringify(contentBetween));
        
        matches.push({
          startIndex,
          endIndex,
          attributes
        });
      }
    }
    
    console.log('Found gmd-block matches:', matches.length);
    
    // Process matches in reverse order to avoid index shifting
    for (let i = matches.length - 1; i >= 0; i--) {
      const blockMatch = matches[i];
      const fullMatch = content.substring(blockMatch.startIndex, blockMatch.endIndex);
      const contentStart = blockMatch.startIndex + `<gmd-block${blockMatch.attributes}>`.length;
      const contentEnd = blockMatch.endIndex - '</gmd-block>'.length;
      const blockContent = content.substring(contentStart, contentEnd);
      
      console.log('Processing gmd-block:');
      console.log('  Full match:', JSON.stringify(fullMatch));
      console.log('  Block content:', JSON.stringify(blockContent));
      
      // Clean up the markdown content
      let markdownContent = blockContent.trim();
      
      // Remove leading whitespace from each line to normalize indentation
      const lines = markdownContent.split('\n');
      markdownContent = lines.map((line: string) => {
        if (line.trim().length === 0) return line; // Keep empty lines as is
        return line.trim();
      }).join('\n');
      
      console.log('Processed gmd-block markdown content:', JSON.stringify(markdownContent));
      
      if (markdownContent) {
        // Render the markdown content
        const renderedMarkdown = marked(markdownContent) as string;
        console.log('Rendered gmd-block markdown:', renderedMarkdown);
        
        // Replace the match in the content
        const replacement = `<gmd-block${blockMatch.attributes}>${renderedMarkdown}</gmd-block>`;
        console.log('Replacement:', JSON.stringify(replacement));
        
        processedContent = processedContent.substring(0, blockMatch.startIndex) + 
                          replacement + 
                          processedContent.substring(blockMatch.endIndex);
        
        console.log('After replacement length:', processedContent.length);
      }
    }
    
    // Then, process any tag with markdownContent attribute
    const markdownContentRegex = /<([a-zA-Z][a-zA-Z0-9-]*)([^>]*markdownContent[^>]*)>([\s\S]*?)<\/\1>/gi;
    
    processedContent = processedContent.replace(markdownContentRegex, (match, tagName, attributes, content) => {
      console.log('Found markdownContent tag:', tagName);
      console.log('Full match:', match);
      console.log('Attributes:', attributes);
      console.log('Raw content:', JSON.stringify(content));
      
      // Clean up the markdown content
      let markdownContent = content.trim();
      
      // Remove leading whitespace from each line to normalize indentation
      const lines = markdownContent.split('\n');
      markdownContent = lines.map((line: string) => {
        if (line.trim().length === 0) return line; // Keep empty lines as is
        return line.trim();
      }).join('\n');
      
      console.log('Processed markdown content:', JSON.stringify(markdownContent));
      
      if (markdownContent) {
        // Render the markdown content
        const renderedMarkdown = marked(markdownContent) as string;
        console.log('Rendered markdown:', renderedMarkdown);
        
        // Return the original tag with rendered markdown content
        return `<${tagName}${attributes}>${renderedMarkdown}</${tagName}>`;
      }
      
      return match; // Return original if no content
    });
    
    return processedContent;
  }
}
