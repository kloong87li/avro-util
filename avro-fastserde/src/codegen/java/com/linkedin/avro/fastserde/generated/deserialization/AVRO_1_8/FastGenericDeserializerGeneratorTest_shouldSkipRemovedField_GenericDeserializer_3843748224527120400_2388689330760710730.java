
package com.linkedin.avro.fastserde.generated.deserialization.AVRO_1_8;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.linkedin.avro.fastserde.FastDeserializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.util.Utf8;

public class FastGenericDeserializerGeneratorTest_shouldSkipRemovedField_GenericDeserializer_3843748224527120400_2388689330760710730
    implements FastDeserializer<IndexedRecord>
{

    private final Schema readerSchema;
    private final Schema testNotRemoved0;
    private final Schema testNotRemoved20;
    private final Schema subRecord0;
    private final Schema subRecordOptionSchema0;
    private final Schema testNotRemoved1;
    private final Schema testNotRemoved21;
    private final Schema subRecordMap0;
    private final Schema subRecordArray0;

    public FastGenericDeserializerGeneratorTest_shouldSkipRemovedField_GenericDeserializer_3843748224527120400_2388689330760710730(Schema readerSchema) {
        this.readerSchema = readerSchema;
        this.testNotRemoved0 = readerSchema.getField("testNotRemoved").schema();
        this.testNotRemoved20 = readerSchema.getField("testNotRemoved2").schema();
        this.subRecord0 = readerSchema.getField("subRecord").schema();
        this.subRecordOptionSchema0 = subRecord0 .getTypes().get(1);
        this.testNotRemoved1 = subRecordOptionSchema0 .getField("testNotRemoved").schema();
        this.testNotRemoved21 = subRecordOptionSchema0 .getField("testNotRemoved2").schema();
        this.subRecordMap0 = readerSchema.getField("subRecordMap").schema();
        this.subRecordArray0 = readerSchema.getField("subRecordArray").schema();
    }

    public IndexedRecord deserialize(IndexedRecord reuse, Decoder decoder)
        throws IOException
    {
        return deserializeFastGenericDeserializerGeneratorTest_shouldSkipRemovedField0((reuse), (decoder));
    }

    public IndexedRecord deserializeFastGenericDeserializerGeneratorTest_shouldSkipRemovedField0(Object reuse, Decoder decoder)
        throws IOException
    {
        IndexedRecord FastGenericDeserializerGeneratorTest_shouldSkipRemovedField;
        if ((((reuse)!= null)&&((reuse) instanceof IndexedRecord))&&(((IndexedRecord)(reuse)).getSchema() == readerSchema)) {
            FastGenericDeserializerGeneratorTest_shouldSkipRemovedField = ((IndexedRecord)(reuse));
        } else {
            FastGenericDeserializerGeneratorTest_shouldSkipRemovedField = new org.apache.avro.generic.GenericData.Record(readerSchema);
        }
        int unionIndex0 = (decoder.readIndex());
        if (unionIndex0 == 0) {
            decoder.readNull();
            FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.put(0, null);
        } else {
            if (unionIndex0 == 1) {
                Object oldString0 = FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.get(0);
                if (oldString0 instanceof Utf8) {
                    FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.put(0, (decoder).readString(((Utf8) oldString0)));
                } else {
                    FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.put(0, (decoder).readString(null));
                }
            } else {
                throw new RuntimeException(("Illegal union index for 'testNotRemoved': "+ unionIndex0));
            }
        }
        populate_FastGenericDeserializerGeneratorTest_shouldSkipRemovedField0((FastGenericDeserializerGeneratorTest_shouldSkipRemovedField), (decoder));
        populate_FastGenericDeserializerGeneratorTest_shouldSkipRemovedField1((FastGenericDeserializerGeneratorTest_shouldSkipRemovedField), (decoder));
        populate_FastGenericDeserializerGeneratorTest_shouldSkipRemovedField2((FastGenericDeserializerGeneratorTest_shouldSkipRemovedField), (decoder));
        return FastGenericDeserializerGeneratorTest_shouldSkipRemovedField;
    }

    private void populate_FastGenericDeserializerGeneratorTest_shouldSkipRemovedField0(IndexedRecord FastGenericDeserializerGeneratorTest_shouldSkipRemovedField, Decoder decoder)
        throws IOException
    {
        int unionIndex1 = (decoder.readIndex());
        if (unionIndex1 == 0) {
            decoder.readNull();
        } else {
            if (unionIndex1 == 1) {
                decoder.skipString();
            } else {
                throw new RuntimeException(("Illegal union index for 'testRemoved': "+ unionIndex1));
            }
        }
        int unionIndex2 = (decoder.readIndex());
        if (unionIndex2 == 0) {
            decoder.readNull();
            FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.put(1, null);
        } else {
            if (unionIndex2 == 1) {
                Object oldString1 = FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.get(1);
                if (oldString1 instanceof Utf8) {
                    FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.put(1, (decoder).readString(((Utf8) oldString1)));
                } else {
                    FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.put(1, (decoder).readString(null));
                }
            } else {
                throw new RuntimeException(("Illegal union index for 'testNotRemoved2': "+ unionIndex2));
            }
        }
    }

    private void populate_FastGenericDeserializerGeneratorTest_shouldSkipRemovedField1(IndexedRecord FastGenericDeserializerGeneratorTest_shouldSkipRemovedField, Decoder decoder)
        throws IOException
    {
        int unionIndex3 = (decoder.readIndex());
        if (unionIndex3 == 0) {
            decoder.readNull();
            FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.put(2, null);
        } else {
            if (unionIndex3 == 1) {
                FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.put(2, deserializesubRecord0(FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.get(2), (decoder)));
            } else {
                throw new RuntimeException(("Illegal union index for 'subRecord': "+ unionIndex3));
            }
        }
        Map<Utf8, IndexedRecord> subRecordMap1 = null;
        long chunkLen0 = (decoder.readMapStart());
        if (chunkLen0 > 0) {
            Map<Utf8, IndexedRecord> subRecordMapReuse0 = null;
            Object oldMap0 = FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.get(3);
            if (oldMap0 instanceof Map) {
                subRecordMapReuse0 = ((Map) oldMap0);
            }
            if (subRecordMapReuse0 != (null)) {
                subRecordMapReuse0 .clear();
                subRecordMap1 = subRecordMapReuse0;
            } else {
                subRecordMap1 = new HashMap<Utf8, IndexedRecord>(((int)(((chunkLen0 * 4)+ 2)/ 3)));
            }
            do {
                for (int counter0 = 0; (counter0 <chunkLen0); counter0 ++) {
                    Utf8 key0 = (decoder.readString(null));
                    subRecordMap1 .put(key0, deserializesubRecord0(null, (decoder)));
                }
                chunkLen0 = (decoder.mapNext());
            } while (chunkLen0 > 0);
        } else {
            subRecordMap1 = new HashMap<Utf8, IndexedRecord>(0);
        }
        FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.put(3, subRecordMap1);
    }

    public IndexedRecord deserializesubRecord0(Object reuse, Decoder decoder)
        throws IOException
    {
        IndexedRecord subRecord;
        if ((((reuse)!= null)&&((reuse) instanceof IndexedRecord))&&(((IndexedRecord)(reuse)).getSchema() == subRecordOptionSchema0)) {
            subRecord = ((IndexedRecord)(reuse));
        } else {
            subRecord = new org.apache.avro.generic.GenericData.Record(subRecordOptionSchema0);
        }
        int unionIndex4 = (decoder.readIndex());
        if (unionIndex4 == 0) {
            decoder.readNull();
            subRecord.put(0, null);
        } else {
            if (unionIndex4 == 1) {
                Object oldString2 = subRecord.get(0);
                if (oldString2 instanceof Utf8) {
                    subRecord.put(0, (decoder).readString(((Utf8) oldString2)));
                } else {
                    subRecord.put(0, (decoder).readString(null));
                }
            } else {
                throw new RuntimeException(("Illegal union index for 'testNotRemoved': "+ unionIndex4));
            }
        }
        populate_subRecord0((subRecord), (decoder));
        return subRecord;
    }

    private void populate_subRecord0(IndexedRecord subRecord, Decoder decoder)
        throws IOException
    {
        int unionIndex5 = (decoder.readIndex());
        if (unionIndex5 == 0) {
            decoder.readNull();
        } else {
            if (unionIndex5 == 1) {
                decoder.skipString();
            } else {
                throw new RuntimeException(("Illegal union index for 'testRemoved': "+ unionIndex5));
            }
        }
        int unionIndex6 = (decoder.readIndex());
        if (unionIndex6 == 0) {
            decoder.readNull();
            subRecord.put(1, null);
        } else {
            if (unionIndex6 == 1) {
                Object oldString3 = subRecord.get(1);
                if (oldString3 instanceof Utf8) {
                    subRecord.put(1, (decoder).readString(((Utf8) oldString3)));
                } else {
                    subRecord.put(1, (decoder).readString(null));
                }
            } else {
                throw new RuntimeException(("Illegal union index for 'testNotRemoved2': "+ unionIndex6));
            }
        }
    }

    private void populate_FastGenericDeserializerGeneratorTest_shouldSkipRemovedField2(IndexedRecord FastGenericDeserializerGeneratorTest_shouldSkipRemovedField, Decoder decoder)
        throws IOException
    {
        List<IndexedRecord> subRecordArray1 = null;
        long chunkLen1 = (decoder.readArrayStart());
        Object oldArray0 = FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.get(4);
        if (oldArray0 instanceof List) {
            subRecordArray1 = ((List) oldArray0);
            subRecordArray1 .clear();
        } else {
            subRecordArray1 = new org.apache.avro.generic.GenericData.Array<IndexedRecord>(((int) chunkLen1), subRecordArray0);
        }
        while (chunkLen1 > 0) {
            for (int counter1 = 0; (counter1 <chunkLen1); counter1 ++) {
                Object subRecordArrayArrayElementReuseVar0 = null;
                if (oldArray0 instanceof GenericArray) {
                    subRecordArrayArrayElementReuseVar0 = ((GenericArray) oldArray0).peek();
                }
                subRecordArray1 .add(deserializesubRecord0(subRecordArrayArrayElementReuseVar0, (decoder)));
            }
            chunkLen1 = (decoder.arrayNext());
        }
        FastGenericDeserializerGeneratorTest_shouldSkipRemovedField.put(4, subRecordArray1);
    }

}
