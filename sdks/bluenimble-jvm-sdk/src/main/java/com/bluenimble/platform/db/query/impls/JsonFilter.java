/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluenimble.platform.db.query.impls;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.db.query.Filter;
import com.bluenimble.platform.db.query.Query;
import com.bluenimble.platform.db.query.Query.Conjunction;
import com.bluenimble.platform.db.query.Query.Operator;
import com.bluenimble.platform.json.JsonObject;

public class JsonFilter implements Filter {

	private static final long serialVersionUID = -4482690420390364506L;
	
	private static final Set<String> Conjunctions = new HashSet<String> ();
	static {
		Conjunctions.add (Query.Conjunction.and.name ());
		Conjunctions.add (Query.Conjunction.or.name ());
	}
	
	protected JsonObject 	source;
	protected Conjunction 	conjuction;
	
	protected Conjunction 	parentConjuction;
	
	public JsonFilter (JsonObject source) {
		this (null, source);
	}

	public JsonFilter (Conjunction parentConjuction, JsonObject source) {
		this.parentConjuction 	= parentConjuction;
		conjuction 				= Conjunction.valueOf (Json.getString (source, JsonQuery.Spec.Conjunction, Query.Conjunction.and.name ()));
		
		if (source != null) {
			source.remove (JsonQuery.Spec.Conjunction);
		}
		this.source = source;
	}

	@Override
	public Conjunction conjunction () {
		return conjuction;
	}

	@Override
	public Iterator<String> conditions () {
		if (source == null) {
			return null;
		}
		return source.keys ();
	}

	@Override
	public Object get (String field) {
		Object o = source.get (field);
		if (!(o instanceof JsonObject)) {
			return new ConditionImpl (field, Operator.eq, o);
		}
		JsonObject spec = (JsonObject)o;
		if (Conjunctions.contains (field)) {
			return new JsonFilter (Conjunction.valueOf (field), spec);
		} 
		return new ConditionImpl (
			field, 
			Operator.valueOf (Json.getString (spec, JsonQuery.Spec.Operator, Operator.eq.name ())), 
			spec.get (JsonQuery.Spec.Value)
		);
	}

	@Override
	public void set (String field, Operator operator, Object value) {
		source.set (field, new JsonObject ().set (JsonQuery.Spec.Operator, operator.name ()).set (JsonQuery.Spec.Value, value));
	}

	@Override
	public int count () {
		if (source == null) {
			return 0;
		}
		return source.size ();
	}
	
	@Override
	public boolean isEmpty () {
		if (source == null) {
			return true;
		}
		return source.isEmpty ();
	}

	@Override
	public Conjunction parentConjunction () {
		return parentConjuction;
	}
	
}
