<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>

    <#if section = "form">
        <#if realm.password>
            <form id="kc-form-login" action="${url.loginAction}" method="post" novalidate="novalidate">

                <input type="hidden" id="username" name="username" value="" />

                <#if !usernameHidden??>
                    <div class="clt-form-group">
                        <label for="companyCode" class="clt-label">${msg("companyCode")} <span class="clt-required">*</span></label>
                        <input id="companyCode" type="text"
                               class="clt-input <#if messagesPerField.existsError('username','password')>clt-input-error</#if>"
                               autofocus autocomplete="organization" />
                    </div>

                    <div class="clt-form-group">
                        <label for="displayUsername" class="clt-label">${msg("username")} <span class="clt-required">*</span></label>
                        <input id="displayUsername" type="text"
                               class="clt-input <#if messagesPerField.existsError('username','password')>clt-input-error</#if>"
                               autocomplete="username" />
                    </div>
                </#if>

                <div class="clt-form-group">
                    <label for="password" class="clt-label">${msg("password")} <span class="clt-required">*</span></label>
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

            <script>
                document.getElementById('kc-form-login').addEventListener('submit', function(e) {
                    var fields = [
                        { el: document.getElementById('companyCode'), name: 'companyCode' },
                        { el: document.getElementById('displayUsername'), name: 'username' },
                        { el: document.getElementById('password'), name: 'password' }
                    ];
                    var valid = true;

                    fields.forEach(function(f) {
                        var group = f.el.closest('.clt-form-group');
                        var msg = group.querySelector('.clt-field-error');
                        if (msg) msg.remove();
                        f.el.classList.remove('clt-input-error');

                        if (!f.el.value.trim()) {
                            valid = false;
                            f.el.classList.add('clt-input-error');
                            var err = document.createElement('div');
                            err.className = 'clt-field-error';
                            err.textContent = 'This field is required';
                            group.appendChild(err);
                        }
                    });

                    if (!valid) {
                        e.preventDefault();
                        return false;
                    }

                    document.getElementById('username').value =
                        fields[0].el.value.trim() + '::' + fields[1].el.value.trim();
                    document.getElementById('kc-login').disabled = true;
                });
            </script>
        </#if>
    </#if>

</@layout.registrationLayout>
