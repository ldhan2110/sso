# CLT Login Theme — Design Spec

## Overview

Custom Keycloak login theme for CLT forwarding & logistics ecosystem. Modern, tech-forward glassmorphism design with Deep Space color palette. Covers login and forgot password pages.

**Platform:** Keycloak 26.6.3 custom theme (FreeMarker `.ftl` + CSS)
**Parent theme:** `keycloak.v2`
**Deploy path:** `keycloak-26.6.3/themes/clt/login/`

## Pages

### 1. Login Page (`login.ftl`)

**Layout:** Centered glassmorphism card over full-viewport gradient background.

**Elements (top to bottom):**
1. CLT logo — bold white text, 28px, `letter-spacing: 4px`
2. Subtitle — "FORWARDING & LOGISTICS", muted white (`rgba(255,255,255,0.4)`), 11px, `letter-spacing: 2px`
3. Username field — uppercase label + dark transparent input
4. Password field — uppercase label + dark transparent input
5. "SIGN IN" button — full-width gradient (`#6366f1` → `#8b5cf6`), 13px bold, `letter-spacing: 1px`
6. "Forgot password?" link — muted violet (`rgba(139,92,246,0.7)`), 12px, centered

**No other elements.** No remember me, no social login, no language selector.

### 2. Forgot Password Page (`login-reset-password.ftl`)

**Layout:** Same card, same background as login.

**Elements (top to bottom):**
1. CLT logo + subtitle (same as login)
2. Instructional text — "Enter your email address and we'll send you a link to reset your password." — muted white, 13px
3. Email field — uppercase label + dark transparent input
4. "RESET PASSWORD" button — same gradient style as sign in
5. "Back to login" link — muted violet, 12px, centered

## Visual Specification

### Background
- **Gradient:** `linear-gradient(135deg, #0a0a1a 0%, #1a1a3e 50%, #2d1b69 100%)`
- **Glow orbs:** Two radial gradients positioned top-right and bottom-left
  - Top-right: `radial-gradient(circle, rgba(99,102,241,0.15), transparent 70%)`, 200px
  - Bottom-left: `radial-gradient(circle, rgba(139,92,246,0.1), transparent 70%)`, 160px
- **Full viewport:** `min-height: 100vh`, centered flex container

### Card
- **Background:** `rgba(255,255,255,0.06)`
- **Backdrop filter:** `blur(24px)`
- **Border:** `1px solid rgba(255,255,255,0.1)`
- **Border radius:** `20px`
- **Padding:** `36px 32px`
- **Max width:** `400px`
- **Shadow:** `0 8px 32px rgba(0,0,0,0.3)`

### Form Inputs
- **Background:** `rgba(255,255,255,0.07)`
- **Border:** `1px solid rgba(255,255,255,0.1)`
- **Border radius:** `10px`
- **Height:** `42px`
- **Padding:** `0 14px`
- **Color:** `#fff`
- **Placeholder color:** `rgba(255,255,255,0.3)`
- **Focus state:** border color `rgba(99,102,241,0.5)`, subtle glow `0 0 0 3px rgba(99,102,241,0.1)`

### Labels
- **Color:** `rgba(255,255,255,0.5)`
- **Font size:** `11px`
- **Letter spacing:** `1px`
- **Text transform:** `uppercase`
- **Margin bottom:** `6px`

### Primary Button
- **Background:** `linear-gradient(135deg, #6366f1, #8b5cf6)`
- **Border radius:** `10px`
- **Height:** `44px`
- **Color:** `#fff`
- **Font size:** `13px`
- **Font weight:** `600`
- **Letter spacing:** `1px`
- **Text transform:** `uppercase`
- **Hover:** brightness increase, subtle lift shadow
- **Active:** slight scale down

### Links
- **Color:** `rgba(139,92,246,0.7)`
- **Hover:** `rgba(139,92,246,1)`
- **Font size:** `12px`
- **No underline** (underline on hover)

### Error Messages
- **Background:** `rgba(239,68,68,0.1)`
- **Border:** `1px solid rgba(239,68,68,0.3)`
- **Color:** `#fca5a5`
- **Border radius:** `10px`
- **Padding:** `12px 16px`
- **Font size:** `13px`

### Success Messages
- **Background:** `rgba(34,197,94,0.1)`
- **Border:** `1px solid rgba(34,197,94,0.3)`
- **Color:** `#86efac`
- **Border radius:** `10px`

## File Structure

```
keycloak-26.6.3/themes/clt/
  login/
    theme.properties          # Theme config, parent=keycloak.v2
    login.ftl                 # Login page template
    login-reset-password.ftl  # Forgot password template
    resources/
      css/
        login.css             # All custom styles
      img/
        logo.svg              # CLT logo (optional, can use text)
```

## theme.properties

```properties
parent=keycloak.v2
import=common/keycloak
styles=css/login.css
```

## Responsive Behavior

- Card max-width `400px`, fluid on smaller screens
- Below `480px`: card padding reduces to `28px 20px`, full-width with `16px` margin
- Background gradient and glow orbs remain, no layout changes
- Inputs and buttons maintain full width within card

## Accessibility

- All form inputs linked to labels via `for`/`id`
- Focus states visible (indigo glow ring)
- Color contrast: white text on dark backgrounds meets WCAG AA
- Error messages use both color and icon for non-color-dependent communication
- Tab order: username → password → sign in → forgot password
