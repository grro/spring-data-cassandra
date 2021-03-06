/*
 * Copyright 2013-2014 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cassandra.core.keyspace;

import static org.springframework.cassandra.core.cql.CqlIdentifier.cqlId;

import org.springframework.cassandra.core.cql.CqlIdentifier;
import org.springframework.util.Assert;

import com.datastax.driver.core.DataType;

/**
 * Base class for column changes that include {@link DataType} information.
 * 
 * @author Matthew T. Adams
 */
public abstract class ColumnTypeChangeSpecification extends ColumnChangeSpecification {

	private DataType type;

	public ColumnTypeChangeSpecification(String name, DataType type) {
		this(cqlId(name), type);
	}

	public ColumnTypeChangeSpecification(CqlIdentifier name, DataType type) {
		super(name);
		setType(type);
	}

	private void setType(DataType type) {
		Assert.notNull(type);
		this.type = type;
	}

	public DataType getType() {
		return type;
	}
}
