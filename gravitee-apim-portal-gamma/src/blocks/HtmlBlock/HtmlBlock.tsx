import { CodeEditor } from '@gravitee/graphene-core/code-editor';
import { type MouseEvent, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { createReactBlockSpec } from '@blocknote/react';
import { getGmdBlockHooks } from '../../features/editor/gmd/gmd-block-hooks';
import { registerGraviteeComponentCompletions } from '../../features/html/html-component-completion';
import { HtmlSlotHydrator } from '../../features/html/hydrate-slots';
import { sanitizePortalHtml } from '../../features/html/sanitize-html';
import { scopeCustomCss } from '../../features/html/scope-custom-css';
import styles from './HtmlBlock.module.scss';

type Tab = 'html' | 'css' | 'preview';

function HtmlSourceEditor({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <CodeEditor
      language="html"
      value={value}
      onChange={next => onChange(next ?? '')}
      className={styles.monacoEditor}
      height={300}
      onMount={(editor, monaco) => {
        const disposable = registerGraviteeComponentCompletions(monaco);
        editor.onDidDispose(() => disposable.dispose());
      }}
    />
  );
}

function stopBlockNoteTableHandling(event: MouseEvent) {
  // BlockNote's TableHandles listens on the editor root for mouse events and
  // misidentifies native <table> markup inside custom HTML blocks (e.g.
  // subscription viewer) as BlockNote table blocks, then crashes on content.rows.
  event.stopPropagation();
}

function HtmlBlockView({
  html,
  css,
  blockId,
  shouldHydrateSlots,
}: {
  html: string;
  css: string;
  blockId: string;
  shouldHydrateSlots: boolean;
}) {
  const htmlContentRef = useRef<HTMLDivElement>(null);
  const scopeId = `block-${blockId}`;
  const scopedCss = useMemo(() => scopeCustomCss(css, `[data-block-scope="${scopeId}"]`), [css, scopeId]);
  const sanitizedHtml = useMemo(() => sanitizePortalHtml(html), [html]);

  useLayoutEffect(() => {
    if (htmlContentRef.current) {
      htmlContentRef.current.innerHTML = sanitizedHtml;
    }
  }, [sanitizedHtml]);

  return (
    <div
      className={styles.htmlBlock}
      data-block-scope={scopeId}
      data-style-target="html-block"
      onMouseDown={stopBlockNoteTableHandling}
      onMouseMove={stopBlockNoteTableHandling}
      onMouseUp={stopBlockNoteTableHandling}
    >
      <style>{scopedCss}</style>
      <div ref={htmlContentRef} />
      {shouldHydrateSlots ? (
        <HtmlSlotHydrator
          containerRef={htmlContentRef}
          enabled={shouldHydrateSlots}
          htmlRevision={sanitizedHtml}
        />
      ) : null}
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
        return <HtmlBlockView html={html} css={css} blockId={block.id} shouldHydrateSlots />;
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
              <HtmlSourceEditor
                value={html}
                onChange={nextHtml => editor.updateBlock(block, { props: { html: nextHtml } })}
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
              <HtmlBlockView html={html} css={css} blockId={block.id} shouldHydrateSlots />
            )}
          </div>
        </div>
      );
    },
  },
);
