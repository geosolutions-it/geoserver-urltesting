package it.geosolutions.urltesting.services.wfs;

import static org.junit.Assert.assertEquals;
import it.geosolutions.urltesting.HttpTest;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.methods.GetMethod;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class WFS20DescribeFeatureTypeTest extends HttpTest
{

    @Override
    protected void registerNamespaces(Map<String, String> namespaces)
    {
        super.registerNamespaces(namespaces);
        namespaces.put("wfs", "http://www.opengis.net/wfs/2.0");
        namespaces.put("fes", "http://www.opengis.net/fes/2.0");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("gml", "http://www.opengis.net/gml/3.2");
        namespaces.put("topp", "http://www.openplans.org/topp");
        namespaces.put("sf", "http://www.openplans.org/spearfish");
    }

    @Test
    public void testToppStates() throws Exception
    {
        GetMethod method =
            http("geoserver/wfs").kvp("version", "2.0").kvp(
                "service", "WFS").kvp(
                "request", "DescribeFeatureType").kvp("typeName", "topp:states").get();

        assertHttpResponse(200, method, "text/xml; subtype=gml/3.2");

        new DescribeFeatureTypeDocumentChecker(xpath, method).expectedFeatureTypes("statesType").expectedAttributes(
            "the_geom",
            "STATE_NAME",
            "STATE_FIPS",
            "SUB_REGION",
            "STATE_ABBR",
            "LAND_KM",
            "WATER_KM",
            "PERSONS",
            "FAMILIES",
            "HOUSHOLD",
            "MALE",
            "FEMALE",
            "WORKERS",
            "DRVALONE",
            "CARPOOL",
            "PUBTRANS",
            "EMPLOYED",
            "UNEMPLOY",
            "SERVICE",
            "MANUAL",
            "P_MALE",
            "P_FEMALE",
            "SAMP_POP").assertValiDescribeDocument();
    }

   
    class DescribeFeatureTypeDocumentChecker
    {
        String[] expectedFeatureTypes = new String[]
            {
                "statesType",
                "roadsType"
            };

        String[] expectedAttributes = new String[] {};

        private GetMethod method;

        private XpathEngine xpath;

        public DescribeFeatureTypeDocumentChecker(XpathEngine xpath, GetMethod method)
        {
            this.method = method;
            this.xpath = xpath;
        }

        public DescribeFeatureTypeDocumentChecker expectedFeatureTypes(String... expectedFeatureTypes)
        {
            this.expectedFeatureTypes = expectedFeatureTypes;

            return this;
        }

        public DescribeFeatureTypeDocumentChecker expectedAttributes(String... expectedAttributes)
        {
            this.expectedAttributes = expectedAttributes;

            return this;
        }

        public void assertValiDescribeDocument() throws IOException, ParserConfigurationException, SAXException,
            XpathException
        {
            // grab the dom
            Document dom = dom(method);
//            try {
//                              print(dom);
//                      } catch (Exception e) {
//                              // TODO Auto-generated catch block
//                              e.printStackTrace();
//                      }

            // some basic checks
            assertEquals("Check caps format", "http://www.opengis.net/gml/3.2", xpath.evaluate(
                    "//xsd:schema/xsd:import/@namespace",
                    dom));

            // available feature types
            NodeList featureTypeNames = xpath.getMatchingNodes(
                    "//xsd:schema/xsd:complexType/@name", dom);
            assertSetMatches(featureTypeNames, expectedFeatureTypes);

            // available feature attributes
            if ((expectedAttributes != null) && (expectedAttributes.length > 0))
            {
                NodeList featureAttributes = xpath.getMatchingNodes(
                        "//xsd:schema/xsd:complexType/xsd:complexContent/xsd:extension/xsd:sequence/xsd:element/@name",
                        dom);
                assertSetMatches(featureAttributes, expectedAttributes);
            }

        }
    }

}
