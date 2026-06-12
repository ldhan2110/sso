# CLT Login Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a custom Keycloak login theme with glassmorphism styling for the CLT forwarding & logistics ecosystem, covering login and forgot password pages.

**Architecture:** Override `template.ftl` to replace PatternFly layout with a custom full-viewport gradient background + centered glassmorphism card. Override `login.ftl` and `login-reset-password.ftl` for minimal form layouts. All visual styling lives in a single `login.css`. The theme extends `keycloak.v2` to inherit message bundles and JS utilities.

**Tech Stack:** Keycloak 26.6.3, FreeMarker templates (.ftl), CSS3 (glassmorphism, gradients, backdrop-filter)

---

## File Structure

```
keycloak-26.6.3/themes/clt/login/
├── theme.properties              # Theme config: parent=keycloak.v2, styles
├── template.ftl                  # Base layout: Deep Space bg, glassmorphism card shell
├── login.ftl                     # Login form: username, password, sign in, forgot link
├── login-reset-password.ftl      # Forgot password: email, reset button, back link
└── resources/
    └── css/
        └── login.css             # All custom styles (resets PF5, glassmorphism, responsive)
```

- **template.ftl** — owns the `<html>`, `<head>`, `<body>`. Renders background gradient, glow orbs, glassmorphism card container, CLT logo/subtitle, message alerts, and `<#nested "form">` slot. Preserves Keycloak's `<head>` requirements (scripts, importmap, auth checker, etc.).
- **login.ftl** — imports template, fills the `"form"` section with username + password fields + sign-in button + forgot password link. No social providers, no remember me, no registration link.
- **login-reset-password.ftl** — imports template, fills `"form"` section with instructional text + email field + reset button + back-to-login link.
- **login.css** — resets all PatternFly v5 styling on the login page, applies Deep Space gradient, glassmorphism card, custom input/button/label/link/alert styles, responsive breakpoints.

---

### Task 1: Create theme.properties

**Files:**
- Create: `keycloak-26.6.3/themes/clt/login/theme.properties`

- [ ] **Step 1: Create directory structure**

Run:
```bash
mkdir -p keycloak-26.6.3/themes/clt/login/resources/css
```

- [ ] **Step 2: Write theme.properties**

```properties
parent=keycloak.v2
import=common/keycloak
styles=css/login.css
```

- [ ] **Step 3: Commit**

```bash
git add keycloak-26.6.3/themes/clt/
git commit -m "feat(theme): scaffold CLT login theme with theme.properties"
```

---

### Task 2: Create login.css

**Files:**
- Create: `keycloak-26.6.3/themes/clt/login/resources/css/login.css`

- [ ] **Step 1: Write login.css with all styles from the design spec**

```css
/* ===== CLT Login Theme — Deep Space Glassmorphism ===== */

/* --- Reset & Base --- */
*, *::before, *::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html, body {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* --- Background --- */
body#keycloak-bg {
  background: linear-gradient(135deg, #0a0a1a 0%, #1a1a3e 50%, #2d1b69 100%) !important;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow-x: hidden;
}

/* Glow orbs */
body#keycloak-bg::before,
body#keycloak-bg::after {
  content: '';
  position: fixed;
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
}

body#keycloak-bg::before {
  top: -100px;
  right: -100px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(99,102,241,0.15), transparent 70%);
}

body#keycloak-bg::after {
  bottom: -80px;
  left: -80px;
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(139,92,246,0.1), transparent 70%);
}

/* --- Hide default Keycloak chrome --- */
#kc-header,
.pf-v5-c-login__header,
.pf-v5-c-login__footer,
#kc-locale,
#kc-info,
#kc-registration-container {
  display: none !important;
}

/* --- Layout container --- */
.clt-login-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

/* --- Glassmorphism Card --- */
.clt-card {
  background: rgba(255,255,255,0.06);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 20px;
  padding: 36px 32px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.3);
}

/* --- Logo --- */
.clt-logo {
  text-align: center;
  margin-bottom: 28px;
}

.clt-logo-text {
  color: #fff;
  font-weight: 800;
  font-size: 28px;
  letter-spacing: 4px;
  line-height: 1;
}

.clt-logo-subtitle {
  color: rgba(255,255,255,0.4);
  font-size: 11px;
  letter-spacing: 2px;
  margin-top: 6px;
  text-transform: uppercase;
}

/* --- Form Groups --- */
.clt-form-group {
  margin-bottom: 16px;
}

.clt-form-group:last-of-type {
  margin-bottom: 20px;
}

/* --- Labels --- */
.clt-label {
  display: block;
  color: rgba(255,255,255,0.5);
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-bottom: 6px;
  font-weight: 500;
}

/* --- Inputs --- */
.clt-input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  background: rgba(255,255,255,0.07);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 10px;
  color: #fff;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.clt-input::placeholder {
  color: rgba(255,255,255,0.3);
}

.clt-input:focus {
  border-color: rgba(99,102,241,0.5);
  box-shadow: 0 0 0 3px rgba(99,102,241,0.1);
}

.clt-input.clt-input-error {
  border-color: rgba(239,68,68,0.5);
  box-shadow: 0 0 0 3px rgba(239,68,68,0.1);
}

/* --- Password wrapper (input + toggle) --- */
.clt-password-wrapper {
  position: relative;
}

.clt-password-wrapper .clt-input {
  padding-right: 44px;
}

.clt-password-toggle {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: rgba(255,255,255,0.4);
  cursor: pointer;
  padding: 4px;
  font-size: 16px;
  line-height: 1;
}

.clt-password-toggle:hover {
  color: rgba(255,255,255,0.7);
}

/* --- Primary Button --- */
.clt-btn-primary {
  width: 100%;
  height: 44px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  border-radius: 10px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: uppercase;
  cursor: pointer;
  transition: filter 0.2s, box-shadow 0.2s, transform 0.1s;
}

.clt-btn-primary:hover {
  filter: brightness(1.1);
  box-shadow: 0 4px 20px rgba(99,102,241,0.3);
}

.clt-btn-primary:active {
  transform: scale(0.98);
}

/* --- Links --- */
.clt-link {
  display: block;
  text-align: center;
  margin-top: 16px;
  color: rgba(139,92,246,0.7);
  font-size: 12px;
  text-decoration: none;
  transition: color 0.2s;
}

.clt-link:hover {
  color: rgba(139,92,246,1);
  text-decoration: underline;
}

/* --- Alert Messages --- */
.clt-alert {
  border-radius: 10px;
  padding: 12px 16px;
  font-size: 13px;
  margin-bottom: 20px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.clt-alert-icon {
  flex-shrink: 0;
  font-size: 16px;
  line-height: 1.3;
}

.clt-alert-error {
  background: rgba(239,68,68,0.1);
  border: 1px solid rgba(239,68,68,0.3);
  color: #fca5a5;
}

.clt-alert-error .clt-alert-icon::before {
  content: '\26A0';
}

.clt-alert-success {
  background: rgba(34,197,94,0.1);
  border: 1px solid rgba(34,197,94,0.3);
  color: #86efac;
}

.clt-alert-success .clt-alert-icon::before {
  content: '\2714';
}

.clt-alert-warning {
  background: rgba(234,179,8,0.1);
  border: 1px solid rgba(234,179,8,0.3);
  color: #fde68a;
}

.clt-alert-warning .clt-alert-icon::before {
  content: '\26A0';
}

.clt-alert-info {
  background: rgba(59,130,246,0.1);
  border: 1px solid rgba(59,130,246,0.3);
  color: #93c5fd;
}

.clt-alert-info .clt-alert-icon::before {
  content: '\2139';
}

/* --- Instructional text --- */
.clt-instruction {
  color: rgba(255,255,255,0.5);
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 20px;
  text-align: center;
}

/* --- Responsive --- */
@media (max-width: 480px) {
  .clt-card {
    padding: 28px 20px;
    margin: 0 16px;
    border-radius: 16px;
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add keycloak-26.6.3/themes/clt/login/resources/css/login.css
git commit -m "feat(theme): add CLT glassmorphism login styles"
```

---

### Task 3: Create template.ftl

**Files:**
- Create: `keycloak-26.6.3/themes/clt/login/template.ftl`

This overrides the parent's base layout. It preserves all Keycloak `<head>` requirements (scripts, importmap, auth session checks) but replaces the `<body>` with our custom layout.

- [ ] **Step 1: Write template.ftl**

```html
<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}" lang="${lang}"<#if realm.internationalizationEnabled> dir="${(locale.rtl)?then('rtl','ltr')}"</#if>>

<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title!}</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <script type="importmap">
        {
            "imports": {
                "rfc4648": "${url.resourcesCommonPath}/vendor/rfc4648/rfc4648.js"
            }
        }
    </script>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <script type="module" src="${url.resourcesPath}/js/passwordVisibility.js"></script>
    <script type="module">
        <#outputformat "JavaScript">
        import { startSessionPolling } from ${(url.resourcesPath + "/js/authChecker.js")?c};
        startSessionPolling(${url.ssoLoginInOtherTabsUrl?c});
        </#outputformat>
    </script>
    <#if authenticationSession??>
        <script type="module">
            <#outputformat "JavaScript">
            import { checkAuthSession } from ${(url.resourcesPath + "/js/authChecker.js")?c};
            checkAuthSession(${authenticationSession.authSessionIdHash?c});
            </#outputformat>
        </script>
    </#if>
</head>

<body id="keycloak-bg" class="${properties.kcBodyClass!}">
  <div class="clt-login-wrapper">
    <div class="clt-card">
      <div class="clt-logo">
        <div class="clt-logo-text">CLT</div>
        <div class="clt-logo-subtitle">Forwarding &amp; Logistics</div>
      </div>

      <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
        <div class="clt-alert clt-alert-${(message.type = 'error')?then('error', message.type)}">
          <span class="clt-alert-icon"></span>
          <span>${message.summary}</span>
        </div>
      </#if>

      <#nested "form">
    </div>
  </div>
</body>
</html>
</#macro>
```

Key decisions:
- Strips PatternFly layout, header, footer, locale selector, registration link
- Keeps all `<head>` scripts Keycloak needs (importmap, authChecker, passwordVisibility, session polling)
- Alert rendering uses `message.type` directly for CSS class mapping (`clt-alert-error`, `clt-alert-success`, etc.)
- Only renders `"form"` nested section — no `"header"`, `"info"`, or `"socialProviders"` sections

- [ ] **Step 2: Commit**

```bash
git add keycloak-26.6.3/themes/clt/login/template.ftl
git commit -m "feat(theme): add CLT custom template.ftl with glassmorphism layout"
```

---

### Task 4: Create login.ftl

**Files:**
- Create: `keycloak-26.6.3/themes/clt/login/login.ftl`

- [ ] **Step 1: Write login.ftl**

```html
<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password'); section>

    <#if section = "form">
        <#if realm.password>
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post" novalidate="novalidate">

                <#if !usernameHidden??>
                    <div class="clt-form-group">
                        <#assign label>
                            <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
                        </#assign>
                        <label for="username" class="clt-label">${label}</label>
                        <input id="username" name="username" value="${login.username!''}" type="text"
                               class="clt-input <#if messagesPerField.existsError('username','password')>clt-input-error</#if>"
                               autofocus autocomplete="username" />
                    </div>
                </#if>

                <div class="clt-form-group">
                    <label for="password" class="clt-label">${msg("password")}</label>
                    <div class="clt-password-wrapper">
                        <input id="password" name="password" type="password"
                               class="clt-input <#if messagesPerField.existsError('username','password')>clt-input-error</#if>"
                               <#if usernameHidden??>autofocus</#if> autocomplete="current-password" />
                        <button class="clt-password-toggle" type="button" aria-label="${msg('showPassword')}"
                                aria-controls="password" data-password-toggle
                                data-icon-show="${properties.kcFormPasswordVisibilityIconShow!}"
                                data-icon-hide="${properties.kcFormPasswordVisibilityIconHide!}"
                                data-label-show="${msg('showPassword')}" data-label-hide="${msg('hidePassword')}"
                                id="password-show-password">
                            <i class="${properties.kcFormPasswordVisibilityIconShow!}" aria-hidden="true"></i>
                        </button>
                    </div>
                </div>

                <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>

                <button id="kc-login" name="login" type="submit" class="clt-btn-primary">
                    ${msg("doLogIn")}
                </button>

                <#if realm.resetPasswordAllowed>
                    <a href="${url.loginResetCredentialsUrl}" class="clt-link">${msg("doForgotPassword")}</a>
                </#if>
            </form>
        </#if>
    </#if>

</@layout.registrationLayout>
```

Key decisions:
- Uses Keycloak's built-in `msg()` for i18n labels (not hardcoded "USERNAME"/"PASSWORD")
- Preserves `data-password-toggle` for Keycloak's `passwordVisibility.js` to hook into
- Preserves `credentialId` hidden input for WebAuthn flows
- Error state applied to both username and password inputs simultaneously (Keycloak bundles these errors)
- No remember me, social providers, or registration link

- [ ] **Step 2: Commit**

```bash
git add keycloak-26.6.3/themes/clt/login/login.ftl
git commit -m "feat(theme): add CLT login page template"
```

---

### Task 5: Create login-reset-password.ftl

**Files:**
- Create: `keycloak-26.6.3/themes/clt/login/login-reset-password.ftl`

- [ ] **Step 1: Write login-reset-password.ftl**

```html
<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username'); section>

    <#if section = "form">
        <form id="kc-reset-password-form" action="${url.loginAction}" method="post">

            <p class="clt-instruction">
                <#if realm.duplicateEmailsAllowed>
                    ${msg("emailInstructionUsername")}
                <#else>
                    ${msg("emailInstruction")}
                </#if>
            </p>

            <div class="clt-form-group">
                <#assign label>
                    <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
                </#assign>
                <label for="username" class="clt-label">${label}</label>
                <input id="username" name="username" value="${auth.attemptedUsername!''}" type="text"
                       class="clt-input <#if messagesPerField.existsError('username')>clt-input-error</#if>"
                       autofocus autocomplete="username" />
            </div>

            <button type="submit" class="clt-btn-primary">
                ${msg("doSubmit")}
            </button>

            <a href="${url.loginUrl}" class="clt-link">${msg("backToLogin")}</a>
        </form>
    </#if>

</@layout.registrationLayout>
```

Key decisions:
- Uses `msg("emailInstruction")` / `msg("emailInstructionUsername")` for i18n — maps to Keycloak's built-in "Enter your username or email address and we will send you instructions on how to create a new password."
- Uses `msg("doSubmit")` for button text and `msg("backToLogin")` for back link — both are existing Keycloak message keys
- Handles `duplicateEmailsAllowed` realm config like the built-in template

- [ ] **Step 2: Commit**

```bash
git add keycloak-26.6.3/themes/clt/login/login-reset-password.ftl
git commit -m "feat(theme): add CLT forgot password page template"
```

---

### Task 6: Manual verification

- [ ] **Step 1: Verify file structure**

Run:
```bash
find keycloak-26.6.3/themes/clt -type f | sort
```

Expected output:
```
keycloak-26.6.3/themes/clt/login/login-reset-password.ftl
keycloak-26.6.3/themes/clt/login/login.ftl
keycloak-26.6.3/themes/clt/login/resources/css/login.css
keycloak-26.6.3/themes/clt/login/template.ftl
keycloak-26.6.3/themes/clt/login/theme.properties
```

- [ ] **Step 2: Start Keycloak in dev mode to test**

Run:
```bash
cd keycloak-26.6.3
bin/kc.sh start-dev
```

In dev mode, themes are not cached — changes are live-reloaded.

- [ ] **Step 3: Apply theme in Keycloak admin console**

1. Open http://localhost:8080/admin
2. Go to Realm Settings → Themes
3. Set Login Theme to `clt`
4. Save

- [ ] **Step 4: Test login page**

1. Open an incognito window
2. Go to http://localhost:8080/realms/{realm-name}/account
3. Verify: Deep Space gradient background with glow orbs
4. Verify: Centered glassmorphism card with CLT logo + subtitle
5. Verify: Username and password fields with uppercase labels
6. Verify: Gradient "Sign In" button
7. Verify: "Forgot password?" link below button
8. Try wrong credentials — verify error alert appears styled correctly
9. Resize to mobile width — verify responsive card padding

- [ ] **Step 5: Test forgot password page**

1. Click "Forgot password?" link
2. Verify: Same background and card style
3. Verify: Instructional text present
4. Verify: Email/username field
5. Verify: "Submit" button with gradient style
6. Verify: "Back to Login" link
7. Submit empty — verify error styling
