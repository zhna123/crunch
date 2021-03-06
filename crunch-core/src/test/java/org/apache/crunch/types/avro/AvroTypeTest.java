/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.crunch.types.avro;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.crunch.Pair;
import org.apache.crunch.TupleN;
import org.apache.crunch.test.Person;
import org.apache.crunch.test.StringWrapper;
import org.apache.crunch.types.PType;
import org.apache.crunch.types.PTypes;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

public class AvroTypeTest {

  @Test
  public void testIsSpecific_SpecificData() {
    assertTrue(Avros.records(Person.class).hasSpecific());
  }

  @Test
  public void testIsGeneric_SpecificData() {
    assertFalse(Avros.records(Person.class).isGeneric());
  }

  @Test
  public void testIsSpecific_GenericData() {
    assertFalse(Avros.generics(Person.SCHEMA$).hasSpecific());
  }

  @Test
  public void testIsGeneric_GenericData() {
    assertTrue(Avros.generics(Person.SCHEMA$).isGeneric());
  }

  @Test
  public void testIsSpecific_NonAvroClass() {
    assertFalse(Avros.ints().hasSpecific());
  }

  @Test
  public void testIsGeneric_NonAvroClass() {
    assertFalse(Avros.ints().isGeneric());
  }

  @Test
  public void testIsSpecific_SpecificAvroTable() {
    assertTrue(Avros.tableOf(Avros.strings(), Avros.records(Person.class)).hasSpecific());
  }

  @Test
  public void testIsGeneric_SpecificAvroTable() {
    assertFalse(Avros.tableOf(Avros.strings(), Avros.records(Person.class)).isGeneric());
  }

  @Test
  public void testIsSpecific_GenericAvroTable() {
    assertFalse(Avros.tableOf(Avros.strings(), Avros.generics(Person.SCHEMA$)).hasSpecific());
  }

  @Test
  public void testIsGeneric_GenericAvroTable() {
    assertFalse(Avros.tableOf(Avros.strings(), Avros.generics(Person.SCHEMA$)).isGeneric());
  }

  @Test
  public void testIsReflect_GenericType() {
    assertFalse(Avros.generics(Person.SCHEMA$).hasReflect());
  }

  @Test
  public void testIsReflect_SpecificType() {
    assertFalse(Avros.records(Person.class).hasReflect());
  }

  @Test
  public void testIsReflect_ReflectSimpleType() {
    assertTrue(Avros.reflects(StringWrapper.class).hasReflect());
  }

  @Test
  public void testIsReflect_NonReflectSubType() {
    assertFalse(Avros.pairs(Avros.ints(), Avros.ints()).hasReflect());
  }

  @Test
  public void testIsReflect_ReflectSubType() {
    assertTrue(Avros.pairs(Avros.ints(), Avros.reflects(StringWrapper.class)).hasReflect());
  }

  @Test
  public void testIsReflect_TableOfNonReflectTypes() {
    assertFalse(Avros.tableOf(Avros.ints(), Avros.strings()).hasReflect());
  }

  @Test
  public void testIsReflect_TableWithReflectKey() {
    assertTrue(Avros.tableOf(Avros.reflects(StringWrapper.class), Avros.ints()).hasReflect());
  }

  @Test
  public void testIsReflect_TableWithReflectValue() {
    assertTrue(Avros.tableOf(Avros.ints(), Avros.reflects(StringWrapper.class)).hasReflect());
  }

  @Test
  public void testReflect_CollectionContainingReflectValue() {
    assertTrue(Avros.collections(Avros.reflects(StringWrapper.class)).hasReflect());
  }

  @Test
  public void testReflect_CollectionNotContainingReflectValue() {
    assertFalse(Avros.collections(Avros.generics(Person.SCHEMA$)).hasReflect());
  }

  @Test
  public void testStableTupleNames() {
    AvroType<Pair<Long, Float>> at1 = Avros.pairs(Avros.longs(), Avros.floats());
    AvroType<Pair<Long, Float>> at2 = Avros.pairs(Avros.longs(), Avros.floats());
    assertEquals(at1.getSchema(), at2.getSchema());
  }

  @Test
  public void testGetDetachedValue_AlreadyMappedAvroType() {
    Integer value = 42;
    AvroType<Integer> intType = Avros.ints();
    intType.initialize(new Configuration());
    Integer detachedValue = intType.getDetachedValue(value);
    assertSame(value, detachedValue);
  }

  @Test
  public void testGetDetachedValue_GenericAvroType() {
    AvroType<Record> genericType = Avros.generics(Person.SCHEMA$);
    genericType.initialize(new Configuration());
    GenericData.Record record = new GenericData.Record(Person.SCHEMA$);
    record.put("name", "name value");
    record.put("age", 42);
    record.put("siblingnames", Lists.newArrayList());

    Record detachedRecord = genericType.getDetachedValue(record);
    assertEquals(record, detachedRecord);
    assertNotSame(record, detachedRecord);
  }

  private Person createPerson() {
    Person person = new Person();
    person.name = "name value";
    person.age = 42;
    person.siblingnames = Lists.<CharSequence> newArrayList();
    return person;
  }

  @Test
  public void testGetDetachedValue_SpecificAvroType() {
    AvroType<Person> specificType = Avros.specifics(Person.class);
    specificType.initialize(new Configuration());
    Person person = createPerson();
    Person detachedPerson = specificType.getDetachedValue(person);
    assertEquals(person, detachedPerson);
    assertNotSame(person, detachedPerson);
  }

  @Test(expected = IllegalStateException.class)
  public void testGetDetachedValue_NotInitialized() {
    AvroType<Person> specificType = Avros.specifics(Person.class);
    Person person = createPerson();
    specificType.getDetachedValue(person);
  }

  @Test
  public void testGetDetachedValue_ReflectAvroType() {
    AvroType<ReflectedPerson> reflectType = Avros.reflects(ReflectedPerson.class);
    reflectType.initialize(new Configuration());
    ReflectedPerson rp = new ReflectedPerson();
    rp.setName("josh");
    rp.setAge(32);
    rp.setSiblingnames(Lists.<String>newArrayList());
    ReflectedPerson detached = reflectType.getDetachedValue(rp);
    assertEquals(rp, detached);
    assertNotSame(rp, detached);
  }

  @Test
  public void testGetDetachedValue_Pair() {
    Person person = createPerson();
    AvroType<Pair<Integer, Person>> pairType = Avros.pairs(Avros.ints(),
        Avros.records(Person.class));
    pairType.initialize(new Configuration());

    Pair<Integer, Person> inputPair = Pair.of(1, person);
    Pair<Integer, Person> detachedPair = pairType.getDetachedValue(inputPair);

    assertEquals(inputPair, detachedPair);
    assertNotSame(inputPair.second(), detachedPair.second());
  }

  @Test
  public void testGetDetachedValue_Collection() {
    Person person = createPerson();
    List<Person> personList = Lists.newArrayList(person);

    AvroType<Collection<Person>> collectionType = Avros.collections(Avros.records(Person.class));
    collectionType.initialize(new Configuration());

    Collection<Person> detachedCollection = collectionType.getDetachedValue(personList);

    assertEquals(personList, detachedCollection);
    Person detachedPerson = detachedCollection.iterator().next();

    assertNotSame(person, detachedPerson);
  }

  @Test
  public void testGetDetachedValue_Map() {
    String key = "key";
    Person value = createPerson();

    Map<String, Person> stringPersonMap = Maps.newHashMap();
    stringPersonMap.put(key, value);

    AvroType<Map<String, Person>> mapType = Avros.maps(Avros.records(Person.class));
    mapType.initialize(new Configuration());

    Map<String, Person> detachedMap = mapType.getDetachedValue(stringPersonMap);

    assertEquals(stringPersonMap, detachedMap);
    assertNotSame(value, detachedMap.get(key));
  }

  @Test
  public void testGetDetachedValue_TupleN() {
    Person person = createPerson();
    AvroType<TupleN> ptype = Avros.tuples(Avros.records(Person.class));
    ptype.initialize(new Configuration());
    TupleN tuple = new TupleN(person);
    TupleN detachedTuple = ptype.getDetachedValue(tuple);

    assertEquals(tuple, detachedTuple);
    assertNotSame(person, detachedTuple.get(0));
  }

  @Test
  public void testGetDetachedValue_ImmutableDerived() {
    PType<UUID> uuidType = PTypes.uuid(AvroTypeFamily.getInstance());
    uuidType.initialize(new Configuration());

    UUID uuid = new UUID(1L, 1L);
    UUID detached = uuidType.getDetachedValue(uuid);

    assertSame(uuid, detached);
  }

  @Test
  public void testGetDetachedValue_MutableDerived() {
    PType<StringWrapper> jsonType = PTypes.jsonString(StringWrapper.class, AvroTypeFamily.getInstance());
    jsonType.initialize(new Configuration());

    StringWrapper stringWrapper = new StringWrapper();
    stringWrapper.setValue("test");

    StringWrapper detachedValue = jsonType.getDetachedValue(stringWrapper);

    assertNotSame(stringWrapper, detachedValue);
    assertEquals(stringWrapper, detachedValue);
  }

  @Test
  public void testGetDetachedValue_Bytes() {
    byte[] buffer = new byte[]{1, 2, 3};
    AvroType<ByteBuffer> byteType = Avros.bytes();
    byteType.initialize(new Configuration());

    ByteBuffer detachedValue = byteType.getDetachedValue(ByteBuffer.wrap(buffer));

    byte[] detachedBuffer = new byte[buffer.length];
    detachedValue.get(detachedBuffer);

    assertArrayEquals(buffer, detachedBuffer);
    buffer[0] = 99;
    assertEquals(detachedBuffer[0], 1);
  }
}
