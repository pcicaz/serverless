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

import java.util.Iterator;

import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.query.CompiledQuery;
import com.bluenimble.platform.db.query.Condition;
import com.bluenimble.platform.db.query.Filter;
import com.bluenimble.platform.db.query.GroupBy;
import com.bluenimble.platform.db.query.OrderBy;
import com.bluenimble.platform.db.query.OrderByField;
import com.bluenimble.platform.db.query.Query;
import com.bluenimble.platform.db.query.QueryCompiler;
import com.bluenimble.platform.db.query.Select;

public abstract class EventedQueryCompiler implements QueryCompiler {

	private static final long serialVersionUID = -4226119119102167983L;

	protected enum Timing {
		start,
		end
	}
	
	@Override
	public CompiledQuery compile (Query query) throws DatabaseException {
		onQuery 	(Timing.start, query);
		
		// where
		processSelect (query.select ());
		
		// where
		processFilter (query.where (), true);
		
		// group by 
		processGroupBy (query.groupBy ());

		// order by 
		processOrderBy (query.orderBy ());

		onQuery 	(Timing.end, query);
		
		return done ();
	}
	
	private void processSelect (Select select) throws DatabaseException {
		onSelect (Timing.start, select);
		if (select == null || select.isEmpty ()) {
			onSelect (Timing.end, select);
			return;
		}
		for (int i = 0; i < select.count (); i++) {
			onSelectField (select.get (i), select.count (), i);
		}
		onSelect (Timing.end, select);
	}
	
	private void processFilter (Filter filter, boolean isWhere) throws DatabaseException {
		onFilter (Timing.start, filter, isWhere);
		if (filter == null || filter.isEmpty ()) {
			return;
		}
		Iterator<String> fields = filter.conditions ();
		if (fields == null) {
			return;
		}
		
		int counter = 0;
		while (fields.hasNext ()) {
			Object condition = filter.get (fields.next ());
			if (Condition.class.isAssignableFrom (condition.getClass ())) {
				onCondition ((Condition)condition, filter, counter++);
			} else if (Filter.class.isAssignableFrom (condition.getClass ())) {
				processFilter ((Filter)condition, false);
			}
		}
		onFilter (Timing.end, filter, isWhere);
	}
	
	private void processOrderBy (OrderBy orderBy) throws DatabaseException {
		onOrderBy (Timing.start, orderBy);
		if (orderBy == null || orderBy.isEmpty ()) {
			return;
		}
		Iterator<String> fields = orderBy.fields ();
		if (fields == null) {
			return;
		}
		
		int counter = 0;
		while (fields.hasNext ()) {
			onOrderByField (orderBy.get (fields.next ()), orderBy.count (), counter++);
			
		}
		onOrderBy (Timing.end, orderBy);
	}
	
	private void processGroupBy (GroupBy groupBy) throws DatabaseException {
		onGroupBy (Timing.start, groupBy);
		if (groupBy == null || groupBy.isEmpty ()) {
			return;
		}
		for (int i = 0; i < groupBy.count (); i++) {
			onGroupByField (groupBy.get (i), groupBy.count (), i);
		}
		onGroupBy (Timing.end, groupBy);
	}
	
	protected abstract void onQuery 		(Timing timing, Query query) 					throws DatabaseException;

	protected abstract void onSelect 		(Timing timing, Select select) 					throws DatabaseException;
	protected abstract void onSelectField 	(String field, int count, int index) 			throws DatabaseException;
	
	protected abstract void onFilter 		(Timing timing, Filter filter, boolean isWhere) throws DatabaseException;
	protected abstract void onCondition 	(Condition condition, Filter filter, int index) throws DatabaseException;

	protected abstract void onOrderBy 		(Timing timing, OrderBy orderBy) 				throws DatabaseException;
	protected abstract void onOrderByField 	(OrderByField orderBy, int count, int index) 	throws DatabaseException;
	
	protected abstract void onGroupBy 		(Timing timing, GroupBy groupBy) 				throws DatabaseException;
	protected abstract void onGroupByField 	(String field, int count, int index) 			throws DatabaseException;

	protected abstract CompiledQuery 		
							done 			() 												throws DatabaseException;

}
