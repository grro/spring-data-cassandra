/*
 * Copyright 2016 the original author or authors.
 *
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
 */

package org.springframework.data.cassandra.convert;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assume.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cassandra.core.PrimaryKeyType;
import org.springframework.core.SpringVersion;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext;
import org.springframework.data.cassandra.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.mapping.CassandraType;
import org.springframework.data.cassandra.mapping.PrimaryKey;
import org.springframework.data.cassandra.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.mapping.Table;
import org.springframework.test.util.ReflectionTestUtils;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType.Name;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Assignment;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Delete.Where;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import com.datastax.driver.core.querybuilder.Update.Assignments;

/**
 * Unit tests for {@link MappingCassandraConverter}.
 *
 * @author Mark Paluch
 * @soundtrack Outlandich - Dont Leave Me Feat Cyt (Sun Kidz Electrocore Mix)
 */
@RunWith(MockitoJUnitRunner.class)
public class MappingCassandraConverterUnitTests {

	@Rule public final ExpectedException expectedException = ExpectedException.none();
	@Mock private Row rowMock;
	@Mock private ColumnDefinitions columnDefinitionsMock;

	private CassandraMappingContext mappingContext;
	private MappingCassandraConverter mappingCassandraConverter;

	@Before
	public void setUp() throws Exception {

		mappingContext = new BasicCassandraMappingContext();
		mappingCassandraConverter = new MappingCassandraConverter(mappingContext);

		mappingCassandraConverter.afterPropertiesSet();
	}

	/**
	 * @see DATACASS-260
	 */
	@Test
	public void insertEnumShouldMapToString() {

		WithEnumColumns withEnumColumns = new WithEnumColumns();
		withEnumColumns.setCondition(Condition.MINT);

		Insert insert = QueryBuilder.insertInto("table");

		mappingCassandraConverter.write(withEnumColumns, insert);

		assertThat(getValues(insert), contains((Object) "MINT"));
	}

	/**
	 * @see DATACASS-260
	 */
	@Test
	public void insertEnumDoesNotMapToOrdinalBeforeSpring43() {

		assumeThat(SpringVersion.getVersion(), not(startsWith("4.3")));

		expectedException.expect(ConverterNotFoundException.class);
		expectedException.expectMessage(allOf(containsString("No converter found"), containsString("java.lang.Integer")));

		UnsupportedEnumToOrdinalMapping unsupportedEnumToOrdinalMapping = new UnsupportedEnumToOrdinalMapping();
		unsupportedEnumToOrdinalMapping.setAsOrdinal(Condition.MINT);

		Insert insert = QueryBuilder.insertInto("table");

		mappingCassandraConverter.write(unsupportedEnumToOrdinalMapping, insert);
	}

	/**
	 * @see DATACASS-255
	 */
	@Test
	public void insertEnumMapsToOrdinalWithSpring43() {

		assumeThat(SpringVersion.getVersion(), startsWith("4.3"));

		UnsupportedEnumToOrdinalMapping unsupportedEnumToOrdinalMapping = new UnsupportedEnumToOrdinalMapping();
		unsupportedEnumToOrdinalMapping.setAsOrdinal(Condition.USED);

		Insert insert = QueryBuilder.insertInto("table");

		mappingCassandraConverter.write(unsupportedEnumToOrdinalMapping, insert);

		assertThat(getValues(insert), contains((Object) Integer.valueOf(Condition.USED.ordinal())));
	}

	/**
	 * @see DATACASS-260
	 */
	@Test
	public void insertEnumAsPrimaryKeyShouldMapToString() {

		EnumPrimaryKey key = new EnumPrimaryKey();
		key.setCondition(Condition.MINT);

		Insert insert = QueryBuilder.insertInto("table");

		mappingCassandraConverter.write(key, insert);

		assertThat(getValues(insert), contains((Object) "MINT"));
	}

	/**
	 * @see DATACASS-260
	 */
	@Test
	public void insertEnumInCompositePrimaryKeyShouldMapToString() {

		EnumCompositePrimaryKey key = new EnumCompositePrimaryKey();
		key.setCondition(Condition.MINT);

		CompositeKeyThing composite = new CompositeKeyThing();
		composite.setKey(key);

		Insert insert = QueryBuilder.insertInto("table");

		mappingCassandraConverter.write(composite, insert);

		assertThat(getValues(insert), contains((Object) "MINT"));
	}

	/**
	 * @see DATACASS-260
	 */
	@Test
	public void updateEnumShouldMapToString() {

		WithEnumColumns withEnumColumns = new WithEnumColumns();
		withEnumColumns.setCondition(Condition.MINT);

		Update update = QueryBuilder.update("table");

		mappingCassandraConverter.write(withEnumColumns, update);

		assertThat(getAssignmentValues(update), contains((Object) "MINT"));
	}

	/**
	 * @see DATACASS-260
	 */
	@Test
	public void updateEnumAsPrimaryKeyShouldMapToString() {

		EnumPrimaryKey key = new EnumPrimaryKey();
		key.setCondition(Condition.MINT);

		Update update = QueryBuilder.update("table");

		mappingCassandraConverter.write(key, update);

		assertThat(getWhereValues(update), contains((Object) "MINT"));
	}

	/**
	 * @see DATACASS-260
	 */
	@Test
	public void updateEnumInCompositePrimaryKeyShouldMapToString() {

		EnumCompositePrimaryKey key = new EnumCompositePrimaryKey();
		key.setCondition(Condition.MINT);

		CompositeKeyThing composite = new CompositeKeyThing();
		composite.setKey(key);

		Update update = QueryBuilder.update("table");

		mappingCassandraConverter.write(composite, update);

		assertThat(getWhereValues(update), contains((Object) "MINT"));
	}

	/**
	 * @see DATACASS-260
	 */
	@Test
	public void whereEnumAsPrimaryKeyShouldMapToString() {

		EnumPrimaryKey key = new EnumPrimaryKey();
		key.setCondition(Condition.MINT);

		Where where = QueryBuilder.delete().from("table").where();

		mappingCassandraConverter.write(key, where);

		assertThat(getWhereValues(where), contains((Object) "MINT"));
	}

	/**
	 * @see DATACASS-260
	 */
	@Test
	public void whereEnumInCompositePrimaryKeyShouldMapToString() {

		EnumCompositePrimaryKey key = new EnumCompositePrimaryKey();
		key.setCondition(Condition.MINT);

		CompositeKeyThing composite = new CompositeKeyThing();
		composite.setKey(key);

		Where where = QueryBuilder.delete().from("table").where();

		mappingCassandraConverter.write(composite, where);

		assertThat(getWhereValues(where), contains((Object) "MINT"));
	}

	/**
	 * @see DATACASS-280
	 */
	@Test
	public void shouldReadStringCorrectly() {

		when(rowMock.getString(0)).thenReturn("foo");

		String result = mappingCassandraConverter.readRow(String.class, rowMock);

		assertThat(result, is(equalTo("foo")));
	}

	/**
	 * @see DATACASS-280
	 */
	@Test
	public void shouldReadIntegerCorrectly() {

		when(rowMock.getObject(0)).thenReturn(2);

		Integer result = mappingCassandraConverter.readRow(Integer.class, rowMock);

		assertThat(result, is(equalTo(2)));
	}

	/**
	 * @see DATACASS-280
	 */
	@Test
	public void shouldReadLongCorrectly() {

		when(rowMock.getObject(0)).thenReturn(2);

		Long result = mappingCassandraConverter.readRow(Long.class, rowMock);

		assertThat(result, is(equalTo(2L)));
	}

	/**
	 * @see DATACASS-280
	 */
	@Test
	public void shouldReadDoubleCorrectly() {

		when(rowMock.getObject(0)).thenReturn(2D);

		Double result = mappingCassandraConverter.readRow(Double.class, rowMock);

		assertThat(result, is(equalTo(2D)));
	}

	/**
	 * @see DATACASS-280
	 */
	@Test
	public void shouldReadFloatCorrectly() {

		when(rowMock.getObject(0)).thenReturn(2F);

		Float result = mappingCassandraConverter.readRow(Float.class, rowMock);

		assertThat(result, is(equalTo(2F)));
	}

	/**
	 * @see DATACASS-280
	 */
	@Test
	public void shouldReadBigIntegerCorrectly() {

		when(rowMock.getObject(0)).thenReturn(BigInteger.valueOf(2));

		BigInteger result = mappingCassandraConverter.readRow(BigInteger.class, rowMock);

		assertThat(result, is(equalTo(BigInteger.valueOf(2))));
	}

	/**
	 * @see DATACASS-280
	 */
	@Test
	public void shouldReadBigDecimalCorrectly() {

		when(rowMock.getObject(0)).thenReturn(BigDecimal.valueOf(2));

		BigDecimal result = mappingCassandraConverter.readRow(BigDecimal.class, rowMock);

		assertThat(result, is(equalTo(BigDecimal.valueOf(2))));
	}

	/**
	 * @see DATACASS-280
	 */
	@Test
	public void shouldReadUUIDCorrectly() {

		UUID uuid = UUID.randomUUID();
		when(rowMock.getUUID(0)).thenReturn(uuid);

		UUID result = mappingCassandraConverter.readRow(UUID.class, rowMock);

		assertThat(result, is(equalTo(uuid)));
	}

	/**
	 * @see DATACASS-280
	 */
	@Test
	public void shouldReadInetAddressCorrectly() throws UnknownHostException {

		InetAddress localHost = InetAddress.getLocalHost();
		when(rowMock.getInet(0)).thenReturn(localHost);

		InetAddress result = mappingCassandraConverter.readRow(InetAddress.class, rowMock);

		assertThat(result, is(equalTo(localHost)));
	}

	/**
	 * @see DATACASS-280
	 */
	@Test
	public void shouldReadDateCorrectly() throws UnknownHostException {

		Date date = new Date(1);
		when(rowMock.getDate(0)).thenReturn(date);

		Date result = mappingCassandraConverter.readRow(Date.class, rowMock);

		assertThat(result, is(equalTo(date)));
	}

	/**
	 * @see DATACASS-280
	 */
	@Test
	public void shouldReadBooleanCorrectly() throws UnknownHostException {

		when(rowMock.getBool(0)).thenReturn(true);

		Boolean result = mappingCassandraConverter.readRow(Boolean.class, rowMock);

		assertThat(result, is(equalTo(true)));
	}

	@SuppressWarnings("unchecked")
	private List<Object> getValues(Insert statement) {
		return (List<Object>) ReflectionTestUtils.getField(statement, "values");
	}

	@SuppressWarnings("unchecked")
	private List<Object> getAssignmentValues(Update statement) {

		List<Object> result = new ArrayList<Object>();

		Assignments assignments = (Assignments) ReflectionTestUtils.getField(statement, "assignments");
		List<Assignment> listOfAssignments = (List<Assignment>) ReflectionTestUtils.getField(assignments, "assignments");
		for (Assignment assignment : listOfAssignments) {
			result.add(ReflectionTestUtils.getField(assignment, "value"));
		}

		return result;
	}

	private List<Object> getWhereValues(Update statement) {
		return getWhereValues(statement.where());
	}

	private List<Object> getWhereValues(BuiltStatement where) {

		List<Object> result = new ArrayList<Object>();

		List<Clause> clauses = (List<Clause>) ReflectionTestUtils.getField(where, "clauses");
		for (Clause clause : clauses) {
			result.add(ReflectionTestUtils.getField(clause, "value"));
		}

		return result;
	}

	@Table
	public static class UnsupportedEnumToOrdinalMapping {

		@PrimaryKey private String id;

		@CassandraType(type = Name.INT) private Condition asOrdinal;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Condition getAsOrdinal() {
			return asOrdinal;
		}

		public void setAsOrdinal(Condition asOrdinal) {
			this.asOrdinal = asOrdinal;
		}
	}

	@Table
	public static class WithEnumColumns {

		@PrimaryKey private String id;

		private Condition condition;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Condition getCondition() {
			return condition;
		}

		public void setCondition(Condition condition) {
			this.condition = condition;
		}
	}

	@PrimaryKeyClass
	public static class EnumCompositePrimaryKey implements Serializable {

		@PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.PARTITIONED) private Condition condition;

		public EnumCompositePrimaryKey() {
		}

		public EnumCompositePrimaryKey(Condition condition) {
			this.condition = condition;
		}

		public Condition getCondition() {
			return condition;
		}

		public void setCondition(Condition condition) {
			this.condition = condition;
		}
	}

	@Table
	public static class EnumPrimaryKey {

		@PrimaryKey private Condition condition;

		public Condition getCondition() {
			return condition;
		}

		public void setCondition(Condition condition) {
			this.condition = condition;
		}
	}

	@Table
	public static class CompositeKeyThing {

		@PrimaryKey private EnumCompositePrimaryKey key;

		public CompositeKeyThing() {
		}

		public CompositeKeyThing(EnumCompositePrimaryKey key) {
			this.key = key;
		}

		public EnumCompositePrimaryKey getKey() {
			return key;
		}

		public void setKey(EnumCompositePrimaryKey key) {
			this.key = key;
		}
	}

	public static enum Condition {
		MINT, USED;
	}
}
