package eu.interedition.text.json;

import com.google.common.base.Throwables;
import com.google.common.collect.*;
import eu.interedition.text.*;
import eu.interedition.text.json.map.AnnotationSerializer;
import eu.interedition.text.json.map.NameSerializer;
import eu.interedition.text.mem.SimpleAnnotation;
import eu.interedition.text.query.Operator;
import eu.interedition.text.rdbms.RelationalName;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static com.google.common.base.Preconditions.checkState;
import static eu.interedition.text.query.Criteria.*;
import static org.codehaus.jackson.JsonToken.*;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class JSONSerializer {

  public static final String TEXT_FIELD = "t";
  public static final String TEXT_LENGTH_FIELD = "l";
  public static final String ANNOTATIONS_FIELD = "a";
  public static final String ANNOTATION_DATA_FIELD = "d";
  public static final String NAMES_FIELD = "n";


  private TextRepository textRepository;
  private AnnotationRepository annotationRepository;
  private int batchSize = 1024;

  @Required
  public void setTextRepository(TextRepository textRepository) {
    this.textRepository = textRepository;
  }

  @Required
  public void setAnnotationRepository(AnnotationRepository annotationRepository) {
    this.annotationRepository = annotationRepository;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public void serialize(final JsonGenerator jgen, Text text, final JSONSerializerConfiguration config) throws IOException {
    final Range range = config.getRange();

    jgen.writeStartObject();

    final BiMap<String, URI> nsMap = HashBiMap.create(config.getNamespaceMappings());
    final BiMap<URI, String> prefixMap = (nsMap == null ? null : nsMap.inverse());

    final Operator criterion = and(config.getQuery(), text(text));
    if (range != null) {
      criterion.add(rangeOverlap(range));
    }

    final SortedSet<RelationalName> names = Sets.newTreeSet();
    jgen.writeArrayFieldStart(ANNOTATIONS_FIELD);
    try {
      annotationRepository.scroll(criterion, config.getDataSet(), new AnnotationConsumer() {
        @Override
        public void consume(Annotation annotation) {
          try {
            jgen.writeStartObject();

            final Name an = annotation.getName();
            checkState(RelationalName.class.isAssignableFrom(an.getClass()), an.getClass().toString());
            names.add((RelationalName) an);

            jgen.writeNumberField(AnnotationSerializer.NAME_FIELD, ((RelationalName) an).getId());

            final Range ar = annotation.getRange();
            jgen.writeArrayFieldStart(AnnotationSerializer.RANGE_FIELD);
            jgen.writeNumber(ar.getStart());
            jgen.writeNumber(ar.getEnd());
            jgen.writeEndArray();


            final Map<Name, String> data = annotation.getData();
            if (!data.isEmpty()) {
              jgen.writeArrayFieldStart(ANNOTATION_DATA_FIELD);
              for (Map.Entry<Name, String> dataEntry : data.entrySet()) {
                final Name dn = dataEntry.getKey();
                checkState(RelationalName.class.isAssignableFrom(dn.getClass()), dn.getClass().toString());
                names.add((RelationalName) dn);

                jgen.writeStartArray();
                jgen.writeNumber(((RelationalName) dn).getId());
                jgen.writeString(dataEntry.getValue());
                jgen.writeEndArray();
              }
              jgen.writeEndArray();
            }
            jgen.writeEndObject();
          } catch (IOException e) {
            throw Throwables.propagate(e);
          }
        }
      });
    } catch (Throwable t) {
      Throwables.propagateIfInstanceOf(t, IOException.class);
      Throwables.propagateIfInstanceOf(Throwables.getRootCause(t), IOException.class);
      throw Throwables.propagate(t);
    }
    jgen.writeEndArray();

    if (!names.isEmpty()) {
      jgen.writeObjectFieldStart(NAMES_FIELD);
      for (RelationalName n : names) {
        jgen.writeFieldName(Long.toString(n.getId()));
        NameSerializer.serialize(n, jgen, prefixMap);
      }
      jgen.writeEndObject();
    }

    jgen.writeNumberField(TEXT_LENGTH_FIELD, text.getLength());
    jgen.writeStringField(TEXT_FIELD, textRepository.read(text, range == null ? new Range(0, text.getLength()) : range));

    jgen.writeEndObject();
  }

  public void unserialize(final JsonParser jp, Text text) throws IOException {
    checkFormat(jp.nextToken().equals(START_ARRAY), "Expected start of array", jp);

    final Map<String, Name> names = Maps.newHashMap();
    checkFormat(jp.nextToken().equals(START_OBJECT), "Expected start of name hash", jp);
    while ((jp.nextToken() != null) && !END_OBJECT.equals(jp.getCurrentToken())) {
      checkFormat(jp.getCurrentToken().equals(FIELD_NAME), "Expected field name", jp);
      final String id = jp.getCurrentName();

      checkFormat(jp.nextToken() != null, "Expected QName", jp);
      names.put(id, jp.readValueAs(Name.class));
    }
    checkFormat(jp.getCurrentToken() != null, "Unexpected end of tokens", jp);

    final List<Annotation> batch = Lists.newArrayList();
    checkFormat(START_ARRAY.equals(jp.nextToken()), "Expected start of annotation array", jp);
    while ((jp.nextToken() != null) && !END_ARRAY.equals(jp.getCurrentToken())) {
      checkFormat(START_OBJECT.equals(jp.getCurrentToken()), "Expected start of annotation object", jp);

      final JsonNode annotationNode = jp.readValueAsTree();
      final String nameRef = annotationNode.path(AnnotationSerializer.NAME_FIELD).getTextValue();
      checkFormat(nameRef != null, "No name for annotation", jp);
      checkFormat(names.containsKey(nameRef), "Invalid name reference for annotation", jp);

      final Name name = names.get(nameRef);

      checkFormat(annotationNode.has(AnnotationSerializer.RANGE_FIELD), "No range for annotation", jp);

      final JsonParser rangeParser = annotationNode.get(AnnotationSerializer.RANGE_FIELD).traverse();
      rangeParser.setCodec(jp.getCodec());
      final Range range = rangeParser.readValueAs(Range.class);

      final Map<Name, String> data = Maps.newHashMap();
      if (annotationNode.has(ANNOTATION_DATA_FIELD)) {
        for (JsonNode dataNode : annotationNode.get(ANNOTATION_DATA_FIELD)) {
          checkFormat(dataNode.isArray() && dataNode.size() > 1, "Expected array of 2 as annotation data entry", jp);

          final String dataNameRef = dataNode.get(0).getTextValue();
          checkFormat(dataNameRef != null, "Expected annotation data's name reference", jp);
          checkFormat(names.containsKey(dataNameRef), "Invalid annotation data name reference", jp);

          final Name dataName = names.get(dataNameRef);

          final String dataValue = dataNode.get(1).getTextValue();
          checkFormat(dataValue != null, "Expected annotation data value", jp);
          data.put(dataName, dataValue);
        }
      }

      batch.add(new SimpleAnnotation(text, name, range, data));
      if (batch.size() % batchSize == 0) {
        annotationRepository.create(batch);
        batch.clear();
      }
    }
    if (!batch.isEmpty()) {
      annotationRepository.create(batch);
    }

    checkFormat(jp.nextToken().equals(END_ARRAY), "Expected end of array", jp);
  }

  private static void checkFormat(boolean check, String message, JsonParser jp) throws JsonParseException {
    if (!check) {
      throw new JsonParseException(message, jp.getCurrentLocation());
    }
  }
}
