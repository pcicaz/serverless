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
package com.bluenimble.platform.indexer.impls;

import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.http.HttpHeaders;
import com.bluenimble.platform.http.utils.ContentTypes;
import com.bluenimble.platform.indexer.Indexer;
import com.bluenimble.platform.indexer.IndexerException;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.remote.Serializer;

public class ElasticSearchIndexer implements Indexer {
		
	private interface Internal {
		String 	Id 			= "id";
		String 	Timestamp 	= "timestamp";
		
		interface Elk {
			String Update 	= "_update";
			String Search 	= "_search";
			String Doc 		= "doc";
			String Source 	= "source";
			String Mapping 	= "_mapping";
			String Query 	= "query";
			String MatchAll = "match_all";
			String DeleteQ 	= "_delete_by_query?conflicts=proceed";
		}
	}
	
	private 			String url;
	private transient 	String authToken;
	private 			Remote remote;
	private 			Tracer tracer;

	public ElasticSearchIndexer (Remote remote, String url, String authToken, Tracer tracer) {
		this.remote 	= remote;
		this.url 		= url;
		this.authToken 	= authToken;
	}
	
	@Override
	public boolean exists (String entity) throws IndexerException {
		if (Lang.isNullOrEmpty (entity)) {
			throw new IndexerException ("Entity cannot be null nor empty.");
		}
		
		tracer.log (Tracer.Level.Info, "Checking if entity [{0}] exists.", entity);
		
		Error error = new Error ();
		
		remote.head (
			(JsonObject)new JsonObject ().set (Remote.Spec.Endpoint, url + Internal.Elk.Mapping + Lang.SLASH + entity)
				.set (Remote.Spec.Headers, new JsonObject ()
					.set (HttpHeaders.AUTHORIZATION, authToken)
				),
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onDone (int code, Object message) {
				}
			}
		);
		
		if (!error.happened ()) {
			return true;
		}
		
		if (error.code == 404) {
			return false;
		}
		
		throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
	}
	
	/*
	
	@Override
	public JsonObject init () throws IndexerException {
		JsonObject result 	= new JsonObject ();
		Error error 		= new Error ();
		
		JsonObject oEntity 	= (JsonObject)new JsonObject ()
			.set (Remote.Spec.Endpoint, url)
			.set (Remote.Spec.Headers, 
				new JsonObject ()
					.set (HttpHeaders.AUTHORIZATION, authToken)
					.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
			).set (Remote.Spec.Serializer, Serializer.Name.json);
		
		remote.put (
			oEntity, 
			new Remote.Callback () {
				@Override
				public void onData (int code, byte [] data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	*/
	
	@Override
	public JsonObject create (String entity, JsonObject definition) throws IndexerException {
		if (Lang.isNullOrEmpty (entity)) {
			throw new IndexerException ("Entity cannot be null nor empty.");
		}
		
		tracer.log (Tracer.Level.Info, "Creating entity [{0}] with definition {1}", entity, definition);
		
		JsonObject result = new JsonObject ();
		Error error = new Error ();
		
		JsonObject oEntity = (JsonObject)new JsonObject ()
			.set (Remote.Spec.Endpoint, url + entity + Lang.SLASH + Internal.Elk.Mapping)
			.set (Remote.Spec.Headers, 
				new JsonObject ()
					.set (HttpHeaders.AUTHORIZATION, authToken)
					.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
			).set (Remote.Spec.Serializer, Serializer.Name.json);
		
		if (!Json.isNullOrEmpty (definition)) {
			oEntity.set (Remote.Spec.Data, definition);
		}
		
		remote.put (
			oEntity, 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	@Override
	public JsonObject clear (String entity) throws IndexerException {
		if (Lang.isNullOrEmpty (entity)) {
			throw new IndexerException ("Entity cannot be null nor empty.");
		}
		
		tracer.log (Tracer.Level.Info, "Deleting all documents in entity [{0}]", entity);
		
		JsonObject result = new JsonObject ();
		Error error = new Error ();
		
		remote.post (
			(JsonObject)new JsonObject ()
				.set (Remote.Spec.Endpoint, url + entity + Lang.SLASH + Internal.Elk.DeleteQ)
				.set (Remote.Spec.Headers, 
					new JsonObject ()
						.set (HttpHeaders.AUTHORIZATION, authToken)
						.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
				).set (Remote.Spec.Data, 
					new JsonObject ()
						.set (Internal.Elk.Query, new JsonObject ()
							.set (Internal.Elk.MatchAll, new JsonObject ())
						)
				).set (Remote.Spec.Serializer, Serializer.Name.json), 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		return null;
	}
	
	@Override
	public JsonObject describe (String entity) throws IndexerException {
		if (Lang.isNullOrEmpty (entity)) {
			throw new IndexerException ("Entity cannot be null nor empty.");
		}
		
		tracer.log (Tracer.Level.Info, "Describe entity [{0}]", entity);
		
		JsonObject result = new JsonObject ();
		Error error = new Error ();
		
		remote.get (
			(JsonObject)new JsonObject ()
			.set (Remote.Spec.Endpoint, url + Internal.Elk.Mapping + Lang.SLASH + entity)
			.set (Remote.Spec.Headers, 
				new JsonObject ()
					.set (HttpHeaders.AUTHORIZATION, authToken)
					.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
			).set (Remote.Spec.Serializer, Serializer.Name.json), 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	@Override
	public JsonObject put (String entity, JsonObject doc) throws IndexerException {
		if (Lang.isNullOrEmpty (entity)) {
			throw new IndexerException ("Entity cannot be null nor empty.");
		}
		
		if (doc == null || doc.isEmpty ()) {
			throw new IndexerException ("Document cannot be null nor empty.");
		}
		
		String id = Json.getString (doc, Internal.Id);
		if (Lang.isNullOrEmpty (id)) {
			doc.set (Internal.Id, Lang.rand ());
		}
		
		if (!doc.containsKey (Internal.Timestamp)) {
			doc.set (Internal.Timestamp, Lang.utc ());
		}
		
		tracer.log (Tracer.Level.Info, "Indexing document [{0}]", id);
		
		JsonObject result = new JsonObject ();
		Error error = new Error ();
		
		remote.put (
			(JsonObject)new JsonObject ()
				.set (Remote.Spec.Endpoint, url + entity + Lang.SLASH + id)
				.set (Remote.Spec.Headers, 
					new JsonObject ()
						.set (HttpHeaders.AUTHORIZATION, authToken)
						.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
				).set (Remote.Spec.Data, doc)
				.set (Remote.Spec.Serializer, Serializer.Name.json), 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	@Override
	public JsonObject get (String entity, String id) throws IndexerException {
		if (Lang.isNullOrEmpty (id)) {
			throw new IndexerException ("Document Id cannot be null.");
		}
		
		tracer.log (Tracer.Level.Info, "Lookup document [{0}]", id);
		
		JsonObject result = new JsonObject ();
		Error error = new Error ();
		
		remote.get (
			(JsonObject)new JsonObject ()
				.set (Remote.Spec.Endpoint, url + entity + Lang.SLASH + id)
				.set (Remote.Spec.Headers, new JsonObject ().set (HttpHeaders.AUTHORIZATION, authToken))
				.set (Remote.Spec.Serializer, Serializer.Name.json), 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	@Override
	public JsonObject update (String entity, JsonObject doc, boolean partial) throws IndexerException {
		if (doc == null || doc.isEmpty ()) {
			throw new IndexerException ("Document cannot be null nor empty.");
		}
		
		String id = Json.getString (doc, Internal.Id); 
		
		if (Lang.isNullOrEmpty (id)) {
			throw new IndexerException ("Document Id cannot be null.");
		}
		
		if (!doc.containsKey (Internal.Timestamp)) {
			doc.set (Internal.Timestamp, Lang.utc ());
		}
		
		if (!partial) {
			tracer.log (Tracer.Level.Info, "It's not partial update. Create document [{0}]", id);
			return create (entity, doc);
		}
		
		tracer.log (Tracer.Level.Info, "Update partially document [{0}]", id);
		
		doc.remove (Internal.Id);
		
		JsonObject result = new JsonObject ();
		Error error = new Error ();
		
		remote.post (
			(JsonObject)new JsonObject ()
				.set (Remote.Spec.Endpoint, url + entity + Lang.SLASH + id + Lang.SLASH + Internal.Elk.Update)
				.set (Remote.Spec.Headers, 
					new JsonObject ()
						.set (HttpHeaders.AUTHORIZATION, authToken)
						.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
				).set (Remote.Spec.Data, new JsonObject ().set (Internal.Elk.Doc, doc))
				.set (Remote.Spec.Serializer, Serializer.Name.json), 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	@Override
	public JsonObject delete (String entity, String id) throws IndexerException {
		if (Lang.isNullOrEmpty (id)) {
			throw new IndexerException ("Document Id cannot be null nor empty.");
		}
		
		JsonObject result = new JsonObject ();
		Error error = new Error ();
		
		tracer.log (Tracer.Level.Info, "Delete document [{0}]", id);
		
		remote.delete (
			(JsonObject)new JsonObject ()
				.set (Remote.Spec.Endpoint, url + entity + Lang.SLASH + id)
				.set (Remote.Spec.Headers, new JsonObject ().set (HttpHeaders.AUTHORIZATION, authToken))
				.set (Remote.Spec.Serializer, Serializer.Name.json), 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}

	@Override
	public JsonObject search (JsonObject query, String [] entities) throws IndexerException {
		String types = Lang.BLANK;
		if (entities == null || entities.length == 0) {
			types = Lang.join (entities, Lang.COMMA);
		}
		
		tracer.log (Tracer.Level.Info, "search documents in [{0}] with query ", types.equals (Lang.BLANK) ? "All Entities" : Lang.join (entities), query);
		
		JsonObject result = new JsonObject ();
		Error error = new Error ();
		
		remote.get (
			(JsonObject)new JsonObject ()
				.set (Remote.Spec.Endpoint, url + types + (types.equals (Lang.BLANK) ? Lang.BLANK : Lang.SLASH) + Internal.Elk.Search)
				.set (Remote.Spec.Headers, 
					new JsonObject ()
						.set (HttpHeaders.AUTHORIZATION, authToken)
						.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
				).set (Remote.Spec.Data, new JsonObject ().set (Internal.Elk.Source, Lang.encode (query.toString ())))
				.set (Remote.Spec.Serializer, Serializer.Name.json), 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	class Error {
		int code;
		Object message;
		void set (int code, Object message) {
			this.code = code;
			this.message = message;
		}
		boolean happened () {
			return code > 0;
		}
	}
}