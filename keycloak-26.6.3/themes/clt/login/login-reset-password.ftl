<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username'); section>

    <#if section = "form">
        <form id="kc-reset-password-form" action="${url.loginAction}" method="post">

            <input type="hidden" id="username" name="username" value="" />

            <p class="clt-instruction">
                ${msg("emailInstructionUsername")}
            </p>

            <div class="clt-form-group">
                <label for="companyCode" class="clt-label">${msg("companyCode")} <span class="clt-required">*</span></label>
                <input id="companyCode" type="text"
                       class="clt-input <#if messagesPerField.existsError('username')>clt-input-error</#if>"
                       autofocus autocomplete="organization" />
            </div>

            <div class="clt-form-group">
                <label for="displayUsername" class="clt-label">${msg("username")} <span class="clt-required">*</span></label>
                <input id="displayUsername" type="text"
                       class="clt-input <#if messagesPerField.existsError('username')>clt-input-error</#if>"
                       autocomplete="username" />
            </div>

            <button type="submit" class="clt-btn-primary">
                ${msg("doSubmit")}
            </button>

            <a href="${url.loginUrl}" class="clt-link">${msg("backToLogin")}</a>
        </form>

        <script>
            document.getElementById('kc-reset-password-form').addEventListener('submit', function(e) {
                var fields = [
                    { el: document.getElementById('companyCode'), name: 'companyCode' },
                    { el: document.getElementById('displayUsername'), name: 'username' }
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
            });
        </script>
    </#if>

</@layout.registrationLayout>
