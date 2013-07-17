/*
 * Copyright (c) 2013 The Interedition Development Group.
 *
 * This file is part of Interedition Text.
 *
 * Interedition Text is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Interedition Text is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CollateX.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.interedition.text;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.io.Serializable;
import java.util.SortedSet;

/**
 * Adresses a text segment, for example a segment, that is annotated by some {@link Annotation annotation}.
 * <p/>
 * <p/>
 * <p/>
 * Segments are adressed by start and end offsets of the characters forming the boundaries of a segment. The character pointed to by
 * the start offset is included in the segment, while the character addressed by the end offset is the first excluded from it.
 * <p/>
 * <p/>
 * <p/>
 * Offsets are counted from zero and are located in the <i>gaps</i> between characters:
 * <p/>
 * <pre>
 *   a   b   c   d   e
 * 0 | 1 | 2 | 3 | 4 | 5
 * </pre>
 * <p/>
 * In the given example, the substring "bcd" would be adressed by the segment <code>[1, 4]</code>, the whole string by the segment
 * <code>[0, 5]</code>. Note that the difference between the offsets equals the length of the segment and that "empty" segments
 * pointing in the gaps between characters are valid. So for example to point to the gap between "d" and "e", the corresponding
 * empty segment's address would be <code>[4, 4]</code>.
 * <p/>
 * <p/>
 * <p/>
 * Apart from encapsulating the offset values denoting the segment, objects of this class also have methods to apply <a href=
 * "http://www.mind-to-mind.com/library/papers/ara/core-range-algebra-03-2002.pdf" title="Nicol: Core Range Algebra (PDF)">Gavin
 * Nicols' Core Range Algebra</a>. These methods like {@link #encloses(eu.interedition.text.Range)} or {@link #hasOverlapWith(eu.interedition.text.Range)} define
 * relationships between text segments, which can be used for example to filter sets of range annotations.
 *
 * @author <a href="http://gregor.middell.net/" title="Homepage of Gregor Middell">Gregor Middell</a>
 * @see CharSequence#subSequence(int, int)
 */
public class Range implements Comparable<Range> {
  public static final Range NULL = new Range(0, 0);

  /**
   * The start offset of the segment (counted from zero, inclusive).
   */
  private final long start;

  /**
   * The end offset of the segment (counted from zero, exclusive).
   */
  private final long end;

  /**
   * Creates a text segment address.
   *
   * @param start start offset
   * @param end   end offset
   * @throws IllegalArgumentException if <code>start</code> or <code>end</code> or lower than zero, or if <code>start</code> is greather than
   *                                  <code>end</code>
   */
  public Range(long start, long end) {
    if (start < 0 || end < 0 || start > end) {
      throw new IllegalArgumentException(toString(start, end));
    }
    this.start = start;
    this.end = end;
  }

  /**
   * Copy constructor.
   *
   * @param b the segment address to be copied
   */
  public Range(Range b) {
    this(b.start, b.end);
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  /**
   * The length of the adressed segment.
   *
   * @return the length (difference between start and end offset)
   */
  public long length() {
    return end - start;
  }

  /**
   * <i>a.start &lt;= b.start and a.end &gt;= b.end</i>
   *
   * @param b b range
   * @return <code>true</code>/<code>false</code>
   */
  public boolean encloses(Range b) {
    return (start <= b.start) && (end >= b.end);
  }

  /**
   * <i>a.start = b.start and a.end &gt; b.end</i>
   *
   * @param b b range
   * @return <code>true</code>/<code>false</code>
   */
  public boolean enclosesWithSuffix(Range b) {
    return (start == b.start) && (end > b.end);
  }

  /**
   * <i>a.start &lt; b.start and a.end = b.end</i>
   *
   * @param b b range
   * @return <code>true</code>/<code>false</code>
   */
  public boolean enclosesWithPrefix(Range b) {
    return (start < b.start) && (end == b.end);
  }

  /**
   * <i>(a <> b) and a.start &gt; b.start and a.end &lt;= b.end</i>
   *
   * @param b b range
   * @return <code>true</code>/<code>false</code>
   */
  public boolean fitsWithin(Range b) {
    return !equals(b) && (start >= b.start) && (end <= b.end);
  }

  /**
   * <i>overlap(a, b) &gt; 0</i>
   *
   * @param b b range
   * @return <code>true</code>/<code>false</code>
   */
  public boolean hasOverlapWith(Range b) {
    final Range overlap = overlap(b);
    return (overlap != null) && (overlap.length() > 0);
  }

  /**
   * Yields the overlapping segment of this and another segment.
   *
   * @param b another segment
   * @return <i>[max(a.start, b.start), min(a.end, b.end)]</i>
   */
  public Range intersectionWith(Range b) {
    return new Range(Math.max(start, b.start), Math.min(end, b.end));
  }

  /**
   * <i>min(a.end, b.end) - max(a.start, b.start)</i>
   *
   * @param b b range
   * @return length of overlap
   */
  public Range overlap(Range b) {
    final long start = Math.max(this.start, b.start);
    final long end = Math.min(this.end, b.end);
    return ((end - start) >= 0 ? new Range(start, end) : null);
  }

  /**
   * <i>b.start &gt;= a.end</i>
   *
   * @param b b range
   * @return <code>true</code>/<code>false</code>
   */
  public boolean precedes(Range b) {
    return b.start >= end;
  }

  /**
   * <i>a.start &gt;= (b.end - 1)</i>
   *
   * @param b b range
   * @return <code>true</code>/<code>false</code>
   */
  public boolean follows(Range b) {
    return (start >= (b.end - 1));
  }

  public Range shift(long delta) {
    return new Range(start + delta, end + delta);
  }

  /**
   * Orders segments, first by start offset, then by the reverse order of the end offsets.
   *
   * @see Comparable#compareTo(Object)
   */
  public int compareTo(Range o) {
    final long result = (start == o.start ? o.end - end : start - o.start);
    return (result < 0 ? -1 : (result > 0 ? 1 : 0));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(start, end);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Range)) {
      return super.equals(obj);
    }

    Range b = (Range) obj;
    return (this.start == b.start) && (this.end == b.end);
  }

  /**
   * Creates a string representation of an offset pair.
   *
   * @param start start offset
   * @param end   end offset
   * @return string representation
   */
  public static String toString(long start, long end) {
    return "[" + start + ", " + end + "]";
  }

  @Override
  public String toString() {
    return toString(start, end);
  }

  public SortedSet<Range> substract(Range subtrahend) {
    Preconditions.checkArgument(hasOverlapWith(subtrahend));

    final SortedSet<Range> remainders = Sets.newTreeSet();
    if (fitsWithin(subtrahend)) {
      return remainders;
    }
    if (enclosesWithPrefix(subtrahend)) {
      remainders.add(new Range(subtrahend.start, end));
    } else if (enclosesWithSuffix(subtrahend)) {
      remainders.add(new Range(start, subtrahend.end));
    } else {
      remainders.add(new Range(start, subtrahend.start));
      remainders.add(new Range(subtrahend.end, end));
    }

    return remainders;
  }
}