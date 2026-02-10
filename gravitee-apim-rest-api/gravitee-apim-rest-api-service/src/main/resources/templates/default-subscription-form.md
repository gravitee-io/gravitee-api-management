<gmd-grid columns="1" class="hero-grid">
  <gmd-card class="boilerplate-hero">
    <gmd-card-title>🎨 Subscription Form Builder</gmd-card-title>
    <gmd-md>
      Welcome to your subscription form workspace! This is where you can design and customize the form that API consumers will see when subscribing to your APIs.

      **How to use this space:**

      - **✏️ Edit freely** - Modify the examples below or start from scratch
      - **👀 Live preview** - See your changes instantly on the right side
      - **🧪 Test validation** - Try filling out the form to test required fields and validation rules
      - **💾 Save & publish** - When ready, save your form and publish it to make it available in the portal

      ---

      **💡 Pro tip:** Start by editing the example form below, then delete what you don't need and build your own!
    </gmd-md>
  </gmd-card>
</gmd-grid>

<style>
  .hero-grid {
    margin-bottom: 2rem;
  }

  .boilerplate-hero {
    --gmd-card-container-color: #dbeafe;
    --gmd-card-text-color: #1e3a8a;
    --gmd-card-outline-color: #60a5fa;
    --gmd-card-outline-width: 2px;
    --gmd-card-container-shape: 12px;
  }
</style>

---

## Example 1: Simple Subscription Request Form

Here's a minimal form variant for faster onboarding:

<gmd-grid columns="1" class="compact-form-grid">
  <gmd-card class="form-demo form-demo--compact">
    <gmd-card-title>Quick Access Request</gmd-card-title>
    <gmd-md>Get started quickly with just the essentials.</gmd-md>

    <gmd-input name="appName" label="Application name" fieldKey="app_name" required="true" placeholder="My API Integration"></gmd-input>
    <gmd-select name="team" label="Team or department" fieldKey="team" options="Engineering,Product,Data & Analytics,DevOps,Security,Partner Integration,Other"></gmd-select>
    <gmd-radio name="priority" label="Request priority" fieldKey="priority" options="Low,Normal,High,Urgent"></gmd-radio>
    <gmd-textarea name="notes" label="Additional notes" fieldKey="notes" maxLength="300" placeholder="Any additional context or special requirements..."></gmd-textarea>
    <gmd-checkbox name="terms" label="I accept the terms and conditions" fieldKey="accept_terms" required="true"></gmd-checkbox>
  </gmd-card>
</gmd-grid>

---

## Example 2: Complete Subscription Request Form

Below is a full example showing all available form components. Feel free to customize it or use it as inspiration!

<gmd-grid columns="2" class="subscription-form-grid form-demo">
  <gmd-card>
    <gmd-card-title>Applicant Information</gmd-card-title>
    <gmd-md>Tell us about yourself and your organization.</gmd-md>

    <gmd-input name="fullName" label="Full name" fieldKey="full_name" required="true"></gmd-input>
    <gmd-input name="email" label="Email address" fieldKey="email" type="email" required="true" placeholder="you@company.com"></gmd-input>
    <gmd-input name="company" label="Company" fieldKey="company" required="true" minLength="2" maxLength="100"></gmd-input>
    <gmd-input name="website" label="Company website" fieldKey="company_website" type="url" placeholder="https://example.com"></gmd-input>
    <gmd-select name="country" label="Country" fieldKey="country" required="true" options="United States,Canada,United Kingdom,Germany,France,Poland,Spain,Italy,Netherlands,Other"></gmd-select>
  </gmd-card>

  <gmd-card>
    <gmd-card-title>Usage Details</gmd-card-title>
    <gmd-md>Help us understand your API usage needs.</gmd-md>

    <gmd-select name="plan" label="Requested plan" fieldKey="requested_plan" required="true" options="Free Tier,Starter,Professional,Enterprise"></gmd-select>
    <gmd-radio name="env" label="Target environment" fieldKey="target_environment" required="true" options="Production,Staging,Development,Testing"></gmd-radio>
    <gmd-input name="monthlyCalls" label="Expected monthly API calls" fieldKey="expected_monthly_calls" type="number" placeholder="e.g. 100000"></gmd-input>
    <gmd-textarea
      name="useCase"
      label="Use case description"
      fieldKey="use_case"
      required="true"
      minLength="20"
      maxLength="500"
      placeholder="Please describe how you plan to use this API and what problem it will solve for your business..."></gmd-textarea>

    <gmd-checkbox name="terms" label="I confirm that all information provided is accurate" fieldKey="confirm_accuracy" required="true"></gmd-checkbox>
    <gmd-checkbox name="newsletter" label="Subscribe to API updates and newsletters" fieldKey="subscribe_newsletter"></gmd-checkbox>
  </gmd-card>
</gmd-grid>


<style>
  .subscription-form-grid {
    align-items: stretch;
    gap: 1.5rem;
  }

  .form-demo {
    --gmd-card-container-color: #f8fafc;
    --gmd-card-outline-color: #e2e8f0;
    --gmd-card-outline-width: 1px;
    --gmd-card-container-shape: 10px;
    --gmd-card-text-color: #0f172a;

    --gmd-input-outlined-label-text-color: #0f172a;
    --gmd-input-outlined-input-text-color: #0f172a;
    --gmd-input-outlined-outline-color: #94a3b8;
    --gmd-input-outlined-container-shape: 8px;

    --gmd-textarea-outlined-label-text-color: #0f172a;
    --gmd-textarea-outlined-input-text-color: #0f172a;
    --gmd-textarea-outlined-outline-color: #94a3b8;
    --gmd-textarea-outlined-container-shape: 8px;

    --gmd-select-outlined-label-text-color: #0f172a;
    --gmd-select-outlined-input-text-color: #0f172a;
    --gmd-select-outlined-outline-color: #94a3b8;
    --gmd-select-outlined-container-shape: 8px;

    --gmd-checkbox-outlined-label-text-color: #0f172a;

    --gmd-radio-outlined-label-text-color: #0f172a;
  }

  .form-demo--compact {
    max-width: 600px;
    margin: 0 auto;
  }
</style>