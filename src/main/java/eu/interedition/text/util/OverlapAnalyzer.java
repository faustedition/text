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
package eu.interedition.text.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.TextStream;
import java.util.Set;
import java.util.SortedSet;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class OverlapAnalyzer<T> extends TextStream.ListenerAdapter<T> {
    private Set<Name> selfOverlapping;
    private Set<SortedSet<Name>> overlapping;
    private Set<Layer<T>> started;

    public Set<Name> getSelfOverlapping() {
        return selfOverlapping;
    }

    public Set<SortedSet<Name>> getOverlapping() {
        return overlapping;
    }

    @Override
    public void start(long contentLength) {
        selfOverlapping = Sets.newHashSet();
        overlapping = Sets.newHashSet();
        started = Sets.newHashSet();
    }

    @Override
    public void start(long offset, Iterable<Layer<T>> annotations) {
        Iterables.addAll(started, annotations);
    }

    @Override
    public void end(long offset, Iterable<Layer<T>> annotations) {
        for (Layer ending : annotations) {
            started.remove(ending);
        }

        for (Layer<T> ending : annotations) {
            final Name endingName = ending.getName();
            for (Anchor endingAnchor : ending.getAnchors()) {
                for (Layer<T> started : this.started) {
                    final Name startedName = started.getName();
                    for (Anchor startedAnchor : started.getAnchors()) {
                        if (!startedAnchor.getRange().encloses(endingAnchor.getRange())) {
                            if (startedName.equals(endingName)) {
                                selfOverlapping.add(endingName);
                            } else {
                                overlapping.add(Sets.newTreeSet(Sets.newHashSet(startedName, endingName)));
                            }
                        }
                    }
                }
            }
        }
    }
}
