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
package com.bluenimble.platform.api.security.impls;

import java.util.Date;

import com.bluenimble.platform.Crypto;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiManagementException;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.security.ApiAuthenticationException;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.security.ApiConsumerResolver;
import com.bluenimble.platform.api.security.ApiConsumerResolverAnnotation;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.security.impls.DefaultApiConsumer;

@ApiConsumerResolverAnnotation (name = TokenConsumerResolver.MethodName)
public class TokenConsumerResolver implements ApiConsumerResolver {

	private static final long serialVersionUID = 889277317993642120L;
	
	protected static final String MethodName = "token";

	interface Defaults {
		String 	Scheme 			= "Token";
	}
	
	interface Spec {
		String 	Scheme 			= "scheme";
		interface Auth {
			String 	Secrets 	= "secrets";
		}
	}

	public TokenConsumerResolver () {
	}
	
	@Override
	public ApiConsumer resolve (Api api, ApiService service, ApiRequest request)
			throws ApiAuthenticationException {
		
		JsonObject oResolver = Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Schemes), MethodName);
		
		String 	scheme 	= Json.getString 	(oResolver, Spec.Scheme, Defaults.Scheme);
		
		String placeholder = Json.getString (service.getSecurity (), ApiService.Spec.Security.Placeholder, Scope.Header.name ());
		
		String authHeader 	= (String)request.get (ApiHeaders.Authorization, Scope.valueOf (placeholder));
		
		if (Lang.isNullOrEmpty (authHeader)) {
			return null;
		}
		
		String [] pair = Lang.split (authHeader, Lang.SPACE, true);
		if (pair.length < 2) {
			return null;
		}
		
		String app 		= pair [0];
		String token 	= pair [1];

		if (!app.equalsIgnoreCase (scheme)) {
			return null;
		}
		
		ApiConsumer consumer = new DefaultApiConsumer (ApiConsumer.Type.Token);
		consumer.set (ApiConsumer.Fields.Token, token);
		return consumer;
	}
	
	@Override
	public ApiConsumer authorize (Api api, ApiService service, ApiRequest request, ApiConsumer consumer)
			throws ApiAuthenticationException {
		
		JsonObject auth = Json.getObject (Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Schemes), MethodName), Api.Spec.Security.Auth);
		if (auth == null || auth.isEmpty ()) {
			return consumer;
		}
		
		String token = (String)consumer.get (ApiConsumer.Fields.Token);
		
		// decrypt token
		String decrypted = null;
		
		JsonObject secrets;
		try {
			secrets = api.space ().getSecrets (Json.getString (auth, Spec.Auth.Secrets));
		} catch (ApiManagementException e) {
			throw new ApiAuthenticationException (e.getMessage (), e);
		}
		if (secrets != null && secrets.containsKey (ApiSpace.Spec.secrets.Key)) {
			String key = Json.getString (secrets, ApiSpace.Spec.secrets.Key);
			
			Crypto.Algorithm alg = Crypto.Algorithm.AES;
					
			try {
				alg = Crypto.Algorithm.valueOf (Json.getString (secrets, ApiSpace.Spec.secrets.Algorithm, Crypto.Algorithm.AES.name ()).toUpperCase ());
			} catch (Exception ex) {
				api.tracer ().log (Tracer.Level.Error, Lang.BLANK, ex);
				// IGNORE - > invalid token
			}
			try {
				decrypted = new String (Crypto.decrypt (Lang.decodeHex (token.toCharArray ()), key, alg));
			} catch (Exception ex) {
				api.tracer ().log (Tracer.Level.Error, Lang.BLANK, ex);
				// IGNORE - > invalid token
			}
		}
		
		boolean isServiceSecure = Json.getBoolean (service.getSecurity (), ApiService.Spec.Security.Enabled, true);

		if (decrypted == null) {
			if (isServiceSecure) {
				throw new ApiAuthenticationException ("invalid token");
			} else {
				return consumer;
			}
		}
		
		int indexOfSpace = decrypted.indexOf (Lang.SPACE);
		if (indexOfSpace < 0) {
			if (isServiceSecure) {
				throw new ApiAuthenticationException ("invalid token");
			} else {
				return consumer;
			}
		}
		
		String sExpiry 	= decrypted.substring (0, indexOfSpace);
		
		long expiry = Long.valueOf (sExpiry);
		if (expiry < System.currentTimeMillis ()) {
			if (isServiceSecure) {
				throw new ApiAuthenticationException ("token expired");
			} 
		}
		consumer.set (ApiConsumer.Fields.ExpiryDate, Lang.toUTC (new Date (expiry)));
		
		String sInfo 	= decrypted.substring (indexOfSpace + 1);
		
		JsonArray fields = Json.getArray (api.getSecurity (), Api.Spec.Security.Encrypt);
		if (fields == null || fields.isEmpty ()) {
			consumer.set (ApiConsumer.Fields.Id, sInfo);
		} else {
			String [] values = Lang.split (sInfo, Lang.SEMICOLON);
			for (int i = 0; i < fields.count (); i++) {
				if (i >= values.length) {
					break;
				}
				consumer.set ((String)fields.get (i), values [i]);
			}
		}
		
		consumer.set (ApiConsumer.Fields.Permissions, secrets.get (ApiConsumer.Fields.Permissions));

		consumer.set (ApiConsumer.Fields.Anonymous, false);

		return consumer;
	}
	
}
