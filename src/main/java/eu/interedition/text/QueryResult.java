package eu.interedition.text;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface QueryResult<T> extends Iterable<Layer<T>>, AutoCloseable {
}
