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
