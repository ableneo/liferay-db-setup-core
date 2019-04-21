package com.ableneo.liferay.portal.setup;

/*
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Copyright (C) 2016 - 2019 ableneo s. r. o.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.*;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.ableneo.liferay.portal.setup.domain.ObjectFactory;
import com.ableneo.liferay.portal.setup.domain.Setup;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

public final class MarshallUtil {
    private static final Log LOG = LogFactoryUtil.getLog(MarshallUtil.class);

    private static final SAXParserFactory spf = SAXParserFactory.newInstance();
    private static final Schema schema = getSchema();
    private static XMLReader xr = null;
    private static Unmarshaller unmarshaller = getUnmarshaller();

    static {
        spf.setNamespaceAware(true);
        try {
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            xr = spf.newSAXParser().getXMLReader();
        } catch (SAXException | ParserConfigurationException e) {
            LOG.error(e);
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
        if (validateAgainstXSD(xmlConfigurationFile)) {
            return MarshallUtil.unmarshall(new FileInputStream(xmlConfigurationFile));
        } else {
            return null;
        }
    }

    private static Setup unmarshall(final FileInputStream stream) {
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
            return jc.createUnmarshaller();
        } catch (JAXBException e) {
            LOG.error("db-setup-core library is broken in unexpected way. Please fix the library.", e);
        }
        return null;
    }

    /**
     * @throws IllegalStateException
     *         Code of db-setup-core is broken. Please fix the library.
     * @throws NullPointerException If <code>source</code> is
     *         <code>null</code>.
     * @return if provided configuration is valid
     *
     */
    public static boolean validateAgainstXSD(final File xmlConfigurationFile) {
        Validator validator = schema.newValidator();
        try {
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, StringPool.BLANK);
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, StringPool.BLANK);
            validator.validate(new StreamSource(new FileInputStream(xmlConfigurationFile)));
        } catch (IOException e) {
            throw new IllegalStateException("db-setup-core is broken in unexpected manner. Please fix the library.", e);
        } catch (SAXException e) {
            return false;
        }
        return true;
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
            throw new IllegalStateException("XSD schema that is used for configuration validation cannot be parsed");
        }
        return schema;
    }
}
