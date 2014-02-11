package org.apache.maven.archiver;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @version $Id: ManifestSection.java 1233956 2012-01-20 15:27:43Z olamy $
 */
public class ManifestSection 
{

    private String name = null;

    private Map<String,String> manifestEntries = new HashMap<String,String>();

    public void addManifestEntry( String key, String value )
    {
        manifestEntries.put( key, value );
    }

    public Map<String,String> getManifestEntries()
    {
        return manifestEntries;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void addManifestEntries( Map<String,String> map )
    {
        manifestEntries.putAll( map );
    }

    public boolean isManifestEntriesEmpty()
    {
        return manifestEntries.isEmpty();
    }
}
