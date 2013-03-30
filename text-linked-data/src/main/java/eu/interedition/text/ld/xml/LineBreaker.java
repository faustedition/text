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

package eu.interedition.text.ld.xml;

import javax.xml.stream.XMLStreamReader;
import java.util.Set;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public abstract class LineBreaker extends TextExtractorComponent {


    @Override
    protected void onXMLEvent(XMLStreamReader reader) {
        if (breakLine(reader)) {
            extractor().insert("\n");
        }
    }

    protected abstract boolean breakLine(XMLStreamReader reader);

    public static class ElementNamedBased extends LineBreaker {

        private final Set<String> lineElements;

        public ElementNamedBased(Set<String> lineElements) {
            this.lineElements = lineElements;
        }

        @Override
        protected boolean breakLine(XMLStreamReader reader) {
            return (reader.isStartElement() && extractor().offset() > 0 && lineElements.contains(reader.getLocalName()));
        }
    }
}
