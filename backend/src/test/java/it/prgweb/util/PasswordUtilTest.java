package it.prgweb.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void hashEVerificaPassword() {
        String hash = PasswordUtil.hash("segreta123");
        assertNotEquals("segreta123", hash);
        assertTrue(PasswordUtil.check("segreta123", hash));
        assertFalse(PasswordUtil.check("sbagliata", hash));
    }

    @Test
    void hashNonValidoNonSollevaEccezioni() {
        assertFalse(PasswordUtil.check("x", "non-un-hash"));
        assertFalse(PasswordUtil.check(null, null));
    }
}
