package it.geosolutions.urltesting.security;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;


public class UrlTestingTrustManager implements X509TrustManager
{

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
    {
        // System.out.println("checkClientTrusted with authType:" + authType);

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
    {
        // System.out.println("checkServerTrusted with authType:" + authType);
        // TODO, only load and check against valid loaded certificates

    }

    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
        return null;
    }

}
