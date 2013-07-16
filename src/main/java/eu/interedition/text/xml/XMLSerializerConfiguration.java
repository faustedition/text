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

import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface XMLSerializerConfiguration<T> {

    Name getRootName();

    Map<String, URI> getNamespaceMappings();

    List<Name> getHierarchy();

    Query getQuery();

    Map<Name, String> extractAttributes(Layer<T> layer);

    XMLNodePath extractXMLNodePath(Layer<T> layer);
}