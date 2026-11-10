package com.limelight;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Build contract scoped to the independently installed nonRootDebug touch APK. */
public class TouchBuildIdentityTest {
    @Test
    public void touchDebugDoesNotReplaceOfficialApp() {
        assertEquals("com.donnowhoo.moonlight.touch.debug", BuildConfig.APPLICATION_ID);
        assertFalse(BuildConfig.ROOT_BUILD);
        assertTrue(BuildConfig.DEBUG);
    }

    @Test
    public void touchVersionIdentifiesSource() {
        assertTrue(BuildConfig.VERSION_NAME.matches("12\\.2-touch\\.([0-9a-f]{8}|local)"));
        assertTrue(BuildConfig.VERSION_CODE >= 1000);
    }
}
