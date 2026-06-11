package com.clt.sso.provider;

import com.clt.sso.model.UserInfoAdapter;
import com.clt.sso.model.UserInfoModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.Collections;
import java.util.List;

public class CarisSsoProtocolMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper {

    public static final String PROVIDER_ID = "caris-sso-claims-mapper";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Caris SSO Claims";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Adds tenant_id and user_id claims from Caris SSO user storage";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel,
                           UserSessionModel userSession, KeycloakSession keycloakSession,
                           ClientSessionContext clientSessionCtx) {
        if (!(userSession.getUser() instanceof UserInfoAdapter)) {
            return;
        }
        UserInfoModel entity = ((UserInfoAdapter) userSession.getUser()).getEntity();

        if (entity.getTentId() != null) {
            token.getOtherClaims().put("tenant_id", entity.getTentId());
        }
        if (entity.getUsrId() != null) {
            token.getOtherClaims().put("user_id", entity.getUsrId());
        }
    }
}
