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
package eu.interedition.text;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import eu.interedition.text.mem.SimpleName;
import eu.interedition.text.util.SimpleXMLParserConfiguration;
import eu.interedition.text.xml.XML;
import eu.interedition.text.xml.XMLParser;
import eu.interedition.text.xml.XMLParserModule;
import eu.interedition.text.xml.module.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static eu.interedition.text.TextConstants.TEI_NS;

/**
 * Base class for tests working with documents generated from XML test resources.
 *
 * @author <a href="http://gregor.middell.net/" title="Homepage of Gregor Middell">Gregor Middell</a>
 */
public abstract class AbstractTestResourceTest extends AbstractTextTest {
  /**
   * Names of available XML test resources.
   */
  protected static final List<String> RESOURCES = Lists.newArrayList(//
          "archimedes-palimpsest-tei.xml",//
          "george-algabal-tei.xml",//
          "homer-iliad-tei.xml",//
          "ignt-0101.xml",//
          "whitman-leaves-facs-tei.xml",
          "wp-orpheus1-clix.xml");

  protected static XMLInputFactory xmlInputFactory;

  private Map<String, Text> sources = Maps.newHashMap();
  private Map<String, Text> texts = Maps.newHashMap();

  @Autowired
  private XMLParser xmlParser;

  @Autowired
  protected AnnotationRepository annotationRepository;

  @BeforeClass
  public static void initXmlInputFactory() {
    xmlInputFactory = XML.createXMLInputFactory();
  }

  @After
  public void removeDocuments() {
    for (Iterator<Text> textIt = texts.values().iterator(); textIt.hasNext(); ) {
      textRepository.delete(textIt.next());
      textIt.remove();
    }
    for (Iterator<Text> sourceIt = sources.values().iterator(); sourceIt.hasNext(); ) {
      textRepository.delete(sourceIt.next());
      sourceIt.remove();
    }
  }

  protected Text text() {
    return text(Iterables.getFirst(RESOURCES, null));
  }

  protected Text source() {
    return source(Iterables.getFirst(RESOURCES, null));
  }

  /**
   * Returns a test document generated from the resource with the given name.
   * <p/>
   * <p/>
   * <p/>
   * The generated test document is cached for later reuse.
   *
   * @param resource the name of the resource
   * @return the corresponding test document
   * @see #RESOURCES
   */
  protected synchronized Text text(String resource) {
    load(resource);
    return texts.get(resource);
  }

  protected synchronized Text source(String resource) {
    load(resource);
    return sources.get(resource);
  }

  protected List<XMLParserModule> parserModules() {
    return Lists.<XMLParserModule>newArrayList();
  }

  protected SimpleXMLParserConfiguration configure(SimpleXMLParserConfiguration pc) {
    pc.addLineElement(new SimpleName(TEI_NS, "head"));
    pc.addLineElement(new SimpleName(TEI_NS, "stage"));
    pc.addLineElement(new SimpleName(TEI_NS, "speaker"));
    pc.addLineElement(new SimpleName(TEI_NS, "lg"));
    pc.addLineElement(new SimpleName(TEI_NS, "l"));
    pc.addLineElement(new SimpleName((URI) null, "line"));

    pc.addContainerElement(new SimpleName(TEI_NS, "text"));
    pc.addContainerElement(new SimpleName(TEI_NS, "div"));
    pc.addContainerElement(new SimpleName(TEI_NS, "lg"));
    pc.addContainerElement(new SimpleName(TEI_NS, "subst"));
    pc.addContainerElement(new SimpleName(TEI_NS, "choice"));

    pc.exclude(new SimpleName(TEI_NS, "teiHeader"));
    pc.exclude(new SimpleName(TEI_NS, "front"));
    pc.exclude(new SimpleName(TEI_NS, "fw"));

    return pc;
  }

  protected SimpleXMLParserConfiguration createXMLParserConfiguration() {
    SimpleXMLParserConfiguration pc = new SimpleXMLParserConfiguration();

    final List<XMLParserModule> parserModules = pc.getModules();
    parserModules.add(new LineElementXMLParserModule());
    parserModules.add(new NotableCharacterXMLParserModule());
    parserModules.add(new TextXMLParserModule());
    parserModules.add(new DefaultAnnotationXMLParserModule(annotationRepository, 1000));
    parserModules.add(new CLIXAnnotationXMLParserModule(annotationRepository, 1000));
    parserModules.add(new TEIAwareAnnotationXMLParserModule(annotationRepository, 1000));
    parserModules.addAll(parserModules());

    return pc;
  }

  protected synchronized void unload(String resource) {
    sources.remove(resource);
    texts.remove(resource);
  }

  protected void unload() {
    unload(Iterables.getFirst(RESOURCES, null));
  }

  private synchronized void load(String resource) {
    try {
      if (RESOURCES.contains(resource) && !texts.containsKey(resource)) {
        final URL xmlResource = AbstractTestResourceTest.class.getResource("/" + resource);
        final URI uri = xmlResource.toURI();
        final StopWatch stopWatch = new StopWatch(uri.toString());

        InputStream xmlStream = null;
        XMLStreamReader xmlReader = null;
        try {
          stopWatch.start("create");
          xmlReader = xmlInputFactory.createXMLStreamReader(xmlStream = xmlResource.openStream());
          final Text xml = textRepository.create(xmlReader);
          stopWatch.stop();

          sources.put(resource, xml);
          stopWatch.start("parse");
          texts.put(resource, xmlParser.parse(xml, configure(createXMLParserConfiguration())));
          stopWatch.stop();
        } finally {
          XML.closeQuietly(xmlReader);
          Closeables.close(xmlStream, false);
        }

        if (LOG.isDebugEnabled()) {
          LOG.debug("\n" + stopWatch.prettyPrint());
        }
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }


}
