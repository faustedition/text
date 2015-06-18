/*
 * #%L
 * Text: A text model with range-based markup via standoff annotations.
 * %%
 * Copyright (C) 2010 - 2011 The Interedition Development Group
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package eu.interedition.text.xml;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.TextRange;
import eu.interedition.text.TextRepository;
import eu.interedition.text.util.BatchLayerAdditionSupport;
import eu.interedition.text.util.UpdateSupport;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static eu.interedition.text.TextConstants.XML_TARGET_NAME;

public abstract class XMLTransformerConfigurationBase<T> implements XMLTransformerConfiguration<T> {
    private Set<Name> excluded = Sets.newHashSet();
    private Set<Name> included = Sets.newHashSet();
    private Set<Name> lineElements = Sets.newHashSet();
    private Set<Name> containerElements = Sets.newHashSet();
    private Set<Name> whitespaceTrimmingElements = Sets.newHashSet();
    private Set<Name> notableElements = Sets.newHashSet();
    private char notableCharacter = '\u25CA';
    private boolean compressingWhitespace = true;
    private int textBufferSize = 102400;
    private boolean removeLeadingWhitespace = true;
    private List<XMLTransformerModule<T>> modules = Lists.newArrayList();

    private final TextRepository<T> repository;
    private List<Layer<T>> batch;
    private int batchSize = 1;

    protected XMLTransformerConfigurationBase(TextRepository<T> repository) {
        this.repository = repository;
    }

    public XMLTransformerConfigurationBase<T> withBatchSize(int batchSize) {
        this.batchSize = Math.max(1, (repository instanceof BatchLayerAdditionSupport ? batchSize : 1));
        this.batch = Lists.newArrayListWithCapacity(this.batchSize);
        return this;
    }

    public void addLineElement(Name lineElementName) {
        lineElements.add(lineElementName);
    }

    public boolean removeLineElement(Name lineElementName) {
        return lineElements.remove(lineElementName);
    }

    public boolean isLineElement(XMLEntity entity) {
        return lineElements.contains(entity.getName());
    }

    public void addContainerElement(Name containerElementName) {
        containerElements.add(containerElementName);
    }

    public boolean removeContainerElement(Name containerElementName) {
        return containerElements.remove(containerElementName);
    }

    public boolean isContainerElement(XMLEntity entity) {
        return containerElements.contains(entity.getName());
    }

    public void addWhitespaceTrimmingElement(Name wsTrimmingElementName) {
        whitespaceTrimmingElements.add(wsTrimmingElementName);
    }

    public boolean removeWhitespaceTrimmingElement(Name wsTrimmingElementName) {
        return containerElements.remove(wsTrimmingElementName);
    }

    public boolean isWhitespaceTrimmingElement(XMLEntity entity) {
        return whitespaceTrimmingElements.contains(entity.getName());
    }


    public void include(Name name) {
        included.add(name);
    }

    public void exclude(Name name) {
        excluded.add(name);
    }

    public boolean included(XMLEntity entity) {
        return included.contains(entity.getName());
    }

    public boolean excluded(XMLEntity entity) {
        return excluded.contains(entity.getName());
    }

    public char getNotableCharacter() {
        return notableCharacter;
    }

    public void setNotableCharacter(char notableCharacter) {
        this.notableCharacter = notableCharacter;
    }

    public void addNotableElement(Name name) {
        notableElements.add(name);
    }

    public boolean removeNotableElement(Name name) {
        return notableElements.remove(name);
    }

    public boolean isNotable(XMLEntity entity) {
        return notableElements.contains(entity.getName());
    }

    public boolean isCompressingWhitespace() {
        return compressingWhitespace;
    }

    public void setCompressingWhitespace(boolean compressingWhitespace) {
        this.compressingWhitespace = compressingWhitespace;
    }

    public List<XMLTransformerModule<T>> getModules() {
        return modules;
    }

    public int getTextBufferSize() {
        return textBufferSize;
    }

    public void setTextBufferSize(int textBufferSize) {
        this.textBufferSize = textBufferSize;
    }

    public boolean isRemoveLeadingWhitespace() {
        return removeLeadingWhitespace;
    }

    public void setRemoveLeadingWhitespace(boolean removeLeadingWhitespace) {
        this.removeLeadingWhitespace = removeLeadingWhitespace;
    }

    @Override
    public Layer<T> start(Layer<T> source) throws IOException {
        return repository.add(XML_TARGET_NAME, new StringReader(""), null, new Anchor<T>(source, new TextRange(0, source.length())));
    }


	public void xmlElement(Name name, Map<Name, String> attributes, Anchor<T> anchor) {
		xmlElement(name, attributes, Collections.singleton(anchor));
	}

	@SuppressWarnings("unchecked")
    public void xmlElement(Name name, Map<Name, String> attributes, Iterable<Anchor<T>> anchors) {
        try {
            final Layer<T> layer = translate(name, attributes, Sets.newHashSet(anchors));
            if (batchSize > 1) {
                batch.add(layer);
                if (batch.size() >= batchSize) {
                    ((BatchLayerAdditionSupport<T>) repository).add(batch);
                    batch.clear();
                }
            } else {
                repository.add(layer.getName(), new StringReader(""), layer.data(), layer.getAnchors());
            }
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    @SuppressWarnings("unchecked")
    public void end(Layer<T> target, Reader text) throws IOException {
        if (batchSize > 1 && !batch.isEmpty()) {
            ((BatchLayerAdditionSupport<T>) repository).add(batch);
        }
        if (repository instanceof UpdateSupport) {
            ((UpdateSupport<T>) repository).updateText(target, text);
        }
    }

    protected abstract Layer<T> translate(Name name, Map<Name, String> attributes, Set<Anchor<T>> anchors);
}