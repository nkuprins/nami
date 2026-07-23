package com.app.backend.util;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal streaming (StAX) reader for VZD cadastre XML exports: repeating
 * item elements (e.g. {@code BuildingItemData}), each holding a handful of
 * leaf fields nested a few levels deep. Only the requested item element is
 * buffered in memory at a time, since a single export file can be hundreds
 * of megabytes.
 */
public final class XmlStream implements AutoCloseable {

    private final XMLStreamReader reader;
    private final String itemLocalName;

    private XmlStream(XMLStreamReader reader, String itemLocalName) {
        this.reader = reader;
        this.itemLocalName = itemLocalName;
    }

    public static XmlStream open(InputStream in, String itemLocalName) throws XMLStreamException {
        return new XmlStream(safeReader(in), itemLocalName);
    }

    /**
     * A StAX reader with DTD/external-entity resolution disabled. Callers that need
     * the flat {@code Map} view use {@link #open}; callers with a nested/repeating
     * shape (e.g. parcels) drive this reader directly. Either way the XXE hardening
     * lives here: the files are fetched over the network and re-parsed each ingest.
     */
    public static XMLStreamReader safeReader(InputStream in) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory.createXMLStreamReader(in);
    }

    /**
     * Advances to the next {@code itemLocalName} element and returns its leaf
     * text values keyed by local element name (nesting is flattened — the
     * fields this ingest cares about have unique names within one item), or
     * {@code null} at the end of the document.
     */
    public Map<String, String> next() throws XMLStreamException {
        while (reader.hasNext()) {
            if (reader.next() == XMLStreamConstants.START_ELEMENT
                    && reader.getLocalName().equals(itemLocalName)) {
                return readItem();
            }
        }
        return null;
    }

    private Map<String, String> readItem() throws XMLStreamException {
        Map<String, String> values = new HashMap<>();
        int depth = 1;
        String leaf = null;
        StringBuilder text = new StringBuilder();
        while (depth > 0 && reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT -> {
                    depth++;
                    leaf = reader.getLocalName();
                    text.setLength(0);
                }
                case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> {
                    if (leaf != null) {
                        text.append(reader.getText());
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if (leaf != null && reader.getLocalName().equals(leaf)) {
                        values.put(leaf, text.toString());
                    }
                    leaf = null;
                    depth--;
                }
                default -> { /* ignore comments, whitespace-only events, etc. */ }
            }
        }
        return values;
    }

    @Override
    public void close() throws XMLStreamException {
        reader.close();
    }
}
