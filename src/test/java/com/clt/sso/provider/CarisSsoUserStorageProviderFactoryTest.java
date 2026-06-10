package com.clt.sso.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CarisSsoUserStorageProviderFactoryTest {

    @Test
    void getId_returnsProviderName() {
        CarisSsoUserStorageProviderFactory factory = new CarisSsoUserStorageProviderFactory();
        assertEquals("caris-external-users", factory.getId());
    }
}
