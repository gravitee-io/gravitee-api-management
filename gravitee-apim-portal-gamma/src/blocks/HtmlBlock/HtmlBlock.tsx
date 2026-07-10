import { useEffect, useMemo, useRef, useState } from 'react';
import { createReactBlockSpec } from '@blocknote/react';
import { getGmdBlockHooks } from '../../features/editor/gmd/gmd-block-hooks';
import { hydrateSlots } from '../../features/html/hydrate-slots';
import { sanitizePortalHtml } from '../../features/html/sanitize-html';
import { scopeCustomCss } from '../../features/html/scope-custom-css';
import styles from './HtmlBlock.module.scss';

type Tab = 'html' | 'css' | 'preview';

function HtmlBlockView({ html, css, blockId, isEditable }: { html: string; css: string; blockId: string; isEditable: boolean }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const scopeId = `block-${blockId}`;
  const scopedCss = useMemo(() => scopeCustomCss(css, `[data-block-scope="${scopeId}"]`), [css, scopeId]);
  const sanitizedHtml = useMemo(() => sanitizePortalHtml(html), [html]);

  useEffect(() => {
    if (!containerRef.current || isEditable) return;
    return hydrateSlots(containerRef.current);
  }, [sanitizedHtml, isEditable]);

  return (
    <div
      ref={containerRef}
      className={styles.htmlBlock}
      data-block-scope={scopeId}
      data-style-target="html-block"
    >
      <style>{scopedCss}</style>
      <div dangerouslySetInnerHTML={{ __html: sanitizedHtml }} />
    </div>
  );
}

export const HtmlBlock = createReactBlockSpec(
  {
    type: 'graviteeHtml' as const,
    propSchema: {
      html: { default: '<div class="card">\n  <h2>Hello World</h2>\n  <p>Custom HTML block</p>\n  <div data-gravitee-component="api-catalog"></div>\n</div>' },
      css: { default: '.card {\n  padding: 24px;\n  border-radius: 12px;\n  background: var(--portal-color-surface);\n}' },
    },
    content: 'none',
  },
  {
    ...getGmdBlockHooks('graviteeHtml'),
    render: ({ block, editor }) => {
      const { html, css } = block.props;
      const isEditable = editor.isEditable;
      const [activeTab, setActiveTab] = useState<Tab>('preview');

      if (!isEditable) {
        return <HtmlBlockView html={html} css={css} blockId={block.id} isEditable={false} />;
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
              <HtmlBlockView html={html} css={css} blockId={block.id} isEditable />
            )}
          </div>
        </div>
      );
    },
  },
);
