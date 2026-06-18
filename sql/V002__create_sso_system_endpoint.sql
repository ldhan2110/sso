-- SSO_SYSTEM_ENDPOINT: stores sync target endpoints per app
-- EventListener reads this table to know where to POST user sync events

CREATE TABLE sso_system_endpoint (
    endpoint_id    SERIAL PRIMARY KEY,
    app_name       VARCHAR(50)  NOT NULL,
    endpoint_url   VARCHAR(500) NOT NULL,
    endpoint_type  VARCHAR(50)  NOT NULL,
    remarks        VARCHAR(500),
    act_flg        CHAR(1)      NOT NULL DEFAULT 'Y',
    cre_dt         TIMESTAMP    NOT NULL DEFAULT NOW(),
    cre_usr_id     VARCHAR(50),
    upd_dt         TIMESTAMP,
    upd_usr_id     VARCHAR(50)
);

COMMENT ON TABLE  sso_system_endpoint                IS 'Registry of external app endpoints for event-driven sync';
COMMENT ON COLUMN sso_system_endpoint.app_name       IS 'Application name: CARIS, WMS, etc.';
COMMENT ON COLUMN sso_system_endpoint.endpoint_url   IS 'Full URL to POST sync events to';
COMMENT ON COLUMN sso_system_endpoint.endpoint_type  IS 'USER_SYNC, ROLE_SYNC, HEALTH_CHECK';
COMMENT ON COLUMN sso_system_endpoint.remarks        IS 'Free-text description';
COMMENT ON COLUMN sso_system_endpoint.act_flg        IS 'Y = active, N = disabled';

-- Seed: initial endpoints
INSERT INTO sso_system_endpoint (app_name, endpoint_url, endpoint_type, remarks, cre_usr_id)
VALUES
    ('CARIS', 'http://caris-backend:8080/api/intg/sso/users/sync', 'USER_SYNC', 'CARIS user sync via integration path', 'admin'),
    ('WMS',   'http://wms-backend:8080/api/intg/sso/users/sync',   'USER_SYNC', 'WMS user sync via integration path',   'admin');
