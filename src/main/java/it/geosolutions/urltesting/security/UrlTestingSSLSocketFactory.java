package it.geosolutions.urltesting.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;


public class UrlTestingSSLSocketFactory extends SSLSocketFactory
{

    public static SocketFactory getDefault()
    {
        return new UrlTestingSSLSocketFactory();
    }

    private SSLSocketFactory factory;

    public UrlTestingSSLSocketFactory()
    {
        try
        {
            SSLContext sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(null, // No KeyManager required
                new TrustManager[] { new UrlTestingTrustManager() }, new java.security.SecureRandom());
            factory = (SSLSocketFactory) sslcontext.getSocketFactory();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean flag) throws IOException
    {
        return factory.createSocket(socket, s, i, flag);
    }

    @Override
    public Socket createSocket(InetAddress inaddr, int i, InetAddress inaddr1, int j) throws IOException
    {
        return factory.createSocket(inaddr, i, inaddr1, j);
    }

    @Override
    public Socket createSocket(InetAddress inaddr, int i) throws IOException
    {
        return factory.createSocket(inaddr, i);
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inaddr, int j) throws IOException
    {
        return factory.createSocket(s, i, inaddr, j);
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException
    {
        return factory.createSocket(s, i);
    }

    @Override
    public String[] getDefaultCipherSuites()
    {
        return factory.getSupportedCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites()
    {
        return factory.getSupportedCipherSuites();
    }

}
