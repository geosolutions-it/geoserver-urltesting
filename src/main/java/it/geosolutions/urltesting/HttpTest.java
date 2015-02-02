package it.geosolutions.urltesting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import it.geosolutions.urltesting.security.UrlTestingSSLSocketFactory;

import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
//import com.digitalglobe.online.urltesting.security.UrlTestingSSLSocketFactory;
import net.sf.json.JSON;


@RunWith(HttpReportRunner.class)
public class HttpTest
{
    public static final Pattern PATTERN = Pattern.compile(":\\/*\\/*");

    // this part setups and tear down the http client, so that we do that only once per test class and
    // thus allow it to reuse http connections (https ones are expensive to setup)

    private static HttpClient client;

    private static String host;

    private static String outputFolder;

    private static String testDate;

    @BeforeClass
    public static void setUpAll() throws Exception
    {
        // setup the ssl factory so that we can use self signed certs
        Security.setProperty("ssl.SocketFactory.provider", UrlTestingSSLSocketFactory.class.getName());

        // setup the host
        host = System.getProperty("host", "http://localhost:8080");
        if (host.endsWith("/"))
        {
            host = host.substring(0, host.length() - 1);
        }

        // see if we need to record the test
        outputFolder = System.getProperty("output", null);
        testDate =
            new Formatter(new StringBuilder(), Locale.US).format("%1$tY-%1$tm-%1$tdT%1$tH%1$tM%1$tS",
                System.currentTimeMillis()).toString();
    }

    @AfterClass
    public static void tearDownAll() throws Exception
    {
        if ((client != null) && (client.getHttpConnectionManager() != null))
        {
            client.getHttpConnectionManager().closeIdleConnections(1000);

        }
    }

    // create a per-test logger
    protected Logger LOGGER = Logger.getLogger(getClass());
   

    /**
     * Used for basic auth
     */
    private String username, pw;

    /**
     * Used by the listener to save requests and the like
     */
    @Rule
    public TestName name = new TestName();

    /**
     * Used to record calls and collect http methods to be closed
     */
    protected HttpRequestListener listener = new HttpRequestListener();

    /**
     *
     */
    private List<HttpMethod> methods = new ArrayList<HttpMethod>();

    /**
     * Xpath engine the tests can
     */
    protected XpathEngine xpath;

    /**
     * Ctor for no authentication
     */
    public HttpTest()
    {
        
    }

    /**
     * Ctor for basic auth authentication
     */
    public HttpTest(String username, String pw)
    {
        this.username = username;
        this.pw = pw;
    }

    @Before
    public void setUp() throws Exception
    {
        // configure xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();

        // setup recording if necessary
        listener.start(outputFolder);

        System.setProperty("javax.net.debug", "true");
    }


    @After
    public void tearDown() throws Exception
    {
        listener.stop();

        // make sure we released all http connections
        for (HttpMethod method : methods)
        {
            if (method != null)
            {
                method.releaseConnection();
            }
        }
        methods.clear();
    }

    protected void registerNamespaces(Map<String, String> namespaces)
    {
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    }

    /**
     * Builds the http client
     *
     * @return
     */
    protected HttpClient getHttpClient(String url)
    {
        if (client == null)
        {
            client = new HttpClient();

            if ((username != null) || (pw != null))
            {
                setupBasicAuth(url);
            }

            if (url != null)
            {
                String[] chunks = PATTERN.split(url);
                if (chunks.length < 2)
                {
                    throw new RuntimeException("invalid url supplied as base url: " + url);
                }

                int port = 80;
                if (chunks.length == 2)
                {
                    if (chunks[0].equalsIgnoreCase("https"))
                    {
                        port = 443;
                    }
                }
                else
                {
                    port = Integer.parseInt(chunks[2]);
                }

                client.getHostConfiguration().setHost(chunks[1], port, chunks[0]);

            }

        }

        return client;
    }

    /**
     * Returns a JMXClient connected to the specified host and port
     * @param host
     * @param port
     * @return
     
    protected JMXClient jmx(String host, int port) throws IOException
    {
        return new JMXClient(host, port);
    }*/


    
    /**
     * Returns a new fluent request builder
     */
    protected HTTPRequestBuilder http(String path)
    {
        if (!path.startsWith("/"))
        {
            path = "/" + path;
        }

        HTTPRequestBuilder builder = new HTTPRequestBuilder(getHttpClient(host), path);
        builder.setListener(listener);

        return builder;
    }


    /**
     * Performs basic checks over a http response
     */
    protected void assertHttpResponse(int status, HttpMethod method, String... contentTypes)
    {
        assertEquals("Unexpected http status", status, method.getStatusCode());
        assertTrue("Unexpected content type: " + method.getResponseHeader("Content-Type").getValue() + ", allowed: " + Arrays.asList(contentTypes),
            Arrays.asList(contentTypes).contains(method.getResponseHeader("Content-Type").getValue()));
    }

    protected Document dom(HttpMethod method) throws IOException, ParserConfigurationException, SAXException
    {
        return dom(method, true);
    }

    protected Document dom(HttpMethod method, boolean skipDTD) throws IOException, ParserConfigurationException,
        SAXException
    {
        InputStream input = method.getResponseBodyAsStream();
        if (skipDTD)
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(new EmptyResolver());

            Document dom = builder.parse(input);

            return dom;
        }
        else
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(input);
        }

    }

    /**
     * Utility method to print out a dom.
     */
    protected void print(Document dom) throws Exception
    {
        TransformerFactory txFactory = TransformerFactory.newInstance();
        try
        {
            txFactory.setAttribute("{http://xml.apache.org/xalan}indent-number", new Integer(2));
        }
        catch (Exception e)
        {
            // some
        }

        Transformer tx = txFactory.newTransformer();
        tx.setOutputProperty(OutputKeys.METHOD, "xml");
        tx.setOutputProperty(OutputKeys.INDENT, "yes");
        tx.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        tx.transform(new DOMSource(dom), new StreamResult(new OutputStreamWriter(System.out,
                    "utf-8")));
    }

    protected RenderedImage image(HttpMethod method) throws IOException
    {
        // this will work for most images, but not for JP2K ones for example,
        // we'd need to check the content type and do something different in that
        // case
        return ImageIO.read(method.getResponseBodyAsStream());
    }

    /**
     * Dumps the method stream contents into a file with the specified name, located
     * in <code>./target/tmp/path/to/test/class/testMethodName</code>
     * @param method
     * @param fileName
     * @return
     * @throws IOException
     */
    protected File file(HttpMethod method, String fileName) throws IOException
    {
        File file = getTestFilePath(fileName);
        if (!file.getParentFile().exists())
        {
            if (!file.getParentFile().mkdirs())
            {
                throw new IOException("Could not create output file parent folder " + file.getParent());
            }
        }

        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(file);
            IOUtils.copy(method.getResponseBodyAsStream(), fos);
        }
        finally
        {
            IOUtils.closeQuietly(fos);
        }

        return file;
    }

    /**
     * Builds a full path for the file using the test method and class name to build
     * a folder structure
     *
     * @param fileName
     * @return
     */
    private File getTestFilePath(String fileName)
    {
        try
        {
            throw new Exception();
        }
        catch (Exception e)
        {
            // look for the stack trace element that has the @Test annotation on it, it's the test
            StackTraceElement testElement = null;
            StackTraceElement[] stackTraces = e.getStackTrace();
            for (StackTraceElement st : stackTraces)
            {
                try
                {
                    Annotation[] annotations =
                        Class.forName(st.getClassName()).getMethod(st.getMethodName()).getAnnotations();
                    for (Annotation annotation : annotations)
                    {
                        if (Test.class.equals(annotation.annotationType()))
                        {
                            testElement = st;

                            break;
                        }
                    }
                    if (testElement != null)
                    {
                        break;
                    }
                }
                catch (NoSuchMethodException nsm)
                {
                    // fine, we look for methods that have no params but we can't know from the stack trace
                }
                catch (ClassNotFoundException cnf)
                {
                    throw new RuntimeException("Unexepected failure while looking up stack trace class names", cnf);
                }
            }
            if (testElement != null)
            {
                String fullClass = testElement.getClassName();
                String folderName = fullClass.replace(".", File.separator) +
                    File.separator + testElement.getMethodName();

                return new File("./target/tmp/" + folderName, fileName);
            }
            else
            {
                LOGGER.warn("Warning, could not find a method annotated with @Test, " +
                    "using ./target/tmp as the parent folder for " + fileName);

                return new File("./target/tmp/" + fileName);
            }
        }
    }


    protected JSONObject json(HttpMethod method) throws IOException
    {
        return (JSONObject) JSONSerializer.toJSON(method.getResponseBodyAsString());
    }

    protected JSONArray jsonArray(HttpMethod method) throws IOException
    {
        return (JSONArray) JSONSerializer.toJSON(method.getResponseBodyAsString());
    }

    /**
     * Utility method to print out the contents of a json object.
     */
    protected void print(JSON json)
    {
        System.out.println(json.toString(2));
    }

    private void setupBasicAuth(String url) throws RuntimeException
    {
        LOGGER.info("Basic auth info found: trying to setup httpclient");
        try
        {
            URL httpurl = new URL(url);
            String host = httpurl.getHost();
            int port = httpurl.getPort();
            if (port == -1)
            {
                LOGGER.info("Port not explicitly set: guessing...");
                if (httpurl.getProtocol().equals("http"))
                {
                    port = 80;
                }
                else if (httpurl.getProtocol().equals("https"))
                {
                    port = 443;
                }
                else
                {
                    throw new RuntimeException("can't guess a valid port for url: " + url);
                }
            }

            client.getState().setCredentials(
                new AuthScope(host, port),
                new UsernamePasswordCredentials(username, pw));

        }
        catch (MalformedURLException ex)
        {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Checks the provided node list text contents matches the list of expected values (as a set, order is not important)
     * @param nodes
     * @param expectedValueArray
     */
    protected void assertSetMatches(NodeList nodes, String... expectedValueArray)
    {
        Set<String> expected = new HashSet<String>(Arrays.asList(expectedValueArray));
        for (int i = 0; i < nodes.getLength(); i++)
        {
            String text = nodes.item(i).getTextContent();
            boolean found = expected.remove(text);
            assertTrue(text + " was not expected", found);
        }

        assertTrue("Expected values were not found: " + expected, expected.isEmpty());
    }


    /**
     * Resolves everything to an empty xml document, useful for skipping errors due to missing dtds
     * and the like
     */
    static class EmptyResolver implements org.xml.sax.EntityResolver
    {
        public InputSource resolveEntity(String publicId, String systemId) throws org.xml.sax.SAXException, IOException
        {
            StringReader reader = new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            InputSource source = new InputSource(reader);
            source.setPublicId(publicId);
            source.setSystemId(systemId);

            return source;
        }
    }

    /**
     * Listens to http requests and performs various tasks:
     * <ul>
     * <li>stores all the methods so that we're sure they are going to release the connection</li>
     * <li>eventually dumps all requests and responses on disk</li>
     * @author aaime
     *
     */
    class HttpRequestListener implements HTTPRequestBuilder.Listener
    {
        File methodFolder;
        BufferedWriter bw;
        int count;

        public void start(String outputFolder) throws IOException
        {
            count = 0;
            if (outputFolder != null)
            {
                File classFolder = new File(outputFolder, testDate + "-" + HttpTest.this.getClass().getSimpleName());
                methodFolder = new File(classFolder, name.getMethodName());
                if (!methodFolder.exists() && !methodFolder.mkdirs())
                {
                    throw new IOException("Could not create request dumping folder " + methodFolder.getAbsolutePath());
                }
                bw = new BufferedWriter(new FileWriter(new File(methodFolder, "requests.txt")));
            }
        }

        public void stop() throws IOException
        {
            if (bw != null)
            {
                bw.flush();
                bw.close();
                bw = null;
            }
        }

        @Override
        public void beforeRequest(HttpClient client, HttpMethod method) throws IOException
        {
            count++;
            if (bw != null)
            {
                bw.write(count + ") " + client.getHostConfiguration().getHostURL() + method.getURI().toString());
                bw.newLine();
                for (Header header : method.getRequestHeaders())
                {
                    bw.write("   " + header.getName() + ": " + header.getValue());
                    bw.newLine();
                }
            }
        }

        @Override
        public void afterRequest(HttpClient client, HttpMethod method) throws IOException
        {
            // dump the request if necessary
            if (bw != null)
            {
                // write out the response details
                bw.write("Response: " + method.getStatusCode() + " " + method.getStatusText());
                bw.newLine();
                for (Header header : method.getResponseHeaders())
                {
                    bw.write("   " + header.getName() + ": " + header.getValue());
                    bw.newLine();
                }
                bw.newLine();

                // dump out the output file if necessary
                File outputFile = getOutputFile(method);
                byte[] contents = method.getResponseBody();
                FileOutputStream fos = null;
                try
                {
                    fos = new FileOutputStream(outputFile);
                    fos.write(contents);
                }
                finally
                {
                    if (fos != null)
                    {
                        fos.close();
                    }
                }
            }

            // store the method so that we can be sure we're closing it
            methods.add(method);

            HttpReportRunner.lastMethod = method;
            HttpReportRunner.lastURL = client.getHostConfiguration().getHostURL() + method.getURI().toString();
        }

        private File getOutputFile(HttpMethod method)
        {
            // get the content type
            String contentType = method.getResponseHeader("Content-Type").getValue();

            // heuristics to extract the file extension
            String extension = ".bin";
            if (contentType != null)
            {
                String lct = contentType.toLowerCase();
                if (lct.startsWith("multipart"))
                {
                    extension = ".multipart";
                }
                else if (lct.contains("xml"))
                {
                    extension = ".xml";
                }
                else if (lct.contains("png"))
                {
                    extension = ".png";
                }
                else if (lct.contains("html"))
                {
                    extension = ".html";
                }
                else if (lct.contains("text"))
                {
                    extension = ".txt";
                }
                else if (lct.contains("jpg") || lct.contains("jpeg"))
                {
                    extension = ".jpg";
                }
            }

            return new File(methodFolder, count + extension);
        }

    }


}
