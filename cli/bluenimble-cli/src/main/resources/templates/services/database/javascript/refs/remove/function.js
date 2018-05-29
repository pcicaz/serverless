return {
	
	/**
	 * The only required function that you should implement, if no mock data provided in your Remove[[Model]][[Ref]].json
	 * 
	 * The execute function will be triggered when an application or device makes a call to post [bluenimble-space].[bluenimble-instance].bluenimble.com/[[api]]/[[models]]
	 * which is defined in your service specification file Remove[[Model]][[Ref]].json 
	 * 
	 * Arguments:
	 *  Api 		 the api where this service is running  
	 *  ApiConsumer  the user, application, or device calling this service. It could be a ApiConsumer.Type.Token, ApiConsumer.Type.Cookie, ApiConsumer.Type.Signature, 
	 *				 ApiConsumer.Type.Basic or ApiConsumer.Type.Unknown
	 *  ApiRequest 	 the parameters, headers and streams. 
	 *               ex. request.get ('email') to get a parameter called 'email'
	 *               request.get ('Token', ApiRequest.Scope.Header) to get an http or CoAp header called 'Token'
	 *               request.get ('myfile', ApiRequest.Scope.Stream) to get a stream called 'myfile' (uploads) 
	 *  ApiResponse  to set headers to send back to the calling device or application 
	 * 				 response.set ('X-MyHeader', 'Hello');
	 *				 you can also write data to the response but this is rarely will happen as the platform takes care of this.	
	 *
	 *
	 *	@author		[[user]]
	 *	@created	[[date]]
	 * 
	 **/
	execute: function (api, consumer, request, response) {
		
		var [[model]]Id = request.get ('[[model]]');
		var [[ref]]Id 	= request.get ('[[ref]]');
		
		var db = api.database (request);

		// lookup [[Model]] by :[[model]]
		var [[model]] = db.get ('[[Models]]', [[model]]Id);
		if (![[model]]) {
			throw new ApiServiceExecutionException ("[[model]] '" + [[model]]Id + "' not found").status (ApiResponse.NOT_FOUND);
		}
		
		// lookup [[Ref]] by :[[ref]]
		var [[ref]] = db.get ('[[RefSpec.entity]]', [[ref]]Id);
		if (![[ref]]) {
			throw new ApiServiceExecutionException ("[[ref]] '" + [[ref]]Id + "' not found").status (ApiResponse.NOT_FOUND);
		}
		
		// add link
		return db.create ('[[Model]][[Refs]]', {
			[[model]]: [[model]]Id,
			[[ref]]: [[ref]]Id
		}).save ().toJson (0, 0);
		
	}

}