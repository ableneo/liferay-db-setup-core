package com.ableneo.liferay.portal.setup;

import static org.junit.jupiter.api.Assertions.*;

import com.ableneo.liferay.portal.setup.domain.PageType;
import com.ableneo.liferay.portal.setup.domain.Setup;
import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarshallUtilTest extends ValidSetupTestMocks {

    private File validConfiguration;
    private File invalidConfiguration;
    private File validPageSequenceConfiguration;
    private File invalidPageSequenceConfiguration;

    @BeforeEach
    void setup() {
        try {
            invalidConfiguration = new File(MarshallUtilTest.class.getResource("/invalid-configuration.xml").toURI());
            validConfiguration = new File(MarshallUtilTest.class.getResource("/valid-configuration.xml").toURI());
            validPageSequenceConfiguration = new File(
                MarshallUtilTest.class.getResource("/valid-configuration-page-sequence.xml").toURI()
            );
            invalidPageSequenceConfiguration = new File(
                MarshallUtilTest.class.getResource("/invalid-configuration-page-sequence.xml").toURI()
            );
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

    @Test
    void shouldUnmarshallPageSequenceWhenAttributeIsPresent() throws FileNotFoundException {
        Setup setup = MarshallUtil.unmarshall(validPageSequenceConfiguration);

        assertNotNull(setup);
        List<PageType> pages = setup.getSites().getSite().get(0).getPublicPages().getPage();
        assertEquals(BigInteger.ZERO, pages.get(0).getPageSequence());
        assertEquals(BigInteger.valueOf(2), pages.get(1).getPageSequence());
        assertNull(pages.get(2).getPageSequence());
    }

    @Test
    void shouldRejectConfigurationWhenPageSequenceIsNegative() {
        assertTrue(MarshallUtil.validateAgainstXSD(validPageSequenceConfiguration));
        assertFalse(MarshallUtil.validateAgainstXSD(invalidPageSequenceConfiguration));
    }
}
