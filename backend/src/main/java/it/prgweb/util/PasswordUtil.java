package it.prgweb.util;

import org.mindrot.jbcrypt.BCrypt;

/** Hashing password con BCrypt. */
public final class PasswordUtil {

    private static final int COST = 10;

    private PasswordUtil() {
    }

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(COST));
    }

    public static boolean check(String password, String hash) {
        if (password == null || hash == null || hash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
