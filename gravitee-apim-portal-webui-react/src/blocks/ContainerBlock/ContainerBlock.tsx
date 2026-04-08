import { Node } from '@tiptap/core';
import { createBlockSpecFromTiptapNode } from '@blocknote/core';

type ContainerVariant = 'dark' | 'light' | 'gray' | 'accent' | 'none';

const containerVariants: ContainerVariant[] = ['none', 'dark', 'light', 'gray', 'accent'];

const variantStyles: Record<ContainerVariant, { bg: string; color: string; border?: string }> = {
  dark: { bg: '#111827', color: '#f9fafb' },
  light: { bg: '#f8fafc', color: '#111827', border: '1px solid #e5e7eb' },
  gray: { bg: '#f3f4f6', color: '#111827' },
  accent: { bg: '#7ec8c8', color: '#111827' },
  none: { bg: 'transparent', color: '#111827' },
};

const paddingValues: Record<string, string> = {
  small: '24px 48px',
  medium: '48px 48px',
  large: '72px 48px',
};

const paddingSizes = ['small', 'medium', 'large'];

function createControlButton(title: string, svgInner: string, onClick: () => void, isDark: boolean): HTMLButtonElement {
  const btn = document.createElement('button');
  btn.type = 'button';
  btn.title = title;
  btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">${svgInner}</svg>`;
  Object.assign(btn.style, {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '28px',
    height: '28px',
    border: 'none',
    borderRadius: '6px',
    background: isDark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.06)',
    color: isDark ? 'rgba(255,255,255,0.8)' : '#6b7280',
    cursor: 'pointer',
    backdropFilter: 'blur(8px)',
  });
  btn.addEventListener('mouseenter', () => {
    btn.style.background = isDark ? 'rgba(255,255,255,0.22)' : 'rgba(0,0,0,0.1)';
  });
  btn.addEventListener('mouseleave', () => {
    btn.style.background = isDark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.06)';
  });
  btn.addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();
    onClick();
  });
  return btn;
}

export const ContainerNode = Node.create({
  name: 'graviteeContainer',
  group: 'bnBlock childContainer',
  content: 'blockContainer+',
  priority: 40,
  defining: true,

  addAttributes() {
    return {
      variant: {
        default: 'light',
        parseHTML: (el) => el.getAttribute('data-variant') || 'light',
        renderHTML: (attrs) => ({ 'data-variant': attrs.variant }),
      },
      padding: {
        default: 'medium',
        parseHTML: (el) => el.getAttribute('data-padding') || 'medium',
        renderHTML: (attrs) => ({ 'data-padding': attrs.padding }),
      },
    };
  },

  parseHTML() {
    return [{
      tag: 'div',
      getAttrs: (el) => {
        if (typeof el === 'string') return false;
        return el.getAttribute('data-node-type') === this.name ? {} : false;
      },
    }];
  },

  renderHTML({ HTMLAttributes, node }) {
    const variant = (node.attrs.variant as ContainerVariant) || 'light';
    const padding = (node.attrs.padding as string) || 'medium';
    const vs = variantStyles[variant] || variantStyles.light;

    const wrapper = document.createElement('div');
    wrapper.setAttribute('data-node-type', this.name);
    wrapper.className = 'bn-container-section';
    for (const [k, v] of Object.entries(HTMLAttributes)) {
      if (v != null) wrapper.setAttribute(k, v as string);
    }
    Object.assign(wrapper.style, {
      position: 'relative',
      borderRadius: '0',
      overflow: 'hidden',
      left: '50%',
      right: '50%',
      width: '100vw',
      marginLeft: '-50vw',
      marginRight: '-50vw',
      padding: paddingValues[padding] || paddingValues.medium,
      background: vs.bg,
      color: vs.color,
      borderTop: vs.border || 'none',
      borderBottom: vs.border || 'none',
    });

    const inner = document.createElement('div');
    inner.style.margin = '0 auto';
    inner.style.setProperty('max-width', 'var(--page-width, 760px)');
    inner.style.setProperty('transition', 'max-width 0.3s ease');
    wrapper.appendChild(inner);

    return { dom: wrapper, contentDOM: inner };
  },

  addNodeView() {
    return ({ node, getPos, editor: tiptapEditor }) => {
      const variant = (node.attrs.variant as ContainerVariant) || 'light';
      const padding = (node.attrs.padding as string) || 'medium';
      const vs = variantStyles[variant] || variantStyles.light;
      const isDark = variant === 'dark';
      const isEditable = tiptapEditor.isEditable;

      const wrapper = document.createElement('div');
      wrapper.className = 'bn-container-section';
      Object.assign(wrapper.style, {
        position: 'relative',
        borderRadius: '0',
        overflow: 'visible',
        left: '50%',
        right: '50%',
        width: '100vw',
        marginLeft: '-50vw',
        marginRight: '-50vw',
        padding: paddingValues[padding] || paddingValues.medium,
        background: vs.bg,
        color: vs.color,
        borderTop: vs.border || 'none',
        borderBottom: vs.border || 'none',
        transition: 'background 0.2s, color 0.2s, padding 0.2s',
      });

      const inner = document.createElement('div');
      inner.style.margin = '0 auto';
      inner.style.setProperty('max-width', 'var(--page-width, 760px)');
      inner.style.setProperty('transition', 'max-width 0.3s ease');
      wrapper.appendChild(inner);

      if (isEditable) {
        const controls = document.createElement('div');
        Object.assign(controls.style, {
          position: 'absolute',
          top: '8px',
          right: '8px',
          display: 'flex',
          gap: '4px',
          opacity: '0',
          transition: 'opacity 0.15s',
          zIndex: '5',
        });

        wrapper.addEventListener('mouseenter', () => { controls.style.opacity = '1'; });
        wrapper.addEventListener('mouseleave', () => { controls.style.opacity = '0'; });

        const refreshStyles = () => {
          const pos = getPos();
          if (pos === undefined) return;
          const currentNode = tiptapEditor.state.doc.nodeAt(pos);
          if (!currentNode) return;
          const v = (currentNode.attrs.variant as ContainerVariant) || 'light';
          const p = (currentNode.attrs.padding as string) || 'medium';
          const s = variantStyles[v] || variantStyles.light;
          Object.assign(wrapper.style, {
            background: s.bg,
            color: s.color,
            padding: paddingValues[p] || paddingValues.medium,
            borderTop: s.border || 'none',
            borderBottom: s.border || 'none',
          });
        };

        const colorIcon = '<circle cx="13.5" cy="6.5" r="2.5"/><circle cx="6" cy="12" r="2.5"/><circle cx="18" cy="12" r="2.5"/><circle cx="12" cy="18" r="2.5"/>';
        const paddingIcon = '<rect x="3" y="3" width="18" height="18" rx="2"/><rect x="7" y="7" width="10" height="10" rx="1"/>';

        controls.appendChild(createControlButton('Change background', colorIcon, () => {
          const pos = getPos();
          if (pos === undefined) return;
          const cur = tiptapEditor.state.doc.nodeAt(pos);
          if (!cur) return;
          const idx = containerVariants.indexOf(cur.attrs.variant as ContainerVariant);
          const next = containerVariants[(idx + 1) % containerVariants.length];
          tiptapEditor.chain().focus().command(({ tr }) => {
            tr.setNodeMarkup(pos, undefined, { ...cur.attrs, variant: next });
            return true;
          }).run();
          setTimeout(refreshStyles, 10);
        }, isDark));

        controls.appendChild(createControlButton('Change padding', paddingIcon, () => {
          const pos = getPos();
          if (pos === undefined) return;
          const cur = tiptapEditor.state.doc.nodeAt(pos);
          if (!cur) return;
          const idx = paddingSizes.indexOf(cur.attrs.padding as string);
          const next = paddingSizes[(idx + 1) % paddingSizes.length];
          tiptapEditor.chain().focus().command(({ tr }) => {
            tr.setNodeMarkup(pos, undefined, { ...cur.attrs, padding: next });
            return true;
          }).run();
          setTimeout(refreshStyles, 10);
        }, isDark));

        wrapper.appendChild(controls);
      }

      return { dom: wrapper, contentDOM: inner };
    };
  },
});

export const ContainerBlock = createBlockSpecFromTiptapNode(
  {
    node: ContainerNode,
    type: 'graviteeContainer',
    content: 'none',
  },
  {
    variant: { default: 'light' },
    padding: { default: 'medium' },
  },
);
