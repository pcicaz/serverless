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

// native imports 
var System 			= native ('java.lang.System');
var File 			= native ('java.io.File');
var Pattern 		= native ('java.util.regex.Pattern');
var Api 			= native ('com.bluenimble.platform.api.Api');
var Lang 			= native ('com.bluenimble.platform.Lang');
var FileUtils 		= native ('com.bluenimble.platform.FileUtils');
var Json 			= native ('com.bluenimble.platform.Json');
var JsonObject 		= native ('com.bluenimble.platform.json.JsonObject');
var JsonArray 		= native ('com.bluenimble.platform.json.JsonArray');

var BuildUtils 		= native ('com.bluenimble.platform.icli.mgm.utils.BuildUtils');
var SpecUtils 		= native ('com.bluenimble.platform.icli.mgm.utils.SpecUtils');
								  
function extract (service, file, script) {
	var markers = FileUtils.readStartsWith (script, '//@', true);
	if (markers.isEmpty ()) {
		return;
	}
	
	Tool.note ('\t Found ' + markers.size () + ' markers in ' + script.getName ());
	
	for (var i = 0; i < markers.size (); i++) {		
		// read marker and add to service spec
		var marker = markers.get (i);
		
		var line 	= marker.substring (0, marker.indexOf (' '));
		
		marker = marker.substring (marker.indexOf (' ') + 4).trim ();
		
		if (!service.markers) {
			service.markers = new JsonArray ();
		}
		
		var indexOfLt = marker.indexOf ('<');
		var indexOfGt = marker.indexOf ('>');
		
		if (indexOfLt == 0 && indexOfGt > 1) {
			var oMarker = new JsonObject ();
			oMarker.line = line;
			service.markers.add (oMarker);
			
			oMarker.type = marker.substring (indexOfLt + 1, indexOfGt).toLowerCase ();
			var line = marker.substring (indexOfGt + 1).trim ();
			if (line == '') {
				continue;
			}
			var properties = Lang.split (line, '|', true);
			for (var j = 0; j < properties.length; j++) {
				var property = properties [j];
				var pv = Lang.split (property, ' ', true);
				oMarker [ pv [0] ] = (pv.length > 1 ? pv [1] : true); 
			}
		} else {
			if (service.markers.isEmpty ()) {
				continue;
			}
			marker = marker.trim ();
			if (marker.endsWith ('/')) {
				marker = marker.substring (0, marker.length () - 1);
			} else {
				marker += '\n';
			}
			var lastMarker = service.markers.get (service.markers.size () - 1);
			if (lastMarker.comment) {
				lastMarker.comment += marker;
			} else {
				lastMarker.comment = marker;
			}
		}
	}
		
	if (service.markers) {
		Json.store (service, file);
	}

}

function validate (apiFolder, folderOrFile) {

	if (folderOrFile.getName ().startsWith ('.')) {
		return;
	}

	if (folderOrFile.isDirectory ()) {
		var files = folderOrFile.listFiles ();
		for (var i = 0; i < files.length; i++) {
			validate (apiFolder, files [i]);
		}
		return;
	}
	// load service spec, find script and extract issues
	var serviceName = folderOrFile.getName ();
	serviceName = serviceName.substring (0, serviceName.lastIndexOf ('.'));
	Tool.note ('\t Validating service spec ' + ' ' + serviceName);
	var service;
	try {
		service = Json.load (folderOrFile);
	} catch (ex) {
		Tool.error ('File ' + folderOrFile.getAbsolutePath () + ' -> ' + ex.message);
	}
	if (!service) {
		return;
	}
	
	// parse script
	if (!service.runtime || !service.runtime.script) {
		return;
	}
	
	var script = new File (apiFolder, 'resources/' + service.runtime.script);
	if (!script.exists ()) {
		Tool.error ('Service spec using script file ' + script.getAbsolutePath () + ' which is not found');
		return;
	}
	
	extract (service, folderOrFile, script);
	
	return;
	
}

// Start Of Script
var startTime = System.currentTimeMillis ();

if (typeof Keys === 'undefined') {
	throw 'Security Keys not found. Load some keys in order to push your api';
}

// check if valid command args
if (typeof Command === 'undefined') {
	throw 'missing command arguments. eg. push [ApiNs required] [Version optional]';
}

var tokens = Lang.split (Command, ' ', true);

if (tokens.length < 2) {
	throw 'missing command arguments. eg. push api ApiNs OR push api ApiNs v1';
}

if (tokens [0] != 'api') {
	throw 'wrong push target object. eg. push api ApiNs';
}

// api namespace
var apiNs = tokens [1];

// Create the build folder if it doesn't exist
var buildFolder = new File (Home, 'build');
if (!buildFolder.exists ()) {
	buildFolder.mkdir ();
}

var apiFolder = new File (buildFolder, apiNs);

// ONLY If Requested
if (!Vars ['build.release.nocopy'] || Vars ['build.release.nocopy'] != 'true') {
	// delete api if exists
	FileUtils.delete (apiFolder);
	
	// copy
	var apiSrcPath = Config.apis [apiNs];
	if (!apiSrcPath) {
		apiSrcPath = apiNs;
	}
	
	var apiSrc = new File (Config.workspace + '/' + apiSrcPath);
	// mavenized project
	if (new File (apiSrc, "pom.xml").exists ()) {
		// run maven
		var apiMvnBuild = new File (apiSrc, "build");
		if (!apiMvnBuild.exists ()) {
			throw apiMvnBuild.getAbsolutePath () + ' not found';
		}
		
		Tool.note ('Building api - (mvn clean install) ' + apiNs);

		// call maven
		BuildUtils.mvn (Tool.proxy (), apiSrc, "clean install");
		
		var apiMvnBuildFiles = apiMvnBuild.listFiles ();
		if (apiMvnBuildFiles == null || apiMvnBuildFiles.legth == 0) {
			throw 'api build folder not found. run maven first. mvn clean install';
		}
		if (apiMvnBuildFiles.legth > 1) {
			throw 'there is more than 1 api in the build folder';
		}
		apiSrc = apiMvnBuildFiles [0];
	}
	
	FileUtils.copy (apiSrc, buildFolder, true);
	
	apiFolder = new File (buildFolder, apiSrc.getName ());
	
	// assemblies if any
	var assemblies = new File (apiFolder, "assemblies"); 
	if (assemblies.exists ()) {
		FileUtils.delete (assemblies);
	}
	
	// convert yaml to json
	SpecUtils.y2j (apiFolder, true);
}

Tool.note ('Validating api ' + apiNs);

// read api spec and set installDate, version, user id and stamp
var apiSpecFile = new File (apiFolder, 'api.json');
if (!apiSpecFile.exists ()) {
	throw 'api ' + apiNs + ' not found';
}

var apiSpec = Json.load (apiSpecFile);

var release = new JsonObject ();
apiSpec.set (Api.Spec.Release, release);

// set install date
release.set ("date", Lang.utc ());

// set version (this will change the api namespace) from apiNs to apiNs-version. eg travel-v1
if (tokens.length > 2 && tokens [2]) {
	release.set ('version', tokens [2]);
	apiNs += ('-' + tokens [2].toLowerCase ());
	apiSpec.set (Api.Spec.Name, apiSpec.get (Api.Spec.Name) + " ( " + tokens [2] + " )");
}

// set user stamp (this will change the api namespace) from apiNs to apiNs-version-stamp. eg travel-v1-john
if (Keys && Keys.user && Keys.user.stamp && Vars ['api.release.stamp'] == 'true') {
	apiNs += ('-' + Keys.user.stamp.toLowerCase ());
}

apiNs = apiNs.toLowerCase ();

if (!Pattern.matches ("^[a-zA-Z0-9_-]*$", apiNs)) {
	throw 'Invalid api namespace ' + apiNs + '. It should contains only letters, numbers, underscore ( _ ) and dash ( - )';
}

apiSpec.set (Api.Spec.Namespace, apiNs);

// set pusher (user id, email) this is part of the keys
if (Keys && Keys.user && Keys.user.id) {
	release.set ('pushedBy', Keys.user.id);
}

// set release notes
if (Vars ['api.release.notes']) {
	release.set ('notes', Vars ['api.release.notes']);
}

//generate datasources 
BuildUtils.generate (apiFolder, Json.find (apiSpec, 'runtime', 'datasources'));

// save api.json file
Json.store (apiSpec, apiSpecFile);

// read, validate, parse code
validate (apiFolder, new File (apiFolder, 'resources/services'));

var newApiFolder = new File (buildFolder, apiNs);

apiFolder.renameTo (newApiFolder);

Tool.info ('Push Api: ' + apiNs);

Tool.command ('set api.folder ' + buildFolder.getAbsolutePath ());

Tool.command ('npush api ' + apiNs);

Tool.command ('unset api.folder');

FileUtils.delete (newApiFolder);

Tool.info ('Total Push Time: ' + (System.currentTimeMillis () - startTime) + ' millis');
