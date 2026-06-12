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
