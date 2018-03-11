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
package com.bluenimble.platform.plugins.database.mongodb.impls;

import static com.mongodb.client.model.Filters.eq;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.query.CompiledQuery;
import com.bluenimble.platform.db.query.Query;
import com.bluenimble.platform.db.query.Query.Operator;
import com.bluenimble.platform.db.query.QueryCompiler;
import com.bluenimble.platform.db.query.Select;
import com.bluenimble.platform.db.query.impls.SqlQueryCompiler;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.DeleteResult;

public class MongoDatabaseImpl implements Database {

	private static final long serialVersionUID = 3547537996525908902L;
	
	private static final String 	DateFormat 			= "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	
	public static final String		CacheQueriesBucket	= "__plugin/database/odb/QueriesBucket__";
	private static final String 	Lucene 				= "LUCENE";
	
	private interface Tokens {
		String Type 		= "{Type}";
		String Field 		= "{field}";
		String Parent 		= "{parent}";
		String Child 		= "{child}";
		String Collection 	= "{collection}";
		String Value 		= "{value}";
	}
	
	private interface Sql {
		String Skip			= "skip";
		String Limit		= "limit";
		String ReturnBefore	= "return before";
	}
	
    private static final Set<String> SystemEntities = new HashSet<String> ();
	static {
		SystemEntities.add ("OSchedule");
	}
	
	private static final String CollectionAddQuery 		= "UPDATE " + Tokens.Parent + " ADD " + Tokens.Collection + " = " + Tokens.Child;
	private static final String CollectionRemoveQuery 	= "UPDATE " + Tokens.Parent + " REMOVE " + Tokens.Collection + " = " + Tokens.Child;
	
	interface Describe {
		String Size 		= "size";
		String Entities 	= "entities";
			String Name 	= "name";
			String Count 	= "count";
	}
	
	interface SpiDescribe {
		String Size 		= "size";
		String CollStats 	= "collStats";
	}
	
	private Map<String, String> QueriesCache = new ConcurrentHashMap<String, String> ();
	
	private MongoDatabase		db;
	private Tracer				tracer;
	
	public MongoDatabaseImpl (MongoDatabase db, Tracer tracer) {
		this.db 		= db;
		this.tracer 	= tracer;
	}

	@Override
	public DatabaseObject create (String name) throws DatabaseException {
		return new DatabaseObjectImpl (this, name);
	}

	@Override
	public void trx () {
		// 
	}
	
	@Override
	public void commit () throws DatabaseException {
		
	}

	@Override
	public void rollback () throws DatabaseException {
		
	}

	@Override
	public void createEntity (String eType, Field... fields) throws DatabaseException {
		eType = checkNotNull (eType);
		
		
	}

	@Override
	public void createIndex (String eType, IndexType type, String name,
			Field... fields) throws DatabaseException {
		
		eType = checkNotNull (eType);
		
		if (fields == null || fields.length == 0) {
			throw new DatabaseException ("entity " + eType + ". fields required to create an index");
		}
		
		
	}

	@Override
	public void dropIndex (String eType, String name) throws DatabaseException {

		eType = checkNotNull (eType);
		
		
	}

	@Override
	public int increment (DatabaseObject object, String field, int value) throws DatabaseException {
		return 0;
	}

	@Override
	public DatabaseObject get (String eType, Object id) throws DatabaseException {
		Document document = _get (eType, id);
	    if (document == null) {
		    return null;
	    }
		
		return new DatabaseObjectImpl (this, eType, document);

	}
	
	Document _get (String eType, Object id) {
		if (id == null) {
			return null;
		}
		
		MongoCollection<Document> collection = db.getCollection (eType);
		if (collection == null) {
			return null;
		}
		
		Object _id = id;
		if (ObjectId.isValid (String.valueOf (id))) {
			_id = new ObjectId (String.valueOf (id));
		}
		
	    return collection.find (eq (DatabaseObjectImpl.ObjectIdKey, _id)).first ();
	}
	
	@Override
	public List<DatabaseObject> find (String name, Query query, Visitor visitor) throws DatabaseException {
		return null;
	}

	@Override
	public DatabaseObject findOne (String type, Query query) throws DatabaseException {
		
		// force count to 1
		query.count (1);
		
		List<DatabaseObject> result = find (type, query, null);
		if (result == null || result.isEmpty ()) {
			return null;
		}
		
		return result.get (0);
	}

	@Override
	public int delete (String eType, Object id) throws DatabaseException {
		
		checkNotNull (eType);
		
		if (id == null) {
			throw new DatabaseException ("can't delete object (missing object id)");
		}
		
		MongoCollection<Document> collection = db.getCollection (eType);
		if (collection == null) {
			return 0;
		}
		
		DeleteResult result = collection.deleteOne (eq (DatabaseObjectImpl.ObjectIdKey, new ObjectId (String.valueOf (id))));
		
		return (int)result.getDeletedCount ();
		
	}

	@Override
	public void drop (String eType) throws DatabaseException {
		
		eType = checkNotNull (eType);
		
		MongoCollection<Document> collection = db.getCollection (eType);
		if (collection == null) {
			return;
		}
		
		collection.drop ();
	}

	@Override
	public long count (String eType) throws DatabaseException {
		
		eType = checkNotNull (eType);
		
		MongoCollection<Document> collection = db.getCollection (eType);
		if (collection == null) {
			return 0;
		}
		
		return collection.count ();
	}

	@Override
	public int delete (Query query) throws DatabaseException {
		if (query == null) {
			return 0;
		}
		Object result = _query (null, Query.Construct.delete, query, false);
		if (result == null) {
			return 0;
		}
		return (Integer)result;
	}

	@Override
	public void recycle () {
	}
	
	@Override
	public void add (DatabaseObject parent, String collection, DatabaseObject child)
			throws DatabaseException {
		addRemove (CollectionAddQuery, parent, collection, child);
	}

	@Override
	public void remove (DatabaseObject parent, String collection, DatabaseObject child)
			throws DatabaseException {
		addRemove (CollectionRemoveQuery, parent, collection, child);
	}

	@Override
	public JsonObject describe () {
		
		JsonObject describe = new JsonObject ();
		
		describe.set (Describe.Size, 0);
		
		MongoIterable<String> collections = db.listCollectionNames ();
		
		if (collections == null) {
			return describe;
		}
		
		long size = 0;
		
		JsonArray aEntities = new JsonArray ();
		describe.set (Describe.Entities, aEntities);
		
		for (String collection : collections) {
			if (SystemEntities.contains (collection)) {
				continue;
			}
			JsonObject oEntity = new JsonObject ();
			oEntity.set (Describe.Name, collection);
			oEntity.putAll (db.runCommand (new Document (SpiDescribe.CollStats, collection)));
			aEntities.add (oEntity);
			
			size += Json.getLong (oEntity, SpiDescribe.CollStats, 0);
			
		}
		
		describe.set (Describe.Size, size);
		
		return describe;
	}
	
	private void addRemove (String queryTpl, DatabaseObject parent, String collection, DatabaseObject child)
			throws DatabaseException {
		
		if (parent == null || child == null) {
			return;
		}
		
		Document parentDoc = ((DatabaseObjectImpl)parent).document;
		Document childDoc 	= ((DatabaseObjectImpl)child).document;
		
		if (!((DatabaseObjectImpl)parent).persistent) {
			throw new DatabaseException ("Parent Object " + parent.entity () + " is not a persistent object");
		}
		
		if (!((DatabaseObjectImpl)child).persistent) {
			throw new DatabaseException ("Child Object " + child.entity () + " is not a persistent object");
		}
		
		// TODO
	}

	@Override
	public boolean isEntity (Object value) throws DatabaseException {
		if (value == null) {
			return false;
		}
		return DatabaseObject.class.isAssignableFrom (value.getClass ());
	}

	@Override
	public JsonObject bulk (JsonObject data) throws DatabaseException {
		
		JsonObject result = (JsonObject)new JsonObject ().set (Database.Fields.Total, 0);
		
		if (data == null || data.isEmpty ()) {
			return result;
		}
			
		// TODO
		
		return result;
		
	}

	@Override
	public DatabaseObject popOne (String name, Query query) throws DatabaseException {
		
		// force count to 1
		query.count (1);
		
		List<DatabaseObject> result = (List<DatabaseObject>)pop (name, query, null);
		if (result == null || result.isEmpty ()) {
			return null;
		}
		
		return result.get (0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DatabaseObject> pop (String name, Query query, Visitor visitor) throws DatabaseException {
		if (query == null) {
			return null;
		}
		
		// TODO
		
		return null;
	}

	@Override
	public void schedule (String event, Query query, String cron) throws DatabaseException {
		if (query == null || Lang.isNullOrEmpty (query.entity ()) || query.construct () == null) {
			throw new DatabaseException ("query to schedule should provide the entity name and the construct [insert, update or delete]");
		}
		
		// TODO
		
	}

	@Override
	public void unschedule (String event) throws DatabaseException {
		
	}
	
	@Override
	public void exp (Set<String> entities, OutputStream out, Map<ExchangeOption, Boolean> options, final ExchangeListener listener) throws DatabaseException {
		
		// TODO
        
	}

	@Override
	public void imp (Set<String> entities, InputStream in, Map<ExchangeOption, Boolean> options, final ExchangeListener listener) throws DatabaseException {
		// TODO
	}
	
	private List<DatabaseObject> toList (String type, List<Document> documents, Visitor visitor) {
		
		return null;
	}

	private Object _query (String type, Query.Construct construct, final Query query, boolean returnBefore) throws DatabaseException {
		
		if (query == null) {
			return null;
		}
		
		return null;
	}
	
	private CompiledQuery compile (String entity, Query.Construct construct, final Query query, final boolean returnBefore) throws DatabaseException {
		final String fEntity = entity;
		QueryCompiler compiler = new SqlQueryCompiler (construct) {
			private static final long serialVersionUID = -1248971549807669897L;
			
			@Override
			protected void onQuery (Timing timing, Query query)
					throws DatabaseException {
				super.onQuery (timing, query);
				
				if (Timing.start.equals (timing)) {
					return;
				}
				
				if (query.start () > 0) {
					buff.append (Lang.SPACE).append (Sql.Skip).append (Lang.SPACE).append (query.start ());
				}
				if (query.count () > 0) {
					buff.append (Lang.SPACE).append (Sql.Limit).append (Lang.SPACE).append (query.count ());
				}
			}
			
			@Override
			protected void onSelect (Timing timing, Select select) throws DatabaseException {
				super.onSelect (timing, select);
				if (Timing.end.equals (timing) && returnBefore) {
					buff.append (Lang.SPACE).append (Sql.ReturnBefore);
				}
			}
			
			@Override
			protected String operatorFor (Operator operator) {
				if (Operator.ftq.equals (operator)) {
					return Lucene;
				}
				return super.operatorFor (operator);
			}
			
			@Override
			protected void entity () {
				buff.append (fEntity);
			}
		}; 
		
		return compiler.compile (query);
		
	}

	private String format (String query, String type) {
		return Lang.replace (query, Tokens.Type, type);
	}
	
	private String format (String query, String type, String field) {
		return Lang.replace (format (query, type), Tokens.Field, field);
	}
	
	private String format (String query, String parent, String collection, String child) {
		return Lang.replace (Lang.replace (Lang.replace (query, Tokens.Collection, collection), Tokens.Parent, parent), Tokens.Child, child);
	}
	
	private String checkNotNull (String eType) throws DatabaseException {
		if (Lang.isNullOrEmpty (eType)) {
			throw new DatabaseException ("entity name is null");
		}
		return eType;
	}
	
	private boolean option (Map<ExchangeOption, Boolean> options, ExchangeOption option, boolean defaultValue) {
		if (options == null) {
			return defaultValue;
		}
		if (!options.containsKey (option)) {
			return defaultValue;
		}
		return options.get (option);
	}

	@Override
	public Object get () {
		return null;
	}

	@Override
	public void set (ApiSpace space, ClassLoader classLoader, Object... args) {
		
	}

	public MongoDatabase getInternal () {
		return db;
	}

}
