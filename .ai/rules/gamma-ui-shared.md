---
name: Gamma UI Shared
layer: local
dirs:
  - gravitee-gamma/gamma-ui-shared
description: Shared Gamma UI components follow the UI test-id rule of the modules that render them
---

Components here render inside the Gamma UI modules, so the "UI Test Ids" section of `gravitee-gamma/gravitee-gamma-control-plane-webui/AGENTS.md` applies to every change here. If that file has no such section, do not rename or remove any `data-testid` here, and tell the user.
