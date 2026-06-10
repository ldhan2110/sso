package com.clt.sso.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

import com.clt.sso.utils.SessionFactory;

public class CarisSsoUserStorageProviderFactory implements UserStorageProviderFactory<CarisSsoUserStorageProvider> {

    public static final String PROVIDER_ID = "caris-external-users";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public CarisSsoUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new CarisSsoUserStorageProvider(session, model, SessionFactory.getSqlSession());
    }
}
