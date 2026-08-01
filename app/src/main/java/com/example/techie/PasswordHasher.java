package com.example.techie;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class PasswordHasher {

    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";

    private PasswordHasher() {
    }

    static PasswordHash create(String password) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        char[] characters = password.toCharArray();
        try {
            byte[] hash = derive(characters, salt);
            return new PasswordHash(toHex(salt), toHex(hash));
        } finally {
            Arrays.fill(characters, '\0');
        }
    }

    static boolean verify(String password, String saltHex, String expectedHashHex)
            throws GeneralSecurityException {
        byte[] salt = fromHex(saltHex);
        byte[] expectedHash = fromHex(expectedHashHex);
        char[] characters = password.toCharArray();
        try {
            byte[] actualHash = derive(characters, salt);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } finally {
            Arrays.fill(characters, '\0');
        }
    }

    private static byte[] derive(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec specification = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(specification)
                    .getEncoded();
        } finally {
            specification.clearPassword();
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    private static byte[] fromHex(String value) {
        if (value == null || value.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hexadecimal value");
        }

        byte[] result = new byte[value.length() / 2];
        for (int index = 0; index < value.length(); index += 2) {
            int high = Character.digit(value.charAt(index), 16);
            int low = Character.digit(value.charAt(index + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Invalid hexadecimal value");
            }
            result[index / 2] = (byte) ((high << 4) + low);
        }
        return result;
    }

    static final class PasswordHash {
        private final String salt;
        private final String hash;

        PasswordHash(String salt, String hash) {
            this.salt = salt;
            this.hash = hash;
        }

        String getSalt() {
            return salt;
        }

        String getHash() {
            return hash;
        }
    }
}
