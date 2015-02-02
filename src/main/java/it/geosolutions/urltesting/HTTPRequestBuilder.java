package it.geosolutions.urltesting;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 * TODO: add javadocs, logging, request and response recording
 */
public class HTTPRequestBuilder
{

    Logger LOGGER = Logger.getLogger(HTTPRequestBuilder.class);

    HttpClient client;

    String url;

    List<Header> headers = new ArrayList<Header>();

    List<NameValuePair> kvps = new ArrayList<NameValuePair>();

    List<NameValuePair> form = new ArrayList<NameValuePair>();

    RequestEntity postEntity;

    Listener listener;

    public HTTPRequestBuilder(HttpClient client, String url, String connectId)
    {
        this(client, url);
        this.kvps.add(new NameValuePair("connectid", connectId));
    }

    /**
     * Creates an HTTPRequestBuilder without auth info.
     */
    public HTTPRequestBuilder(HttpClient client, String url)
    {
        this.url = url;
        this.client = client;
    }

    /**
     * Removes connectid param.
     */
    public HTTPRequestBuilder noAuth()
    {
        for (Iterator<NameValuePair> it = kvps.iterator(); it.hasNext();)
        {
            NameValuePair nameValuePair = it.next();
            if (nameValuePair.getName().equals("connectid"))
            {
                LOGGER.info("Removing connectId kvp");
                it.remove();
            }
        }

        return this;
    }


    /**
     * @param sink the sink to set
     */
    public void setListener(Listener sink)
    {
        this.listener = sink;
    }

    public HTTPRequestBuilder contentType(String contentType)
    {
        this.headers.add(new Header("Content-type", contentType));

        return this;
    }

    public HTTPRequestBuilder kvp(String key, String value)
    {
        this.kvps.add(new NameValuePair(key, value));

        return this;
    }

    /**
     * Specify all query parameters in one shot as k1=v1&k2=v2&...
     */
    public HTTPRequestBuilder queryString(String queryString)
    {
        StringTokenizer st = new StringTokenizer(queryString, "&");
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            String[] keyValuePair = token.split("=");

            // check for any special characters
            if (keyValuePair.length > 1)
            {
                // replace any equals or & characters
                try
                {
                    // if this one does not work first check if the url encoded content is really
                    // properly encoded. I had good success with this: http://meyerweb.com/eric/tools/dencoder/
                    keyValuePair[1] = URLDecoder.decode(keyValuePair[1], "ISO-8859-1");
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new RuntimeException("Totally unexpected", e);
                }

            }

            kvps.add(new NameValuePair(keyValuePair[0], (keyValuePair.length > 1) ? keyValuePair[1] : ""));
        }

        return this;
    }

    public HTTPRequestBuilder form(String key, String value)
    {
        this.form.add(new NameValuePair(key, value));

        return this;
    }

    public HTTPRequestBuilder header(String name, String value)
    {
        this.headers.add(new Header(name, value));

        return this;

    }

    public GetMethod get() throws HttpException, IOException
    {
        GetMethod method = new GetMethod(url);
        configureMethod(method);

        if (listener != null)
        {
            listener.beforeRequest(client, method);
        }

        LOGGER.log(Level.INFO, "Sending GET request to " + method.getURI());

        if (client.getState().getCredentials(AuthScope.ANY) != null)
        {
            method.setDoAuthentication(true);
        }

        try
        {
            client.executeMethod(method);
        }
        finally
        {
            if (listener != null)
            {
                listener.afterRequest(client, method);
            }
        }

        return method;
    }

    public HTTPRequestBuilder body(String body)
    {
        return body(body, null);
    }

    public HTTPRequestBuilder body(String body, String contentType)
    {
        try
        {
            this.postEntity = new StringRequestEntity(body, contentType, "UTF-8");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return this;
    }

    public HTTPRequestBuilder body(File file)
    {
        this.postEntity = new FileRequestEntity(file, null);

        return this;
    }

    public HTTPRequestBuilder body(InputStream stream)
    {
        this.postEntity = new InputStreamRequestEntity(stream);

        return this;
    }

    public HTTPRequestBuilder body(byte[] body)
    {
        this.postEntity = new ByteArrayRequestEntity(body);

        return this;
    }

    public HTTPRequestBuilder body(RequestEntity entity)
    {
        this.postEntity = entity;

        return this;
    }


    public PostMethod post() throws HttpException, IOException
    {
        PostMethod method = new PostMethod(url);
        configureMethod(method);
        if (listener != null)
        {
            listener.beforeRequest(client, method);
        }

        // run the request
        method.setRequestEntity(postEntity);
        try
        {
            client.executeMethod(method);
        }
        finally
        {
            if (listener != null)
            {
                listener.afterRequest(client, method);
            }
        }

        return method;
    }

    private void configureMethod(HttpMethodBase method)
    {
        // setup the query string (the connectid at the very least)
        NameValuePair[] nvp = (NameValuePair[]) kvps.toArray(new NameValuePair[kvps.size()]);
        method.setQueryString(nvp);

        // add the headers
        for (Header header : headers)
        {
            method.addRequestHeader(header);
        }
    }

    /**
     * Allows to collect and modify requests
     */
    interface Listener
    {

        /**
         * Invoked before the request is made
         */
        public void beforeRequest(HttpClient client, HttpMethod method) throws IOException;

        /**
         * Invoked once the request is made
         * @param client
         * @param method
         * @throws IOException
         */
        public void afterRequest(HttpClient client, HttpMethod method) throws IOException;
    }
}
