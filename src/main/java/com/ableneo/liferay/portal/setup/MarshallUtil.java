package com.ableneo.liferay.portal.setup;

import com.ableneo.liferay.portal.setup.domain.ObjectFactory;
import com.ableneo.liferay.portal.setup.domain.Setup;
import com.liferay.petra.string.StringPool;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public final class MarshallUtil {

    private static final Logger LOG = LoggerFactory.getLogger(MarshallUtil.class);

    private static final SAXParserFactory spf = SAXParserFactory.newInstance();
    private static final Schema schema = getSchema();
    private static XMLReader xr = null;
    private static Unmarshaller unmarshaller = getUnmarshaller();

    private static boolean skipValidate = false;

    static {
        spf.setNamespaceAware(true);
        try {
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            xr = spf.newSAXParser().getXMLReader();
        } catch (SAXException | ParserConfigurationException e) {
            LOG.error("Failed to setup SAX parser.", e);
        }
    }

    private MarshallUtil() {
        // the class should not be instantiated
    }

    /**
     * @param xmlConfigurationFile file with db setup configuration that validates against schema
     * @return valid Setup object or null, in case null is returned please inspect logs
     * @throws FileNotFoundException when provided file cannot be found
     */
    public static Setup unmarshall(final File xmlConfigurationFile) throws FileNotFoundException {
        return unmarshall(new FileInputStream(xmlConfigurationFile));
    }

    public static Setup unmarshall(final InputStream stream) {
        try {
            SAXSource src = new SAXSource(xr, new InputSource(stream));
            return (Setup) unmarshaller.unmarshal(src);
        } catch (JAXBException e) {
            LOG.error("Cannot unmarshall the provided stream", e);
        }
        return null;
    }

    private static Unmarshaller getUnmarshaller() {
        ClassLoader cl = ObjectFactory.class.getClassLoader();
        JAXBContext jc;
        try {
            jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName(), cl);
            final Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshaller.setSchema(schema);
            return unmarshaller;
        } catch (Exception e) {
            LOG.error("db-setup-core library is broken in unexpected way. Please fix the library.", e);
        }
        return null;
    }

    public static void toXmlStdOut(Setup setup) {
        toXmlStream(setup, System.out);
    }

    public static void toXmlStream(Setup setup, OutputStream os) {
        Marshaller m = getMarshaller();
        try {
            if (MarshallUtil.skipValidate) {
                m.setEventHandler(event -> {
                    return true; //all-valid
                });
            }
            m.marshal(setup, os);
        } catch (JAXBException e) {
            LOG.error("Could not convert from xml", e);
        }
    }

    public static void skipValidate(boolean skipValidate) {
        MarshallUtil.skipValidate = skipValidate;
    }

    private static Marshaller getMarshaller() {
        ClassLoader cl = ObjectFactory.class.getClassLoader();
        JAXBContext jc;
        try {
            jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName(), cl);
            final Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setSchema(schema);
            return marshaller;
        } catch (JAXBException e) {
            throw new IllegalStateException(
                "db-setup-core library is broken in unexpected way. Please fix the library.",
                e
            );
        }
    }

    /**
     * @return if provided configuration is valid
     * @throws IllegalStateException Code of db-setup-core is broken. Please fix the library.
     * @throws NullPointerException  If <code>source</code> is
     *                               <code>null</code>.
     */
    public static boolean validateAgainstXSD(final InputStream inputStream) {
        Validator validator = schema.newValidator();
        try {
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, StringPool.BLANK);
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, StringPool.BLANK);
            validator.validate(new StreamSource(inputStream));
        } catch (IOException e) {
            throw new IllegalStateException("db-setup-core is broken in unexpected manner. Please fix the library.", e);
        } catch (SAXException e) {
            LOG.error("Parsing error", e);
            return false;
        }
        return true;
    }

    /**
     * @return if provided configuration is valid
     * @throws IllegalStateException Code of db-setup-core is broken. Please fix the library.
     * @throws NullPointerException  If <code>source</code> is
     *                               <code>null</code>.
     */
    public static boolean validateAgainstXSD(final File xmlConfigurationFile) {
        try {
            return validateAgainstXSD(new FileInputStream(xmlConfigurationFile));
        } catch (IOException e) {
            throw new IllegalStateException("db-setup-core is broken in unexpected manner. Please fix the library.", e);
        }
    }

    private static Schema getSchema() {
        ClassLoader cl = MarshallUtil.class.getClassLoader();
        InputStream schemaInputStream = cl.getResourceAsStream("setup_definition.xsd");
        if (schemaInputStream == null) {
            throw new IllegalStateException("XSD schema that is used for validation configuration not found");
        }

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema;
        try {
            schema = factory.newSchema(new StreamSource(schemaInputStream));
        } catch (SAXException e) {
            throw new IllegalStateException("XSD schema that is used for configuration validation cannot be parsed", e);
        }
        return schema;
    }
}
