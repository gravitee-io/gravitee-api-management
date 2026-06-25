import { useState, useRef, useEffect, useCallback } from 'react';
import { createReactBlockSpec } from '@blocknote/react';
import styles from './HtmlBlock.module.scss';

type Tab = 'html' | 'css' | 'preview';

function buildSrcdoc(html: string, css: string): string {
  return `<!doctype html><html><head><meta charset="utf-8"><style>*{margin:0;box-sizing:border-box}body{font-family:system-ui,-apple-system,sans-serif;padding:16px;color:#1f2937}${css}</style></head><body>${html}</body></html>`;
}

function IframePreview({ html, css }: { html: string; css: string }) {
  const iframeRef = useRef<HTMLIFrameElement>(null);

  const resize = useCallback(() => {
    const iframe = iframeRef.current;
    if (!iframe?.contentDocument?.body) return;
    iframe.style.height = iframe.contentDocument.body.scrollHeight + 'px';
  }, []);

  useEffect(() => {
    const iframe = iframeRef.current;
    if (!iframe) return;
    iframe.addEventListener('load', resize);
    return () => iframe.removeEventListener('load', resize);
  }, [resize]);

  useEffect(() => {
    resize();
  }, [html, css, resize]);

  return (
    <iframe
      ref={iframeRef}
      className={styles.iframe}
      srcDoc={buildSrcdoc(html, css)}
      sandbox="allow-same-origin"
      title="HTML Preview"
    />
  );
}

export const HtmlBlock = createReactBlockSpec(
  {
    type: 'graviteeHtml' as const,
    propSchema: {
      html: { default: '<div class="card">\n  <h2>Hello World</h2>\n  <p>Custom HTML block</p>\n</div>' },
      css: { default: '.card {\n  padding: 24px;\n  border-radius: 12px;\n  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n  color: #fff;\n}' },
    },
    content: 'none',
  },
  {
    render: ({ block, editor }) => {
      const { html, css } = block.props;
      const isEditable = editor.isEditable;
      const [activeTab, setActiveTab] = useState<Tab>('preview');

      if (!isEditable) {
        return <IframePreview html={html} css={css} />;
      }

      const tabs: { key: Tab; label: string }[] = [
        { key: 'preview', label: 'Preview' },
        { key: 'html', label: 'HTML' },
        { key: 'css', label: 'CSS' },
      ];

      return (
        <div className={styles.block} style={{ width: '100%' }}>
          <div className={styles.tabs}>
            {tabs.map((tab) => (
              <button
                key={tab.key}
                className={`${styles.tab} ${activeTab === tab.key ? styles.active : ''}`}
                onClick={() => setActiveTab(tab.key)}
                type="button"
              >
                {tab.label}
              </button>
            ))}
          </div>

          <div className={styles.body}>
            {activeTab === 'html' && (
              <textarea
                className={styles.editor}
                value={html}
                onChange={(e) => editor.updateBlock(block, { props: { html: e.target.value } })}
                spellCheck={false}
                placeholder="<div>Your HTML here...</div>"
              />
            )}
            {activeTab === 'css' && (
              <textarea
                className={styles.editor}
                value={css}
                onChange={(e) => editor.updateBlock(block, { props: { css: e.target.value } })}
                spellCheck={false}
                placeholder=".class { color: red; }"
              />
            )}
            {activeTab === 'preview' && (
              <IframePreview html={html} css={css} />
            )}
          </div>
        </div>
      );
    },
  },
);
