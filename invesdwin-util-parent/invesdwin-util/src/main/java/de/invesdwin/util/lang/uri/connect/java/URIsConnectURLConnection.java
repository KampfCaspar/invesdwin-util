package de.invesdwin.util.lang.uri.connect.java;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import de.invesdwin.util.lang.Closeables;
import de.invesdwin.util.lang.uri.Addresses;
import de.invesdwin.util.lang.uri.URIs;
import de.invesdwin.util.lang.uri.connect.IHttpResponse;
import de.invesdwin.util.lang.uri.connect.IURIsConnect;
import de.invesdwin.util.lang.uri.connect.InputStreamHttpResponse;
import de.invesdwin.util.lang.uri.connect.InputStreamHttpResponseConsumer;
import de.invesdwin.util.time.duration.Duration;
import de.invesdwin.util.time.fdate.FTimeUnit;

@NotThreadSafe
public final class URIsConnectURLConnection implements IURIsConnect {

    private final URL url;
    private Duration networkTimeout = URIs.getDefaultNetworkTimeout();
    private Proxy proxy;

    private Map<String, String> headers;

    public URIsConnectURLConnection(final URI uri) {
        this.url = URIs.asUrl(uri);
    }

    @Override
    public URIsConnectURLConnection withNetworkTimeout(final Duration networkTimeout) {
        this.networkTimeout = networkTimeout;
        return this;
    }

    @Override
    public Duration getNetworkTimeout() {
        return networkTimeout;
    }

    @Override
    public URI getUri() {
        return URIs.asUri(url);
    }

    @Override
    public URIsConnectURLConnection withBasicAuth(final String username, final String password) {
        final String authString = username + ":" + password;
        final byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        final String authStringEnc = new String(authEncBytes);
        withHeader("Authorization", "Basic " + authStringEnc);
        return this;
    }

    @Override
    public URIsConnectURLConnection withHeader(final String key, final String value) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(key, value);
        return this;
    }

    @Override
    public URIsConnectURLConnection withProxy(final Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    @Override
    public Proxy getProxy() {
        return proxy;
    }

    @Override
    public boolean isServerResponding() {
        try {
            final Socket socket = new Socket();
            final int timeoutMillis = networkTimeout.intValue(FTimeUnit.MILLISECONDS);
            socket.setSoTimeout(timeoutMillis);
            socket.connect(Addresses.asAddress(url.getHost(), url.getPort()), timeoutMillis);
            socket.close();
            return true;
        } catch (final Throwable e) {
            return false;
        }
    }

    /**
     * Tries to open a connection to the specified url and checks if the content is available there.
     * 
     * TODO: There seems to be a openjdk bug when the wlan module gets reloaded. It seems this causes dns information to
     * get lost and causes UnknownHostExceptions here.
     */
    @Override
    public boolean isDownloadPossible() {
        if (url == null) {
            return false;
        }
        try {
            final URLConnection con = openConnection();
            return con.getInputStream().available() > 0;
        } catch (final Throwable e) {
            return false;
        }
    }

    @Override
    public long lastModified() {
        try {
            final URLConnection con = openConnection();
            return con.getLastModified();
        } catch (final IOException e) {
            return -1;
        }
    }

    @Override
    public String download() {
        InputStream in = null;
        try {
            in = getInputStream();
            return IOUtils.toString(in, Charset.defaultCharset());
        } catch (final Throwable e) {
            return null;
        } finally {
            Closeables.closeQuietly(in);
        }
    }

    @Override
    public String downloadThrowing() throws IOException {
        InputStream in = null;
        try {
            in = getInputStream();
            final String response = IOUtils.toString(in, Charset.defaultCharset());
            if (response == null) {
                throw new IOException("response is null");
            }
            return response;
        } finally {
            Closeables.closeQuietly(in);
        }
    }

    public URLConnection openConnection() throws IOException {
        final URLConnection con;
        if (proxy != null) {
            con = url.openConnection(proxy);
        } else {
            con = url.openConnection();
        }
        con.setUseCaches(false);
        con.setConnectTimeout(networkTimeout.intValue(FTimeUnit.MILLISECONDS));
        con.setReadTimeout(networkTimeout.intValue(FTimeUnit.MILLISECONDS));
        if (headers != null) {
            for (final Entry<String, String> header : headers.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }
        }
        return con;
    }

    @Override
    public InputStreamHttpResponse getInputStream() throws IOException {
        return getInputStream(openConnection());
    }

    public static InputStreamHttpResponse getInputStream(final URLConnection con) throws IOException {
        final IHttpResponse response;
        if (con instanceof HttpURLConnection) {
            final HttpURLConnection cCon = (HttpURLConnection) con;
            // https://stackoverflow.com/questions/613307/read-error-response-body-in-java
            final int respCode = cCon.getResponseCode();
            if (respCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                if (respCode == HttpURLConnection.HTTP_NOT_FOUND || respCode == HttpURLConnection.HTTP_GONE) {
                    throw new FileNotFoundException(con.getURL().toString());
                } else {
                    if (respCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                        final String errorStr;
                        try (InputStream error = cCon.getErrorStream()) {
                            errorStr = IOUtils.toString(error, Charset.defaultCharset());
                        } catch (final Throwable t) {
                            throw new IOException("Server returned HTTP" + " response code: " + respCode + " for URL: "
                                    + con.getURL().toString() + " failed to retrieve error response: <" + t.toString()
                                    + ">");
                        }
                        throw new IOException("Server returned HTTP" + " response code: " + respCode + " for URL: "
                                + con.getURL().toString() + " error response:" + "\n*****************************"
                                + errorStr + "*****************************");
                    } else {
                        throw new IOException("Server returned HTTP" + " response code: " + respCode + " for URL: "
                                + con.getURL().toString());
                    }
                }
            }
            final Map<String, List<String>> headers = cCon.getHeaderFields();
            response = new HttpResponseURLConnection(respCode, headers);
        } else {
            response = null;
        }
        final InputStreamHttpResponseConsumer consumer = new InputStreamHttpResponseConsumer();
        consumer.start(response);
        consumer.data(con.getInputStream());
        final InputStreamHttpResponse result = consumer.buildResult();
        consumer.releaseResources();
        return result;
    }

    @Override
    public String toString() {
        return url.toString();
    }

}
