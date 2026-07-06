---
name: APIM Repository Guide
layer: local
description: Repo layout, setup entrypoint, and where module-specific rules live
---

# APIM Repository Guide

This repository contains multiple backend modules in Java (Maven) and multiple frontend applications and libraries in Angular.

## Setup, build, and runtime

For setup, build, and runtime instructions, read `CONTRIBUTING.adoc`, section **AI Agent Context (Docker Compose Full Stack)**. That section is the source of truth for AI tools.

## Module-specific rules

Several modules keep their own hand-written `AGENTS.md` with conventions that add to or specialize these rules; when editing inside a module, read that module's `AGENTS.md` if it exists (walk up from the files you edit). If something conflicts, prefer the module guidance:

`gravitee-apim-common`, `gravitee-apim-definition`, `gravitee-apim-distribution`, `gravitee-apim-gateway`, `gravitee-apim-integration-tests`, `gravitee-apim-plugin`, `gravitee-apim-repository`, `gravitee-apim-reporter`, `gravitee-apim-rest-api`, `gravitee-gamma/gravitee-gamma-module-apim`, `gravitee-gamma/gravitee-gamma-module-platform`, `gravitee-apim-portal-webui-next`, `gravitee-apim-webui-libs/*`.
