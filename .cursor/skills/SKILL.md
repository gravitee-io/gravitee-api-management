---
name: gravitee-design-system-setup
description: Particles + Angular Material reference and UI archetypes. Use when implementing, modifying, reviewing, or onboarding to UI in yarn Angular projects.
---

# Particles Design System Setup

## When to use this skill

- Implementing new UI in a yarn Angular project
- Modifying or fixing existing UI
- Reviewing UI code for design system compliance
- Onboarding or resolving "which library?" questions

Excluded: `gravitee-apim-portal-webui-next`, `gravitee-apim-portal-webui`.

## Official references

| System             | URL                            | Use for                            |
| ------------------ | ------------------------------ | ---------------------------------- |
| **Particles**      | https://particles.gravitee.io/ | Components (`gio-`), icons, tokens |
| **Angular Material** | https://material.angular.io/ | Fallback components (`mat-`)       |

Always check Particles first.

## Required workflow

1. Check Particles (https://particles.gravitee.io/) for components/tokens before choosing Material.
2. Match screen to the closest archetype below and follow its composition.
3. Validate against the Layout & UI patterns MUSTs in the rule (`particles-design-system.mdc` in `.cursor/rules/` or `cursor/samples/rules/`).

## Page archetypes

### List of items

```
Page title (outside card, top-left)
Layout container (centered, max-width)
  Card
    Single primary CTA to add new item (right-aligned)
    Toolbar: filters/search (left-aligned), pagination (right-aligned) 
    Table
      Header row: aligned with cell content, current header style
      Data rows: consistent padding
      Action column: icon buttons, minimal fixed width
    Pagination (bottom-right)
```

### Create new item

```
Page title (outside card, top-left)
Layout container (centered, max-width)
  Card
    Form sections with field groups (consistent spacing, 4px grid)
    Card footer right-aligned content
      Simple button (e.g. "Cancel")
      Single primary CTA (e.g. "Create")
  No save bar
```

### Edit existing item

```
Page title (outside card, top-left)
Layout container (centered, max-width)
  Card
    Form sections with field groups (consistent spacing, 4px grid)
    Card footer: secondary actions (left), single primary CTA (right-aligned, e.g. "Save")
    Card footer right-aligned content
      secondary actions (left-aligned, e.g. "Cancel")
      Single primary CTA (e.g. "Save")
  No save bar
```

## Checklist

- Spacing: 4px grid, tokens only
- Composition: matches closest archetype
- Escape hatch: warn if request deviates
