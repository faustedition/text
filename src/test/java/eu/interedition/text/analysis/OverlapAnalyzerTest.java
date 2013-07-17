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
package eu.interedition.text.analysis;

import com.google.common.collect.Iterables;
import eu.interedition.text.AbstractTestResourceTest;
import eu.interedition.text.Name;
import eu.interedition.text.Range;
import eu.interedition.text.Text;
import eu.interedition.text.event.AnnotationEventSource;
import eu.interedition.text.mem.SimpleAnnotation;
import eu.interedition.text.mem.SimpleName;
import eu.interedition.text.query.Criteria;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Collections;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class OverlapAnalyzerTest extends AbstractTestResourceTest {

  @Autowired
  private AnnotationEventSource eventSource;

  @Test
  public void analyzeNonOverlap() throws IOException {
    final OverlapAnalyzer analyzer = analyze(text());
    Assert.assertEquals(0, analyzer.getOverlapping().size());
    Assert.assertEquals(0, analyzer.getSelfOverlapping().size());
  }

  @Test
  public void analyzeSelfOverlap() throws IOException {
    final SimpleName overlap = new SimpleName(TEST_NS, "overlap");
    annotationRepository.create(
            new SimpleAnnotation(text, overlap, new Range(0, TEST_TEXT.length() - 1), null),
            new SimpleAnnotation(text, overlap, new Range(1, TEST_TEXT.length()), null)
    );
    final OverlapAnalyzer analyzer = analyze(text);
    Assert.assertEquals(0, analyzer.getOverlapping().size());
    Assert.assertEquals(1, analyzer.getSelfOverlapping().size());
    Assert.assertEquals(overlap, Iterables.getOnlyElement(analyzer.getSelfOverlapping()));
  }

  protected OverlapAnalyzer analyze(Text text) throws IOException {
    final OverlapAnalyzer analyzer = new OverlapAnalyzer();
    eventSource.listen(analyzer, text, Criteria.any(), Collections.<Name>emptySet());
    return analyzer;
  }
}
