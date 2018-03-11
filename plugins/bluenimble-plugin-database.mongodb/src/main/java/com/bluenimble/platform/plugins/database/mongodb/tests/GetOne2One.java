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
package com.bluenimble.platform.plugins.database.mongodb.tests;

import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.impls.DefaultDatabaseObjectSerializer;

public class GetOne2One {
	
	public static void main (String [] args) throws DatabaseException {
		
		Database db = new DatabaseServer ().get ();
		
		DatabaseObject driver = db.get ("Drivers", "5aa425145e67264b7858685a");
		
		DatabaseObject car = (DatabaseObject)driver.get ("car");
		
		System.out.println (car.toJson (new DefaultDatabaseObjectSerializer (2, 2)));
		
	}
	
}
