package org.jboss.as.weld.deployment;


import java.net.URL;

import org.jboss.as.weld.WeldLogger;
import org.jboss.weld.xml.BeansXmlHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * {@link BeansXmlHandler} with AS7-specific logging.
 *
 * @author Jozef Hartinger
 *
 */
public class AS7BeansXmlHandler extends BeansXmlHandler {

    public AS7BeansXmlHandler(URL file) {
        super(file);
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        WeldLogger.DEPLOYMENT_LOGGER.beansXmlValidationWarning(file, e.getLineNumber(), e.getMessage());
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        if (e.getMessage().equals("cvc-elt.1: Cannot find the declaration of element 'beans'.")) {
            // Ignore the errors we get when there is no schema defined
            return;
        }
        WeldLogger.DEPLOYMENT_LOGGER.beansXmlValidationError(file, e.getLineNumber(), e.getMessage());
    }
}
