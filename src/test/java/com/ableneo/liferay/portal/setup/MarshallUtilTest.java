package com.ableneo.liferay.portal.setup;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarshallUtilTest {
    private File validConfiguration;
    private File invalidConfiguration;

    @BeforeEach
    void setup() {
        try {
            invalidConfiguration = new File(MarshallUtilTest.class.getResource("/invalid-configuration.xml").toURI());
            validConfiguration = new File(MarshallUtilTest.class.getResource("/valid-configuration.xml").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    void unmarshallInvalid() throws FileNotFoundException {
        assertNull(MarshallUtil.unmarshall(invalidConfiguration));
    }

    @Test
    void unmarshallValid() throws FileNotFoundException {
        assertNotNull(MarshallUtil.unmarshall(validConfiguration));
    }

    @Test
    void validateAgainstXSD() {
        assertTrue(MarshallUtil.validateAgainstXSD(validConfiguration));
        assertFalse(MarshallUtil.validateAgainstXSD(invalidConfiguration));
    }
}
