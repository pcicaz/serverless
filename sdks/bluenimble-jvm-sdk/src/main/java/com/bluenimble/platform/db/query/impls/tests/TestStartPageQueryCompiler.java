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
package com.bluenimble.platform.db.query.impls.tests;

import java.io.File;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.query.CompiledQuery;
import com.bluenimble.platform.db.query.Query;
import com.bluenimble.platform.db.query.QueryCompiler;
import com.bluenimble.platform.db.query.impls.JsonQuery;
import com.bluenimble.platform.db.query.impls.SqlQueryCompiler;

public class TestStartPageQueryCompiler {

	public static void main (String [] args) throws Exception {
		
		Query query = new JsonQuery (Json.load (new File ("tests/queries/complete.json")));
		
		System.out.println ("Select==>");

		QueryCompiler sc = new SqlQueryCompiler (Query.Construct.select) {
			private static final long serialVersionUID = -1248971549807669897L;

			@Override
			protected void onQuery (Timing timing, Query query)
					throws DatabaseException {
				super.onQuery (timing, query);
				if (Timing.start.equals (timing)) {
					return;
				}
				if (query.start () > 0) {
					buff.append (Lang.SPACE).append ("skip").append (Lang.SPACE).append (query.start ());
				}
				if (query.count () > 0) {
					buff.append (Lang.SPACE).append ("limit").append (Lang.SPACE).append (query.count ());
				}
			}
			
		};
		
		CompiledQuery cq = sc.compile (query);
		
		System.out.println ("   query: " + cq.query ());
		System.out.println ();
		System.out.println ("bindings: " + cq.bindings ());
		
		System.out.println ("Delete==>");

		QueryCompiler dc = new SqlQueryCompiler (Query.Construct.delete);
		
		cq = dc.compile (query);
		
		System.out.println ("   query: " + cq.query ());
		System.out.println ();
		System.out.println ("bindings: " + cq.bindings ());
		
	}
	
}
