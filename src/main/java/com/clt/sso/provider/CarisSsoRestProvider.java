package com.clt.sso.provider;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class CarisSsoRestProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public CarisSsoRestProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new CarisSsoRegistrationResource(session);
    }

    @Override
    public void close() {
    }
}
