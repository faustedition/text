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

import eu.interedition.text.Annotation;
import eu.interedition.text.Text;

import java.util.Set;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface TokenizerSettings {
  boolean startingAnnotationsAreBoundary(Text text, long offset, Iterable<Annotation> annotations);

  boolean emptyAnnotationsAreBoundary(Text text, long offset, Iterable<Annotation> annotations);

  boolean endingAnnotationsAreBoundary(Text text, long offset, Iterable<Annotation> annotations);

  boolean isBoundary(Text text, long offset, char c);
}
