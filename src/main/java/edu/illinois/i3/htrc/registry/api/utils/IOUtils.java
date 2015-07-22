package edu.illinois.i3.htrc.registry.api.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;

public abstract class IOUtils {

    /**
     * Opens the location from where to read.
     *
     * @param uri The location to read from (can be a URL or a local file)
     * @return The reader for this location
     * @throws IOException Thrown if a problem occurred when creating the reader
     */
    public static Reader getReaderForResource(URI uri) throws IOException  {
       return getReaderForResource(uri, 0, 0);
    }

    /**
     * Opens the location from where to read.
     *
     * @param uri The location to read from (can be a URL or a local file)
     * @param connectTimeout The connection timeout in ms (0 = infinite)
     * @param readTimeout The read timeout in ms (0 = infinite)
     * @return The reader for this location
     * @throws IOException Thrown if a timeout occurred or if a problem occurred when creating the reader
     */
    public static Reader getReaderForResource(URI uri, int connectTimeout, int readTimeout) throws IOException {
        return new InputStreamReader(StreamUtils.getInputStreamForResource(uri, connectTimeout, readTimeout), Charset.forName("UTF-8"));
    }

    /**
     * Gets a Writer that can be used to write to a resource
     *
     * @param uri The location to write to (can be URL or local file)
     * @param append True to append / False otherwise
     * @return The writer for this location
     * @throws IOException Thrown if a problem occurred when creating the writer
     */
    public static Writer getWriterForResource(URI uri, boolean append) throws IOException {
        return new OutputStreamWriter(StreamUtils.getOutputStreamForResource(uri, append), Charset.forName("UTF-8"));
    }

    /**
     * Gets a Writer that can be used to write to a resource
     *
     * @param uri The location to write to (can be URL or local file)
     * @return The writer for this location
     * @throws IOException Thrown if a problem occurred when creating the writer
     */
    public static Writer getWriterForResource(URI uri) throws IOException {
        return getWriterForResource(uri, false);
    }

    /**
     * @param uri The resource location (can be a URL or a local path)
     * @return The text contained in the resource
     * @throws IOException Thrown if a problem occurs when pulling data from the resource
     */
    public static String getTextFromReader(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        StringWriter writer = new StringWriter();

        char[] cbuf = new char[4096];
        int nRead;

        while ((nRead = br.read(cbuf)) > 0)
            writer.write(cbuf, 0, nRead);

        reader.close();
        return writer.toString();
    }

}