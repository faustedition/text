/*
 * #%L
 * Text Repository: Datastore for texts based on Interedition's model.
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
package eu.interedition.text.token;

import com.google.common.collect.Lists;
import eu.interedition.text.*;
import eu.interedition.text.event.AnnotationEventListener;
import eu.interedition.text.event.AnnotationEventSource;
import eu.interedition.text.mem.SimpleAnnotation;
import eu.interedition.text.mem.SimpleName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static eu.interedition.text.query.Criteria.*;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Tokenizer {
  public static final Name DEFAULT_TOKEN_NAME = new SimpleName(TextConstants.INTEREDITION_NS_URI, "token");

  private static final Logger LOG = LoggerFactory.getLogger(Tokenizer.class);

  private AnnotationRepository annotationRepository;
  private AnnotationEventSource eventSource;
  private Name tokenName = DEFAULT_TOKEN_NAME;
  private int pageSize = 102400;
  private int batchSize = 1024;

  public void setAnnotationRepository(AnnotationRepository annotationRepository) {
    this.annotationRepository = annotationRepository;
  }

  public void setEventSource(AnnotationEventSource eventSource) {
    this.eventSource = eventSource;
  }

  public void setTokenName(Name tokenName) {
    this.tokenName = tokenName;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public void tokenize(Text text, TokenizerSettings settings) throws IOException {
    annotationRepository.delete(and(text(text), annotationName(tokenName)));
    eventSource.listen(new TokenGeneratingListener(text, settings), pageSize, text, none(), Collections.<Name>emptySet());
  }

  private class TokenGeneratingListener implements AnnotationEventListener {
    private final TokenizerSettings settings;
    private final Text text;

    private List<Annotation> batch = Lists.newArrayListWithExpectedSize(batchSize);
    private boolean lastIsTokenBoundary = true;
    private int offset = 0;
    private int tokenStart = Integer.MAX_VALUE;
    private int tokenCount = 0;

    private TokenGeneratingListener(Text text, TokenizerSettings settings) {
      this.settings = settings;
      this.text = text;
    }

    @Override
    public void start() {
      LOG.debug("Tokenizing " + text);
    }

    @Override
    public void start(long offset, Iterable<Annotation> annotations) {
      if (settings.startingAnnotationsAreBoundary(text, offset, annotations)) {
        lastIsTokenBoundary = true;
      }
    }

    @Override
    public void empty(long offset, Iterable<Annotation> annotations) {
      if (settings.emptyAnnotationsAreBoundary(text, offset, annotations)) {
        lastIsTokenBoundary = true;
      }
    }

    @Override
    public void end(long offset, Iterable<Annotation> annotations) {
      if (settings.endingAnnotationsAreBoundary(text, offset, annotations)) {
        lastIsTokenBoundary = true;
      }
    }

    @Override
    public void text(Range r, String content) {
      for (char c : content.toCharArray()) {
        if (settings.isBoundary(text, offset, c)) {
          lastIsTokenBoundary = true;
        } else {
          if (lastIsTokenBoundary) {
            token();
          }
          if (tokenStart > offset) {
            tokenStart = offset;
          }
          lastIsTokenBoundary = false;
        }

        offset++;
      }
    }

    @Override
    public void end() {
      token();
      emit();
      LOG.debug(text + " has " + tokenCount + " token(s)");
    }

    private void token() {
      if (tokenStart < offset) {
        batch.add(new SimpleAnnotation(text, tokenName, new Range(tokenStart, offset), null));
        if ((batch.size() % batchSize) == 0) {
          emit();
        }
        tokenCount++;
        tokenStart = Integer.MAX_VALUE;
      }
    }

    private void emit() {
      annotationRepository.create(batch);
      batch.clear();
    }
  }
}
