<gmd-grid columns="1">
<gmd-md>
# Gravitee Markdown — full showcase

This page demonstrates **standard Markdown**, **raw HTML**, and **embedded GMD components** together: layout grid, cards, buttons, and form fields (inputs bind to the viewer when used inside a `gmd-form-host` on subscription flows).

[Jump to the live form](#interactive-form-demo) · [Jump to API example](#code-and-tables)
</gmd-md>
<gmd-cell>
<gmd-grid columns="3">
<gmd-cell>
<gmd-button appearance="filled" link="/catalog">Primary CTA</gmd-button>
</gmd-cell>
<gmd-cell>
<gmd-button appearance="outlined" link="/documentation">Secondary</gmd-button>
</gmd-cell>
<gmd-cell>
<gmd-button appearance="text" link="https://documentation.gravitee.io" target="_blank">Docs (new tab)</gmd-button>
</gmd-cell>
</gmd-grid>
</gmd-cell>
</gmd-grid>

<gmd-grid columns="3">
<gmd-cell>
<gmd-card>
<gmd-card-title>Markdown inside cards</gmd-card-title>
<gmd-card-subtitle>`gmd-md` blocks render marked content</gmd-card-subtitle>
<gmd-md>
Use **bold**, *italic*, `inline code`, and [links](https://gravitee.io).

> Blockquotes for callouts or quotes from specs.

1. Ordered steps
2. Stay readable in the portal
3. Mix with components outside the card
</gmd-md>
</gmd-card>
</gmd-cell>
<gmd-cell>
<gmd-card backgroundColor="#1a1d2e" textColor="#f5f5f7">
<gmd-card-title>Themed card</gmd-card-title>
<gmd-card-subtitle>Inputs: `backgroundColor` · `textColor`</gmd-card-subtitle>
<gmd-md>
Contrast panels for **hero metrics**, **alerts**, or **code-oriented** sections. Pair with outlined buttons for balance.
</gmd-md>
<gmd-button appearance="outlined" link="/catalog">Explore APIs</gmd-button>
</gmd-card>
</gmd-cell>
<gmd-cell>
<gmd-card>
<gmd-card-title>Responsive grid</gmd-card-title>
<gmd-md>
`gmd-grid` uses **1–6** columns and collapses on smaller breakpoints. Wrap cells with `gmd-cell` for each column slot.
</gmd-md>
</gmd-card>
</gmd-cell>
</gmd-grid>

<gmd-md>
## Code and tables

### Example request

```bash
curl https://api.example.com/v1/resources \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Accept: application/json"
```

### Example JSON body

```json
{
  "name": "sample-payload",
  "tags": ["gmd", "gravitee", "portal"],
  "enabled": true
}
```

| Feature | Markdown | GMD components |
| :--- | :---: | :--- |
| Headings / lists | Yes | Inside `gmd-md` |
| Tables | Yes | Same |
| Layout | HTML `div` | `gmd-grid` / `gmd-cell` |
| Actions | Links | `gmd-button` |
| Forms | — | `gmd-input`, `gmd-select`, … |

<details>
<summary><strong>Expandable HTML section</strong> (native <code>details</code>)</summary>

<p>Use standard HTML where Markdown is not enough—collapsibles, extra wrappers, or embedded media.</p>
</details>
</gmd-md>

<gmd-md>
## Interactive form demo

These fields use **validation** (required, lengths) and **field keys** for form state when hosted in a form context. In the plain viewer they still render and validate client-side.

</gmd-md>

<gmd-grid columns="1">
<gmd-cell>
<gmd-card>
<gmd-card-title>Contact & access request</gmd-card-title>
<gmd-card-subtitle>All GMD form controls in one place</gmd-card-subtitle>
<gmd-grid columns="2">
<gmd-cell>
<gmd-input name="fullName" label="Full name" placeholder="Ada Lovelace" fieldKey="fullName" required="true" minLength="2" maxLength="80"></gmd-input>
</gmd-cell>
<gmd-cell>
<gmd-input name="workEmail" label="Work email" placeholder="ada@example.com" fieldKey="workEmail" required="true" minLength="5" maxLength="120"></gmd-input>
</gmd-cell>
<gmd-cell>
<gmd-select name="region" label="Region" fieldKey="region" required="true" options="eu-west-1,us-east-1,ap-southeast-1"></gmd-select>
</gmd-cell>
<gmd-cell>
<gmd-select name="plan" label="Plan" fieldKey="plan" value="Pro" options='["Free","Pro","Enterprise"]'></gmd-select>
</gmd-cell>
</gmd-grid>
<gmd-grid columns="1">
<gmd-cell>
<gmd-textarea name="useCase" label="Describe your use case" placeholder="What will you build with the API?" fieldKey="useCase" required="true" minLength="20" maxLength="2000" rows="5"></gmd-textarea>
</gmd-cell>
</gmd-grid>
<gmd-grid columns="2">
<gmd-cell>
<gmd-radio name="environment" label="Target environment" fieldKey="environment" required="true" options="Sandbox,Staging,Production"></gmd-radio>
</gmd-cell>
<gmd-cell>
<gmd-checkbox-group name="capabilities" label="Required capabilities" fieldKey="capabilities" required="true" options="OAuth2,Rate limiting,Analytics,Caching,Federation"></gmd-checkbox-group>
</gmd-cell>
</gmd-grid>
<gmd-grid columns="1">
<gmd-cell>
<gmd-checkbox name="acceptTerms" label="I agree to the API terms and acceptable use policy" fieldKey="acceptTerms" required="true"></gmd-checkbox>
</gmd-cell>
</gmd-grid>
<gmd-grid columns="2">
<gmd-cell>
<gmd-button appearance="filled" link="/catalog">Submit (demo link)</gmd-button>
</gmd-cell>
<gmd-cell>
<gmd-button appearance="outlined" link="/documentation">Cancel</gmd-button>
</gmd-cell>
</gmd-grid>
</gmd-card>
</gmd-cell>
</gmd-grid>

<gmd-md>
## Read-only & disabled samples

Useful for **displaying issued credentials** or **locked plans**.

</gmd-md>

<gmd-grid columns="2">
<gmd-cell>
<gmd-input name="clientId" label="Client ID (read-only)" value="gmd_demo_client_7f3a" readonly="true" fieldKey="clientId"></gmd-input>
</gmd-cell>
<gmd-cell>
<gmd-input name="legacyKey" label="Deprecated key" value="Disabled field" disabled="true"></gmd-input>
</gmd-cell>
</gmd-grid>

<gmd-md>
---

*This file lives in the Gravitee Markdown package at `src/lib/assets/examples/gmd-full-showcase.md`—paste it into a **Gravitee Markdown** documentation page or load it in Storybook to preview the full stack.*
</gmd-md>
