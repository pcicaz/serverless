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
package com.bluenimble.platform.icli.mgm.commands.dev;

import com.bluenimble.platform.cli.command.impls.PrefixedCommand;
import com.bluenimble.platform.icli.mgm.commands.dev.impls.LoadFeatureHandler;
import com.bluenimble.platform.icli.mgm.commands.dev.impls.LoadKeysHandler;

public class LoadCommand extends PrefixedCommand {

	private static final long serialVersionUID = 8809252448144097989L;
	
	interface Subject {
		String Keys 		= "keys";
		String Feature 		= "feature";
	}
	
	public LoadCommand () {
		super ("load", "load keys or load feature. Ex. load keys a-bluenimble-instance.keys [keys name (optional)]");
		addHandler (Subject.Keys, new LoadKeysHandler ());
		addHandler (Subject.Feature, new LoadFeatureHandler ());
	}
}
