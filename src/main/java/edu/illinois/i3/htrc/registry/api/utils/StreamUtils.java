package edu.illinois.i3.htrc.registry.api.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

public abstract class StreamUtils {
    public static int DEFAULT_BUFFER_SIZE = 65536;

    /**
     * Reads the content of an InputStream into a byte array
     *
     * @param dataStream The data stream
     * @return A byte array containing the data from the data stream
     * @throws IOException Thrown if a problem occurred while reading from the stream
     */
    public static byte[] getBytesFromStream(InputStream dataStream) throws IOException {
        InputStream bufStream = (dataStream instanceof BufferedInputStream) ?
                dataStream : new BufferedInputStream(dataStream);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        copyStream(bufStream, baos, DEFAULT_BUFFER_SIZE);

        return baos.toByteArray();
    }

    /**
     * Returns an InputStream for the specified resource.
     *
     * @param uri The resource location (can be a URL or a local file)
     * @return The InputStream to use to read from the resource
     * @throws IOException Thrown if the resource is invalid, does not exist, or cannot be opened
     */
    public static InputStream getInputStreamForResource(URI uri) throws IOException {
        return getInputStreamForResource(uri, 0, 0);
    }

    /**
     * Returns an InputStream for the specified resource.
     *
     * @param uri The resource location (can be a URL or a local file)
     * @param connectTimeout The connection timeout in ms (0 = infinite)
     * @param readTimeout The read timeout in ms (0 = infinite)
     * @return The InputStream to use to read from the resource
     * @throws IOException Thrown if the resource is invalid, does not exist, cannot be opened, or if a timeout occurred
     */
    public static InputStream getInputStreamForResource(URI uri, int connectTimeout, int readTimeout) throws IOException {
        URLConnection connection = getURLforResource(uri).openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        return connection.getInputStream();
    }

    /**
     * Returns an OutputStream for the specified resource
     *
     * @param uri The resource location (specified as either file:// or local path)
     * @param append True to append / False otherwise
     * @return The OutputStream to use to write to the resource
     * @throws IOException Thrown if the resource is invalid
     */
    public static OutputStream getOutputStreamForResource(URI uri, boolean append) throws IOException {
        URL url = getURLforResource(uri);

        if (url.getProtocol().equalsIgnoreCase("file"))
            try {
                return new FileOutputStream(new File(url.toURI()), append);
            }
            catch (URISyntaxException e) {
                // should never happen
                throw new RuntimeException(e);
            }
        else
            // TODO: add webdav support
            throw new UnsupportedOperationException("Can only write to file:// or local resources");
    }

    /**
     * Returns an OutputStream for the specified resource
     *
     * @param uri The resource location (specified as either file:// or local path)
     * @return The OutputStream to use to write to the resource
     * @throws IOException Thrown if the resource is invalid
     */
    public static OutputStream getOutputStreamForResource(URI uri) throws IOException {
        return getOutputStreamForResource(uri, false);
    }

    /**
     * Creates a URL object corresponding to a URI
     *
     * @param uri The URI (can reference URLs and local files)
     * @return The URL object
     * @throws MalformedURLException
     */
    public static URL getURLforResource(URI uri) throws MalformedURLException {
        try  {
            return uri.toURL();
        }
        catch (IllegalArgumentException e) {
            // URI not absolute - trying as local file
            try {
                return new File(URLDecoder.decode(uri.toString(), "UTF-8")).toURI().toURL();
            }
            catch (UnsupportedEncodingException e1) {
                // should never happen
                throw new RuntimeException(e1);
            }
        }
    }

    /**
     * Writes a resource resolvable through the specified class to an output stream
     *
     * @param clazz The class used to resolve the resource
     * @param resourceName The resource name
     * @param outputStream The output stream to write to
     * @throws IOException
     */
    public static void writeClassResourceToStream(Class<?> clazz, String resourceName, OutputStream outputStream) throws IOException {
        InputStream resStream = clazz.getClassLoader().getResourceAsStream(resourceName);
        if (resStream == null)
            throw new FileNotFoundException(resourceName);

        copyStream(resStream, outputStream, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Reads the data read from one stream and writes that data out to another stream
     *
     * @param inputStream The input stream
     * @param outputStream The output stream
     * @param bufferSize The buffer size to use
     * @throws IOException
     */
    public static void copyStream(InputStream inputStream, OutputStream outputStream, int bufferSize) throws IOException {
        int len;
        byte[] buffer = new byte[bufferSize];

        while ((len = inputStream.read(buffer)) != -1)
            outputStream.write(buffer, 0, len);
    }
}
