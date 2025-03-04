/*
 * Copyright 2020 LinkedIn Corp.
 * Licensed under the BSD 2-Clause License (the "License").
 * See License in the project root for license information.
 */


package com.linkedin.avroutil1.compatibility.avro15;

import com.linkedin.avroutil1.compatibility.AvroAdapter;
import com.linkedin.avroutil1.compatibility.AvroGeneratedSourceCode;
import com.linkedin.avroutil1.compatibility.AvroSchemaUtil;
import com.linkedin.avroutil1.compatibility.AvroVersion;
import com.linkedin.avroutil1.compatibility.AvscGenerationConfig;
import com.linkedin.avroutil1.compatibility.CodeGenerationConfig;
import com.linkedin.avroutil1.compatibility.CodeTransformations;
import com.linkedin.avroutil1.compatibility.ExceptionUtils;
import com.linkedin.avroutil1.compatibility.FieldBuilder;
import com.linkedin.avroutil1.compatibility.Jackson1Utils;
import com.linkedin.avroutil1.compatibility.SchemaBuilder;
import com.linkedin.avroutil1.compatibility.SchemaNormalization;
import com.linkedin.avroutil1.compatibility.SchemaParseConfiguration;
import com.linkedin.avroutil1.compatibility.SchemaParseResult;
import com.linkedin.avroutil1.compatibility.SkipDecoder;
import com.linkedin.avroutil1.compatibility.StringPropertyUtils;
import com.linkedin.avroutil1.compatibility.StringRepresentation;
import com.linkedin.avroutil1.compatibility.avro15.backports.Avro15DefaultValuesCache;
import com.linkedin.avroutil1.compatibility.avro15.codec.AliasAwareSpecificDatumReader;
import com.linkedin.avroutil1.compatibility.avro15.codec.BoundedMemoryDecoder;
import com.linkedin.avroutil1.compatibility.avro15.codec.CachedResolvingDecoder;
import com.linkedin.avroutil1.compatibility.avro15.codec.CompatibleJsonDecoder;
import com.linkedin.avroutil1.compatibility.avro15.codec.CompatibleJsonEncoder;
import com.linkedin.avroutil1.compatibility.backports.ObjectInputToInputStreamAdapter;
import com.linkedin.avroutil1.compatibility.backports.ObjectOutputToOutputStreamAdapter;
import java.util.Objects;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.Avro15BinaryDecoderAccessUtil;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumReader;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


public class Avro15Adapter implements AvroAdapter {
  private final static Logger LOG = LoggerFactory.getLogger(Avro15Adapter.class);

  private final Field fieldAliasesField;
  private final Field fieldPropsField;
  private final Field schemaPropsField;
  private boolean compilerSupported;
  private Throwable compilerSupportIssue;
  private String compilerSupportMessage;
  private Constructor<?> specificCompilerCtr;
  private Method compilerEnqueueMethod;
  private Method compilerCompileMethod;
  private Field outputFilePathField;
  private Field outputFileContentsField;

  public Avro15Adapter() {
    try {
      fieldAliasesField = Schema.Field.class.getDeclaredField("aliases");
      fieldAliasesField.setAccessible(true);
      fieldPropsField = Schema.Field.class.getDeclaredField("props");
      fieldPropsField.setAccessible(true);
      schemaPropsField = Schema.class.getDeclaredField("props");
      schemaPropsField.setAccessible(true);
    } catch (Exception e) {
      throw new IllegalStateException("error initializing adapter", e);
    }
    tryInitializeCompilerFields();
  }

  private void tryInitializeCompilerFields() {
    //compiler was moved out into a separate jar in avro 1.5+, so compiler functionality is optional
    try {
      Class<?> compilerClass = Class.forName("org.apache.avro.compiler.specific.SpecificCompiler");
      specificCompilerCtr = compilerClass.getConstructor(Schema.class);
      compilerEnqueueMethod = compilerClass.getDeclaredMethod("enqueue", Schema.class);
      compilerEnqueueMethod.setAccessible(true); //its normally private
      compilerCompileMethod = compilerClass.getDeclaredMethod("compile");
      compilerCompileMethod.setAccessible(true); //package-protected
      Class<?> outputFileClass = Class.forName("org.apache.avro.compiler.specific.SpecificCompiler$OutputFile");
      outputFilePathField = outputFileClass.getDeclaredField("path");
      outputFilePathField.setAccessible(true);
      outputFileContentsField = outputFileClass.getDeclaredField("contents");
      outputFileContentsField.setAccessible(true);
      compilerSupported = true;
    } catch (Exception | LinkageError e) {
      //if a class we directly look for above isnt found, we get ClassNotFoundException
      //but if we're missing a transitive dependency we will get NoClassDefFoundError
      compilerSupported = false;
      compilerSupportIssue = e;
      String reason = ExceptionUtils.rootCause(compilerSupportIssue).getMessage();
      compilerSupportMessage = "avro SpecificCompiler class could not be found or instantiated because " + reason
              + ". please make sure you have a dependency on org.apache.avro:avro-compiler";
      //ignore
    }
  }

  @Override
  public AvroVersion supportedMajorVersion() {
    return AvroVersion.AVRO_1_5;
  }

  @Override
  public BinaryEncoder newBinaryEncoder(OutputStream out, boolean buffered, BinaryEncoder reuse) {
    if (buffered) {
      return EncoderFactory.get().binaryEncoder(out, reuse);
    } else {
      return EncoderFactory.get().directBinaryEncoder(out, reuse);
    }
  }

  @Override
  public BinaryEncoder newBinaryEncoder(ObjectOutput out) {
    return newBinaryEncoder(new ObjectOutputToOutputStreamAdapter(out), false, null);
  }

  @Override
  public BinaryDecoder newBinaryDecoder(InputStream in, boolean buffered, BinaryDecoder reuse) {
    DecoderFactory factory = DecoderFactory.get();
    return buffered ? factory.binaryDecoder(in, reuse) : factory.directBinaryDecoder(in, reuse);
  }

  @Override
  public BinaryDecoder newBinaryDecoder(ObjectInput in) {
    return newBinaryDecoder(new ObjectInputToInputStreamAdapter(in), false, null);
  }

  @Override
  public BinaryDecoder newBinaryDecoder(byte[] bytes, int offset, int length, BinaryDecoder reuse) {
    return Avro15BinaryDecoderAccessUtil.newBinaryDecoder(bytes, offset, length, reuse);
  }

  @Override
  public JsonEncoder newJsonEncoder(Schema schema, OutputStream out, boolean pretty) throws IOException {
    JsonGenerator jsonGenerator = new JsonFactory().createJsonGenerator(out, JsonEncoding.UTF8);
    if (pretty) {
      jsonGenerator.useDefaultPrettyPrinter();
    }
    return EncoderFactory.get().jsonEncoder(schema, jsonGenerator);
  }

  @Override
  public Encoder newJsonEncoder(Schema schema, OutputStream out, boolean pretty, AvroVersion jsonFormat) throws IOException {
    return new CompatibleJsonEncoder(schema, out, pretty, jsonFormat == null || jsonFormat.laterThan(AvroVersion.AVRO_1_4));
  }

  @Override
  public JsonDecoder newJsonDecoder(Schema schema, InputStream in) throws IOException {
    return DecoderFactory.get().jsonDecoder(schema, in);
  }

  @Override
  public JsonDecoder newJsonDecoder(Schema schema, String in) throws IOException {
    return DecoderFactory.get().jsonDecoder(schema, in);
  }

  @Override
  public Decoder newCompatibleJsonDecoder(Schema schema, InputStream in) throws IOException {
    return new CompatibleJsonDecoder(schema, in);
  }

  @Override
  public Decoder newCompatibleJsonDecoder(Schema schema, String in) throws IOException {
    return new CompatibleJsonDecoder(schema, in);
  }

  @Override
  public SkipDecoder newCachedResolvingDecoder(Schema writer, Schema reader, Decoder in) throws IOException {
    return new CachedResolvingDecoder(writer, reader, in);
  }

  @Override
  public Decoder newBoundedMemoryDecoder(InputStream in) throws IOException {
    return new BoundedMemoryDecoder(in);
  }

  @Override
  public Decoder newBoundedMemoryDecoder(byte[] data) throws IOException {
    return new BoundedMemoryDecoder(data);
  }

  @Override
  public <T> SpecificDatumReader<T> newAliasAwareSpecificDatumReader(Schema writer, Class<T> readerClass) {
    Schema readerSchema = AvroSchemaUtil.getDeclaredSchema(readerClass);
    return new AliasAwareSpecificDatumReader<>(writer, readerSchema);
  }

  @Override
  public SchemaParseResult parse(String schemaJson, SchemaParseConfiguration desiredConf, Collection<Schema> known) {
    Schema.Parser parser = new Schema.Parser();
    boolean validateNames = true;
    boolean validateDefaults = false;
    boolean validateNumericDefaultValueTypes = false;
    boolean validateNoDanglingContent = false;
    if (desiredConf != null) {
      validateNames = desiredConf.validateNames();
      validateDefaults = desiredConf.validateDefaultValues();
      validateNumericDefaultValueTypes = desiredConf.validateNumericDefaultValueTypes();
      validateNoDanglingContent = desiredConf.validateNoDanglingContent();
    }
    SchemaParseConfiguration configUsed = new SchemaParseConfiguration(
        validateNames,
        validateDefaults,
        validateNumericDefaultValueTypes,
        validateNoDanglingContent
    );

    parser.setValidate(validateNames);
    if (known != null && !known.isEmpty()) {
      Map<String, Schema> knownByFullName = new HashMap<>(known.size());
      for (Schema s : known) {
        knownByFullName.put(s.getFullName(), s);
      }
      parser.addTypes(knownByFullName);
    }
    Schema mainSchema = parser.parse(schemaJson);
    Map<String, Schema> knownByFullName = parser.getTypes();
    if (configUsed.validateDefaultValues()) {
      //avro 1.5 doesnt properly validate default values, so we have to do it ourselves
      Avro15SchemaValidator validator = new Avro15SchemaValidator(configUsed, known);
      AvroSchemaUtil.traverseSchema(mainSchema, validator); //will throw on issues
    }
    if (configUsed.validateNoDanglingContent()) {
      Jackson1Utils.assertNoTrailingContent(schemaJson);
    }
    return new SchemaParseResult(mainSchema, knownByFullName, configUsed);
  }

  @Override
  public String toParsingForm(Schema s) {
    return SchemaNormalization.toParsingForm(s);
  }

  @Override
  public String getDefaultValueAsJsonString(Schema.Field field) {
    JsonNode json = field.defaultValue();
    if (json == null) {
      throw new AvroRuntimeException("Field " + field + " has no default value");
    }
    return json.toString();
  }

  @Override
  public Object newInstance(Class<?> clazz, Schema schema) {
    return Avro15SpecificDatumReaderAccessUtil.newInstancePlease(clazz, schema);
  }

  @Override
  public Object getSpecificDefaultValue(Schema.Field field) {
    return Avro15DefaultValuesCache.getDefaultValue(field, true);
  }

  @Override
  public GenericData.EnumSymbol newEnumSymbol(Schema enumSchema, String enumValue) {
    return new GenericData.EnumSymbol(enumSchema, enumValue);
  }

  @Override
  public GenericData.Fixed newFixedField(Schema fixedSchema) {
    return new GenericData.Fixed(fixedSchema);
  }

  @Override
  public GenericData.Fixed newFixedField(Schema fixedSchema, byte[] contents) {
    return new GenericData.Fixed(fixedSchema, contents);
  }

  @Override
  public Object getGenericDefaultValue(Schema.Field field) {
    return Avro15DefaultValuesCache.getDefaultValue(field, false);
  }

  @Override
  public boolean fieldHasDefault(Schema.Field field) {
    return null != field.defaultValue();
  }

  @Override
  public boolean defaultValuesEqual(Schema.Field a, Schema.Field b, boolean allowLooseNumerics) {
    JsonNode aVal = a.defaultValue();
    JsonNode bVal = b.defaultValue();
    return Jackson1Utils.JsonNodesEqual(aVal, bVal, allowLooseNumerics);
  }

  @Override
  public Set<String> getFieldAliases(Schema.Field field) {
    try {
      @SuppressWarnings("unchecked")
      Set<String> raw = (Set<String>) fieldAliasesField.get(field);
      if (raw == null || raw.isEmpty()) {
        return Collections.emptySet();
      }
      return Collections.unmodifiableSet(raw); //defensive
    } catch (Exception e) {
      throw new IllegalStateException("cant access field aliases", e);
    }
  }

  @Override
  public FieldBuilder newFieldBuilder(Schema.Field other) {
    return new FieldBuilder15(other);
  }

  @Override
  public SchemaBuilder newSchemaBuilder(Schema other) {
    return new SchemaBuilder15(this, other);
  }

  @Override
  public String getFieldPropAsJsonString(Schema.Field field, String name) {
    return StringPropertyUtils.getFieldPropAsJsonString(field, name);
  }

  @Override
  public void setFieldPropFromJsonString(Schema.Field field, String name, String value, boolean strict) {
    StringPropertyUtils.setFieldPropFromJsonString(field, name, value, strict);
  }

  private Map<String, String> getPropsMap(Schema schema) {
    try {
      //noinspection unchecked
      return (Map<String, String>) schemaPropsField.get(schema);
    } catch (Exception e) {
      throw new IllegalStateException("unable to access Schema.Field.props", e);
    }
  }

  private Map<String, String> getPropsMap(Schema.Field field) {
    try {
      //noinspection unchecked
      return (Map<String, String>) fieldPropsField.get(field);
    } catch (Exception e) {
      throw new IllegalStateException("unable to access Schema.Field.props", e);
    }
  }

  @Override
  public boolean sameJsonProperties(Schema.Field a, Schema.Field b, boolean compareStringProps, boolean compareNonStringProps) {
    if (compareNonStringProps) {
      throw new IllegalArgumentException("avro " + supportedMajorVersion()
          + " does not preserve non-string props and so cannot compare them");
    }
    if (a == null || b == null) {
      return false;
    }
    if (!compareStringProps) {
      return true;
    }
    Map<String, String> aProps = getPropsMap(a);
    Map<String, String> bProps = getPropsMap(b);
    return Objects.equals(aProps, bProps);
  }

  @Override
  public String getSchemaPropAsJsonString(Schema schema, String name) {
    return StringPropertyUtils.getSchemaPropAsJsonString(schema, name);
  }

  @Override
  public void setSchemaPropFromJsonString(Schema schema, String name, String value, boolean strict) {
    StringPropertyUtils.setSchemaPropFromJsonString(schema, name, value, strict);
  }

  @Override
  public boolean sameJsonProperties(Schema a, Schema b, boolean compareStringProps, boolean compareNonStringProps) {
    if (compareNonStringProps) {
      throw new IllegalArgumentException("avro " + supportedMajorVersion()
          + " does not preserve non-string props and so cannot compare them");
    }
    if (a == null || b == null) {
      return false;
    }
    if (!compareStringProps) {
      return true;
    }
    Map<String, String> aProps = getPropsMap(a);
    Map<String, String> bProps = getPropsMap(b);
    return Objects.equals(aProps, bProps);
  }

  @Override
  public List<String> getAllPropNames(Schema schema) {
    return new ArrayList<>(getPropsMap(schema).keySet());
  }

  @Override
  public List<String> getAllPropNames(Schema.Field field) {
    return new ArrayList<>(getPropsMap(field).keySet());
  }

  @Override
  public String getEnumDefault(Schema s) {
    return s.getProp("default");
  }

  @Override
  public String toAvsc(Schema schema, AvscGenerationConfig config) {
    boolean useRuntime;
    if (!isRuntimeAvroCapableOf(config)) {
      if (config.isForceUseOfRuntimeAvro()) {
        throw new UnsupportedOperationException("desired configuration " + config
                + " is forced yet runtime avro " + supportedMajorVersion() + " is not capable of it");
      }
      useRuntime = false;
    } else {
      useRuntime = config.isPreferUseOfRuntimeAvro();
    }

    if (useRuntime) {
      return schema.toString(config.isPrettyPrint());
    } else {
      //if the user does not specify do whatever runtime avro would (which for 1.5 means produce correct schema)
      boolean usePre702Logic = config.getRetainPreAvro702Logic().orElse(Boolean.FALSE);
      Avro15AvscWriter writer = new Avro15AvscWriter(
              config.isPrettyPrint(),
              usePre702Logic,
              config.isAddAvro702Aliases()
      );
      return writer.toAvsc(schema);
    }
  }

  @Override
  public Collection<AvroGeneratedSourceCode> compile(
      Collection<Schema> toCompile,
      AvroVersion minSupportedVersion,
      AvroVersion maxSupportedVersion,
      CodeGenerationConfig config
  ) {
    if (!compilerSupported) {
      throw new UnsupportedOperationException(compilerSupportMessage, compilerSupportIssue);
    }
    if (!StringRepresentation.CharSequence.equals(config.getStringRepresentation())) {
      throw new UnsupportedOperationException("generating String fields as " + config.getStringRepresentation() + " unsupported under avro " + supportedMajorVersion());
    }
    if (toCompile == null || toCompile.isEmpty()) {
      return Collections.emptyList();
    }

    Map<String, String> fullNameToAlternativeAvsc;
    if (!config.isAvro702HandlingEnabled()) {
      fullNameToAlternativeAvsc = Collections.emptyMap();
    } else {
      fullNameToAlternativeAvsc = createAlternativeAvscs(toCompile, config);
    }

    Iterator<Schema> schemaIter = toCompile.iterator();
    Schema first = schemaIter.next();
    try {
      //since avro-compiler may not be on the CP, we use pure reflection to deal with the compiler
      Object compiler = specificCompilerCtr.newInstance(first);

      //the 1.5 compiler has nothing we can configure

      while (schemaIter.hasNext()) {
        compilerEnqueueMethod.invoke(compiler, schemaIter.next());
      }

      Collection<?> outputFiles = (Collection<?>) compilerCompileMethod.invoke(compiler);

      List<AvroGeneratedSourceCode> sourceFiles = new ArrayList<>(outputFiles.size());
      for (Object outputFile : outputFiles) {
        AvroGeneratedSourceCode sourceCode = new AvroGeneratedSourceCode(getPath(outputFile), getContents(outputFile));
        String altAvsc = fullNameToAlternativeAvsc.get(sourceCode.getFullyQualifiedClassName());
        if (altAvsc != null) {
          sourceCode.setAlternativeAvsc(altAvsc);
        }
        sourceFiles.add(sourceCode);
      }

      return transform(sourceFiles, minSupportedVersion, maxSupportedVersion);
    } catch (UnsupportedOperationException e) {
      throw e; //as-is
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private Collection<AvroGeneratedSourceCode> transform(List<AvroGeneratedSourceCode> avroGenerated, AvroVersion minAvro, AvroVersion maxAvro) {
    List<AvroGeneratedSourceCode> transformed = new ArrayList<>(avroGenerated.size());
    for (AvroGeneratedSourceCode generated : avroGenerated) {
      String fixed = CodeTransformations.applyAll(
          generated.getContents(),
          supportedMajorVersion(),
          minAvro,
          maxAvro,
          generated.getAlternativeAvsc()
      );
      transformed.add(new AvroGeneratedSourceCode(generated.getPath(), fixed));
    }
    return transformed;
  }

  private String getPath(Object shouldBeOutputFile) {
    try {
      return (String) outputFilePathField.get(shouldBeOutputFile);
    } catch (Exception e) {
      throw new IllegalStateException("cant extract path from avro OutputFile", e);
    }
  }

  private String getContents(Object shouldBeOutputFile) {
    try {
      return (String) outputFileContentsField.get(shouldBeOutputFile);
    } catch (Exception e) {
      throw new IllegalStateException("cant extract contents from avro OutputFile", e);
    }
  }

  private boolean isRuntimeAvroCapableOf(AvscGenerationConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("config cannot be null");
    }
    if (config.isAddAvro702Aliases()) {
      return false;
    }
    Optional<Boolean> preAvro702Output = config.getRetainPreAvro702Logic();
    //noinspection RedundantIfStatement
    if (preAvro702Output.isPresent() && preAvro702Output.get().equals(Boolean.TRUE)) {
      //avro 1.5 can only do correct avsc
      return false;
    }
    return true;
  }
}
