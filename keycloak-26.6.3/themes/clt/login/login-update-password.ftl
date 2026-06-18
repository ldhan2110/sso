<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>

    <#if section = "form">
        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post" novalidate="novalidate">

            <div class="clt-form-group">
                <label for="password-new" class="clt-label">${msg("passwordNew")} <span class="clt-required">*</span></label>
                <div class="clt-password-wrapper">
                    <input id="password-new" name="password-new" type="password"
                           class="clt-input <#if messagesPerField.existsError('password')>clt-input-error</#if>"
                           autofocus autocomplete="new-password" />
                    <button class="clt-password-toggle" type="button" aria-label="Show password"
                            data-password-toggle data-target="password-new">
                        <i class="${properties.kcFormPasswordVisibilityIconShow!}" aria-hidden="true"></i>
                    </button>
                </div>
                <#if messagesPerField.existsError('password')>
                    <div class="clt-field-error">${messagesPerField.getFirstError('password')}</div>
                </#if>
            </div>

            <div class="clt-form-group">
                <label for="password-confirm" class="clt-label">${msg("passwordConfirm")} <span class="clt-required">*</span></label>
                <div class="clt-password-wrapper">
                    <input id="password-confirm" name="password-confirm" type="password"
                           class="clt-input <#if messagesPerField.existsError('password-confirm')>clt-input-error</#if>"
                           autocomplete="new-password" />
                    <button class="clt-password-toggle" type="button" aria-label="Show password"
                            data-password-toggle data-target="password-confirm">
                        <i class="${properties.kcFormPasswordVisibilityIconShow!}" aria-hidden="true"></i>
                    </button>
                </div>
                <#if messagesPerField.existsError('password-confirm')>
                    <div class="clt-field-error">${messagesPerField.getFirstError('password-confirm')}</div>
                </#if>
            </div>

            <div class="clt-form-group clt-logout-check">
                <label>
                    <input type="checkbox" id="logout-sessions" name="logout-sessions" value="on" checked>
                    <span>${msg("logoutOtherSessions")}</span>
                </label>
            </div>

            <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth?? && auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>

            <#if isAppInitiatedAction??>
                <button id="kc-submit" name="login" type="submit" class="clt-btn-primary">
                    ${msg("doSubmit")}
                </button>
                <button id="kc-cancel" name="cancel-aia" type="submit" class="clt-btn-secondary">
                    ${msg("doCancel")}
                </button>
            <#else>
                <button id="kc-submit" name="login" type="submit" class="clt-btn-primary">
                    ${msg("doSubmit")}
                </button>
            </#if>
        </form>

        <script>
            document.querySelectorAll('[data-password-toggle]').forEach(function(btn) {
                btn.addEventListener('click', function() {
                    var input = document.getElementById(btn.getAttribute('data-target'));
                    var icon = btn.querySelector('i');
                    if (input.type === 'password') {
                        input.type = 'text';
                        icon.className = '${properties.kcFormPasswordVisibilityIconHide!}';
                    } else {
                        input.type = 'password';
                        icon.className = '${properties.kcFormPasswordVisibilityIconShow!}';
                    }
                });
            });
        </script>
    </#if>

</@layout.registrationLayout>
