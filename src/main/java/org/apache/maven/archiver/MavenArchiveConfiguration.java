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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Capture common archive configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: MavenArchiveConfiguration.java 1233956 2012-01-20 15:27:43Z olamy $
 * @todo is this general enough to be in Plexus Archiver?
 */
public class MavenArchiveConfiguration
{
    private boolean compress = true;

    private boolean index;

    private boolean addMavenDescriptor = true;

    private File manifestFile;

    private ManifestConfiguration manifest;

    private Map<String, String> manifestEntries = new HashMap<String, String>();

    private List<ManifestSection> manifestSections = new ArrayList<ManifestSection>();

    /**
     * @since 2.2
     */
    private boolean forced = true;

    /**
     * @since 2.3
     */
    private File pomPropertiesFile;

    public boolean isCompress()
    {
        return compress;
    }

    public boolean isIndex()
    {
        return index;
    }

    public boolean isAddMavenDescriptor()
    {
        return addMavenDescriptor;
    }

    public File getManifestFile()
    {
        return manifestFile;
    }

    public ManifestConfiguration getManifest()
    {
        if ( manifest == null )
        {
            manifest = new ManifestConfiguration();
        }
        return manifest;
    }

    public void setCompress( boolean compress )
    {
        this.compress = compress;
    }

    public void setIndex( boolean index )
    {
        this.index = index;
    }

    public void setAddMavenDescriptor( boolean addMavenDescriptor )
    {
        this.addMavenDescriptor = addMavenDescriptor;
    }

    public void setManifestFile( File manifestFile )
    {
        this.manifestFile = manifestFile;
    }

    public void setManifest( ManifestConfiguration manifest )
    {
        this.manifest = manifest;
    }

    public void addManifestEntry( String key, String value )
    {
        manifestEntries.put( key, value );
    }

    public void addManifestEntries( Map<String,String> map )
    {
        manifestEntries.putAll( map );
    }

    public boolean isManifestEntriesEmpty()
    {
        return manifestEntries.isEmpty();
    }

    public Map<String,String> getManifestEntries()
    {
        return manifestEntries;
    }

    public void setManifestEntries( Map manifestEntries )
    {
        this.manifestEntries = manifestEntries;
    }

    public void addManifestSection( ManifestSection section )
    {
        manifestSections.add( section );
    }

    public void addManifestSections( List<ManifestSection> list )
    {
        manifestSections.addAll( list );
    }

    public boolean isManifestSectionsEmpty()
    {
        return manifestSections.isEmpty();
    }

    public List<ManifestSection> getManifestSections()
    {
        return manifestSections;
    }

    public void setManifestSections( List<ManifestSection> manifestSections )
    {
        this.manifestSections = manifestSections;
    }

    /**
     * <p>Returns, whether recreating the archive is forced (default). Setting
     * this option to false means, that the archiver should compare the
     * timestamps of included files with the timestamp of the target archive
     * and rebuild the archive only, if the latter timestamp precedes the
     * former timestamps. Checking for timestamps will typically offer a
     * performance gain (in particular, if the following steps in a build
     * can be suppressed, if an archive isn't recrated) on the cost that
     * you get inaccurate results from time to time. In particular, removal
     * of source files won't be detected.</p>
     * <p>An archiver doesn't necessarily support checks for uptodate. If
     * so, setting this option to true will simply be ignored.</p>
     *
     * @return True, if the target archive should always be created; false
     *         otherwise
     * @see #setForced(boolean)
     */
    public boolean isForced()
    {
        return forced;
    }

    /**
     * <p>Sets, whether recreating the archive is forced (default). Setting
     * this option to false means, that the archiver should compare the
     * timestamps of included files with the timestamp of the target archive
     * and rebuild the archive only, if the latter timestamp precedes the
     * former timestamps. Checking for timestamps will typically offer a
     * performance gain (in particular, if the following steps in a build
     * can be suppressed, if an archive isn't recrated) on the cost that
     * you get inaccurate results from time to time. In particular, removal
     * of source files won't be detected.</p>
     * <p>An archiver doesn't necessarily support checks for uptodate. If
     * so, setting this option to true will simply be ignored.</p>
     *
     * @param forced True, if the target archive should always be created; false
     *               otherwise
     * @see #isForced()
     */
    public void setForced( boolean forced )
    {
        this.forced = forced;
    }

    /**
     * Returns the location of the "pom.properties" file.
     * May be null, in which case a default value is choosen.
     *
     * @return "pom.properties" location or null.
     */
    public File getPomPropertiesFile()
    {
        return pomPropertiesFile;
    }

    /**
     * Sets the location of the "pom.properties" file.
     * May be null, in which case a default value is choosen.
     *
     * @param pomPropertiesFile "pom.properties" location or null.
     */
    public void setPomPropertiesFile( File pomPropertiesFile )
    {
        this.pomPropertiesFile = pomPropertiesFile;
    }
}
