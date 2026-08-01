package com.example.techie;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PasswordHasherTest {

    @Test
    public void correctPasswordMatchesStoredHash() throws Exception {
        PasswordHasher.PasswordHash passwordHash =
                PasswordHasher.create("repair-pass-123");

        assertTrue(PasswordHasher.verify(
                "repair-pass-123",
                passwordHash.getSalt(),
                passwordHash.getHash()
        ));
    }

    @Test
    public void incorrectPasswordDoesNotMatchStoredHash() throws Exception {
        PasswordHasher.PasswordHash passwordHash =
                PasswordHasher.create("repair-pass-123");

        assertFalse(PasswordHasher.verify(
                "different-password",
                passwordHash.getSalt(),
                passwordHash.getHash()
        ));
    }

    @Test
    public void accountsUseDifferentRandomSalts() throws Exception {
        PasswordHasher.PasswordHash first = PasswordHasher.create("same-password");
        PasswordHasher.PasswordHash second = PasswordHasher.create("same-password");

        assertNotEquals(first.getSalt(), second.getSalt());
        assertNotEquals(first.getHash(), second.getHash());
    }
}
