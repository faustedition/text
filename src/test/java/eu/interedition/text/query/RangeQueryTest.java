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
package eu.interedition.text.query;

import com.google.common.base.Joiner;
import eu.interedition.text.AbstractTestResourceTest;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.simple.KeyValues;
import eu.interedition.text.util.AutoCloseables;
import org.junit.Test;

import static eu.interedition.text.Query.and;
import static eu.interedition.text.Query.rangeLength;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class RangeQueryTest extends AbstractTestResourceTest {

  @Test
  public void searchEmptyRanges() {
      final QueryResult<KeyValues> qr = repository.query(and(Query.text(text()), rangeLength(0)));
      try {
          LOG.fine(Joiner.on('\n').join(qr));
      } finally {
          AutoCloseables.closeQuietly(qr);
      }
  }
}
