import { createReactBlockSpec } from '@blocknote/react';
import { getGmdBlockHooks } from '../../editor/gmd/gmd-block-hooks';
import { DEFAULT_HTML_BLOCK_CSS, DEFAULT_HTML_BLOCK_HTML } from '../../html/default-html-content';
import { HtmlContentView } from '../../html/HtmlContentView';
import { HtmlEditorShell } from '../../html/HtmlEditorShell';
import shellStyles from '../../html/HtmlEditorShell.module.scss';

export const HtmlBlock = createReactBlockSpec(
  {
    type: 'graviteeHtml' as const,
    propSchema: {
      html: { default: DEFAULT_HTML_BLOCK_HTML },
      css: { default: DEFAULT_HTML_BLOCK_CSS },
    },
    content: 'none',
  },
  {
    ...getGmdBlockHooks('graviteeHtml'),
    render: ({ block, editor }) => {
      const { html, css } = block.props;
      const isEditable = editor.isEditable;

      if (!isEditable) {
        return (
          <HtmlContentView
            html={html}
            css={css}
            scopeId={`block-${block.id}`}
            isolateBlockNoteEvents
          />
        );
      }

      return (
        <div className={shellStyles.blockChrome} style={{ width: '100%' }}>
          <HtmlEditorShell
            html={html}
            css={css}
            scopeId={`block-${block.id}`}
            layout="tabs"
            onHtmlChange={nextHtml => editor.updateBlock(block, { props: { html: nextHtml } })}
            onCssChange={nextCss => editor.updateBlock(block, { props: { css: nextCss } })}
            isolateBlockNoteEvents
          />
        </div>
      );
    },
  },
);
