CREATE TABLE sso_user (
    tent_id         VARCHAR(50)     NOT NULL,
    usr_id          VARCHAR(50)     NOT NULL,
    usr_nm          VARCHAR(100)    NOT NULL,
    usr_pwd         VARCHAR(100),
    usr_eml         VARCHAR(255),
    act_flg         CHAR(1)         DEFAULT 'Y',
    cre_dt          BIGINT          NOT NULL,
    cre_usr_id      VARCHAR(50),
    upd_dt          BIGINT          NOT NULL,
    upd_usr_id      VARCHAR(50),
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    email_verified  BOOLEAN         DEFAULT FALSE,

    CONSTRAINT pk_sso_user PRIMARY KEY (tent_id, usr_id),
    CONSTRAINT uk_sso_user_nm UNIQUE (tent_id, usr_nm)
);

COMMENT ON TABLE sso_user IS 'External user storage for Keycloak SPI (caris-sso)';
COMMENT ON COLUMN sso_user.tent_id IS 'Tenant ID for multi-tenant isolation';
COMMENT ON COLUMN sso_user.usr_pwd IS 'BCrypt hashed password';
COMMENT ON COLUMN sso_user.act_flg IS 'Active flag: Y=enabled, N=disabled';
COMMENT ON COLUMN sso_user.cre_dt IS 'Creation timestamp (epoch millis)';
COMMENT ON COLUMN sso_user.upd_dt IS 'Last update timestamp (epoch millis)';
