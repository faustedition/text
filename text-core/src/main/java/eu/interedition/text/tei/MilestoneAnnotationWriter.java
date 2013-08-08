/*
 * Copyright (c) 2013 The Interedition Development Group.
 *
 * This file is part of CollateX.
 *
 * CollateX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CollateX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CollateX.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.interedition.text.tei;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import eu.interedition.text.Annotation;
import eu.interedition.text.AnnotationTarget;
import eu.interedition.text.repository.Store;
import eu.interedition.text.xml.AnnotationWriter;
import eu.interedition.text.xml.NamespaceMapping;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class MilestoneAnnotationWriter extends AnnotationWriter {

    private static final Map<String, String> MILESTONE_ELEMENT_UNITS = Maps.newHashMap();

    static {
        MILESTONE_ELEMENT_UNITS.put("pb", "page");
        MILESTONE_ELEMENT_UNITS.put("lb", "line");
        MILESTONE_ELEMENT_UNITS.put("cb", "column");
        MILESTONE_ELEMENT_UNITS.put("gb", "gathering");
    }

    private static final String MILESTONE_NAME = "milestone";
    private static final String MILESTONE_UNIT_ATTR_NAME = "unit";

    private static final String SPAN_TO_ATTR_NAME = "spanTo";
    private static final String ID_ATTR_NAME = "id";

    private final long text;
    private final Iterator<Long> annotationIds;
    private final ObjectMapper objectMapper;

    private final Deque<Boolean> handledElements = new ArrayDeque<Boolean>();
    private final Multimap<String, Annotation> spanning = ArrayListMultimap.create();
    private final Map<String, Annotation> milestones = Maps.newHashMap();
    private final Map<String, Integer> milestoneStarts = Maps.newHashMap();

    public MilestoneAnnotationWriter(Store texts, long text, Iterator<Long> annotationIds) {
        super(texts);
        this.text = text;
        this.annotationIds = annotationIds;
        this.objectMapper = texts.objectMapper();
    }


    @Override
    public boolean accept(XMLStreamReader reader) {
        if (reader.isStartElement()) {
            final QName elementName = reader.getName();
            final String elementNs = Strings.nullToEmpty(elementName.getNamespaceURI());
            boolean handled = false;
            if (NamespaceMapping.TEI_NS_URI.equals(elementNs)) {
                final boolean handledSpanningElement = handleSpanningElements(reader, elementName);
                final boolean handledMilestoneElement = handleMilestoneElements(reader, elementName);
                handled = (handledSpanningElement || handledMilestoneElement);
            }
            handledElements.push(handled);
            return !handled;
        } else if (reader.isEndElement()) {
            return !handledElements.pop();
        }
        return true;
    }


    @Override
    public void flush() {
        final int offset = extractor().offset();
        for (Map.Entry<String, Annotation> milestone : milestones.entrySet()) {
            final Integer start = milestoneStarts.remove(milestone.getKey());
            if (start != null) {
                final Annotation last = milestone.getValue();
                write(new Annotation(last.id(), new AnnotationTarget(text, start, offset), last.data()));
            }
        }
        super.flush();
    }

    boolean handleMilestoneElements(XMLStreamReader reader, QName elementName) {
        final String elementLn = elementName.getLocalPart();
        String milestoneUnit = null;
        if (MILESTONE_NAME.equals(elementLn)) {
            for (int a = 0, ac = reader.getAttributeCount(); a < ac; a++) {
                final String ns = Strings.nullToEmpty(reader.getAttributeNamespace(a));
                if (!ns.isEmpty() && !NamespaceMapping.TEI_NS_URI.equals(ns)) {
                    continue;
                }
                final String ln = reader.getAttributeLocalName(a);
                if (MILESTONE_UNIT_ATTR_NAME.equals(ln)) {
                    milestoneUnit = reader.getAttributeValue(a);
                    break;
                }
            }
        } else if (MILESTONE_ELEMENT_UNITS.containsKey(elementLn)) {
            milestoneUnit = MILESTONE_ELEMENT_UNITS.get(elementLn);
        }

        if (milestoneUnit == null) {
            return false;
        }

        final int offset = extractor().offset();
        final Annotation last = milestones.remove(milestoneUnit);
        final Integer start = milestoneStarts.remove(milestoneUnit);
        if (last != null && start != null) {
            write(new Annotation(last.id(), new AnnotationTarget(text, start, offset), last.data()));
        }

        final ObjectNode attributes = objectMapper.createObjectNode();
        for (int a = 0, ac = reader.getAttributeCount(); a < ac; a++) {
            final QName name = reader.getAttributeName(a);
            final String ns = Strings.nullToEmpty(name.getNamespaceURI());
            if ((ns.isEmpty() || NamespaceMapping.TEI_NS_URI.equals(ns)) && MILESTONE_UNIT_ATTR_NAME.equals(name.getLocalPart())) {
                continue;
            }
            attributes.put(extractor().name(name), reader.getAttributeValue(a));
        }
        final ObjectNode data = objectMapper.createObjectNode();
        data.put(extractor().name(XML_ELEMENT_NAME), extractor().name(new QName(NamespaceMapping.TEI_NS_URI, milestoneUnit)));
        if (attributes.size() > 0) {
            data.put(extractor().name(XML_ELEMENT_ATTRS), attributes);
        }

        milestones.put(milestoneUnit, new Annotation(annotationIds.next(), Sets.<AnnotationTarget>newTreeSet(), data));
        milestoneStarts.put(milestoneUnit, offset);

        return true;
    }

    boolean handleSpanningElements(XMLStreamReader reader, QName elementName) {
        String spanTo = null;
        String refId = null;
        for (int a = 0, ac = reader.getAttributeCount(); a < ac; a++) {
            final String ns = Strings.nullToEmpty(reader.getAttributeNamespace(a));
            final String ln = reader.getAttributeLocalName(a);
            if ((ns.isEmpty() || NamespaceMapping.TEI_NS_URI.equals(ns)) && SPAN_TO_ATTR_NAME.equals(ln)) {
                spanTo = reader.getAttributeValue(a).replaceAll("^#", "");
            } else if (XMLConstants.XML_NS_URI.equals(ns) && ID_ATTR_NAME.equals(ln)) {
                refId = reader.getAttributeValue(a);
            }
        }

        if (spanTo != null) {
            final ObjectNode attributes = objectMapper.createObjectNode();
            for (int a = 0, ac = reader.getAttributeCount(); a < ac; a++) {
                final QName name = reader.getAttributeName(a);
                final String ns = Strings.nullToEmpty(name.getNamespaceURI());
                if ((ns.isEmpty() || NamespaceMapping.TEI_NS_URI.equals(ns)) && SPAN_TO_ATTR_NAME.equals(name.getLocalPart())) {
                    continue;
                }
                attributes.put(extractor().name(name), reader.getAttributeValue(a));
            }
            final ObjectNode data = objectMapper.createObjectNode();
            data.put(extractor().name(XML_ELEMENT_NAME), extractor().name(new QName(
                    NamespaceMapping.TEI_NS_URI,
                    elementName.getLocalPart().replaceAll("Span$", "")
            )));
            if (attributes.size() > 0) {
                data.put(extractor().name(XML_ELEMENT_ATTRS), attributes);
            }

            final int offset = extractor().offset();
            spanning.put(spanTo, new Annotation(annotationIds.next(), new AnnotationTarget(text, offset, offset), data));
        }
        if (refId != null) {
            for (Annotation annotation : spanning.removeAll(refId)) {
                final int offset = extractor().offset();
                write(new Annotation(
                        annotation.id(),
                        new AnnotationTarget(text, annotation.targets().first().start(), offset),
                        annotation.data()
                ));
            }
        }
        return (spanTo != null);
    }
}
