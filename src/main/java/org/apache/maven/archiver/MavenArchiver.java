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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PrefixedPropertiesValueSource;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Revision: 1235294 $ $Date: 2012-01-24 09:20:06 -0600 (Tue, 24 Jan 2012) $
 */
public class MavenArchiver
{

    public static final String SIMPLE_LAYOUT =
        "${artifact.artifactId}-${artifact.version}${dashClassifier?}.${artifact.extension}";

    public static final String REPOSITORY_LAYOUT = "${artifact.groupIdPath}/${artifact.artifactId}/" +
        "${artifact.baseVersion}/${artifact.artifactId}-" +
        "${artifact.version}${dashClassifier?}.${artifact.extension}";

    public static final String SIMPLE_LAYOUT_NONUNIQUE =
        "${artifact.artifactId}-${artifact.baseVersion}${dashClassifier?}.${artifact.extension}";

    public static final String REPOSITORY_LAYOUT_NONUNIQUE = "${artifact.groupIdPath}/${artifact.artifactId}/" +
        "${artifact.baseVersion}/${artifact.artifactId}-" +
        "${artifact.baseVersion}${dashClassifier?}.${artifact.extension}";

    private static final List<String> ARTIFACT_EXPRESSION_PREFIXES;

    static
    {
        List<String> artifactExpressionPrefixes = new ArrayList<String>();
        artifactExpressionPrefixes.add( "artifact." );

        ARTIFACT_EXPRESSION_PREFIXES = artifactExpressionPrefixes;
    }

    private JarArchiver archiver;

    private File archiveFile;

    /**
     * Return a pre-configured manifest
     *
     * @param project the project
     * @param config the configuration to use
     * @todo Add user attributes list and user groups list
     * @deprecated
     * @return a manifest, clients are recommended to use java.util.jar.Manifest datatype.
     * @throws org.apache.maven.artifact.DependencyResolutionRequiredException .
     * @throws org.codehaus.plexus.archiver.jar.ManifestException .
     */
    @SuppressWarnings( "UnusedDeclaration" )
    public Manifest getManifest( MavenProject project, MavenArchiveConfiguration config )
        throws ManifestException, DependencyResolutionRequiredException
    {
        return getManifest( null, project, config );
    }

    public Manifest getManifest( MavenSession session, MavenProject project, MavenArchiveConfiguration config )
        throws ManifestException, DependencyResolutionRequiredException
    {
        boolean hasManifestEntries = !config.isManifestEntriesEmpty();
        @SuppressWarnings( "unchecked" )
        Map<String, String> entries = hasManifestEntries ? config.getManifestEntries() : Collections.EMPTY_MAP;
        Manifest manifest = getManifest( session, project, config.getManifest(), entries );

        // any custom manifest entries in the archive configuration manifest?
        if ( hasManifestEntries )
        {

            for ( Map.Entry<String, String> entry : entries.entrySet() )
            {
                String key = entry.getKey();
                String value = entry.getValue();
                Manifest.Attribute attr = manifest.getMainSection().getAttribute( key );
                if ( key.equals( "Class-Path" ) && attr != null )
                {
                    // Merge the user-supplied Class-Path value with the programmatically
                    // generated Class-Path.  Note that the user-supplied value goes first
                    // so that resources there will override any in the standard Class-Path.
                    attr.setValue( value + " " + attr.getValue() );
                }
                else
                {
                    addManifestAttribute( manifest, key, value );
                }
            }
        }

        // any custom manifest sections in the archive configuration manifest?
        if ( !config.isManifestSectionsEmpty() )
        {
            for ( ManifestSection section : config.getManifestSections() )
            {
                Manifest.Section theSection = new Manifest.Section();
                theSection.setName( section.getName() );

                if ( !section.isManifestEntriesEmpty() )
                {
                    Map<String, String> sectionEntries = section.getManifestEntries();

                    for ( Map.Entry<String, String> entry : sectionEntries.entrySet() )
                    {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        Manifest.Attribute attr = new Manifest.Attribute( key, value );
                        theSection.addConfiguredAttribute( attr );
                    }
                }

                manifest.addConfiguredSection( theSection );
            }
        }

        return manifest;
    }

    /**
     * Return a pre-configured manifest
     *
     * @todo Add user attributes list and user groups list
     */
    @SuppressWarnings( { "JavaDoc", "UnusedDeclaration" } )
    public Manifest getManifest( MavenProject project, ManifestConfiguration config )
        throws ManifestException, DependencyResolutionRequiredException
    {
        return getManifest( null, project, config, Collections.<String, String>emptyMap() );
    }

    public Manifest getManifest( MavenSession mavenSession, MavenProject project, ManifestConfiguration config )
        throws ManifestException, DependencyResolutionRequiredException
    {
        return getManifest( mavenSession, project, config, Collections.<String, String>emptyMap() );
    }

    private void addManifestAttribute( Manifest manifest, Map<String, String> map, String key, String value )
        throws ManifestException
    {
        if ( map.containsKey( key ) )
        {
            return;  // The map value will be added later
        }
        addManifestAttribute( manifest, key, value );
    }

    private void addManifestAttribute( Manifest manifest, String key, String value )
        throws ManifestException
    {
        if ( !StringUtils.isEmpty( value ) )
        {
            Manifest.Attribute attr = new Manifest.Attribute( key, value );
            manifest.addConfiguredAttribute( attr );
        }
        else
        {
            // if the value is empty we have create an entry with an empty string 
            // to prevent null print in the manifest file
            Manifest.Attribute attr = new Manifest.Attribute( key, "" );
            manifest.addConfiguredAttribute( attr );
        }
    }

    protected Manifest getManifest( MavenSession session, MavenProject project, ManifestConfiguration config,
                                    Map<String, String> entries )
        throws ManifestException, DependencyResolutionRequiredException
    {
        // TODO: Should we replace "map" with a copy? Note, that we modify it!

        // Added basic entries
        Manifest m = new Manifest();
        addCreatedByEntry( session, m, entries );

        addCustomEntries( m, entries, config );

        if ( config.isAddClasspath() )
        {
            StringBuilder classpath = new StringBuilder();

            @SuppressWarnings( "unchecked" )
            List<String> artifacts = project.getRuntimeClasspathElements();
            String classpathPrefix = config.getClasspathPrefix();
            String layoutType = config.getClasspathLayoutType();
            String layout = config.getCustomClasspathLayout();

            Interpolator interpolator = new StringSearchInterpolator();

            for ( String artifactFile : artifacts )
            {
                File f = new File( artifactFile );
                if ( f.getAbsoluteFile().isFile() )
                {
                    @SuppressWarnings( "unchecked" )
                    Artifact artifact = findArtifactWithFile( project.getArtifacts(), f );

                    if ( classpath.length() > 0 )
                    {
                        classpath.append( " " );
                    }
                    classpath.append( classpathPrefix );

                    // NOTE: If the artifact or layout type (from config) is null, give up and use the file name by itself.
                    if ( artifact == null || layoutType == null )
                    {
                        classpath.append( f.getName() );
                    }
                    else
                    {
                        List<ValueSource> valueSources = new ArrayList<ValueSource>();
                        valueSources.add(
                            new PrefixedObjectValueSource( ARTIFACT_EXPRESSION_PREFIXES, artifact, true ) );
                        valueSources.add( new PrefixedObjectValueSource( ARTIFACT_EXPRESSION_PREFIXES, artifact == null
                            ? null
                            : artifact.getArtifactHandler(), true ) );

                        Properties extraExpressions = new Properties();
                        if ( artifact != null )
                        {
                            // FIXME: This query method SHOULD NOT affect the internal
                            // state of the artifact version, but it does.
                            if ( !artifact.isSnapshot() )
                            {
                                extraExpressions.setProperty( "baseVersion", artifact.getVersion() );
                            }

                            extraExpressions.setProperty( "groupIdPath", artifact.getGroupId().replace( '.', '/' ) );
                            if ( StringUtils.isNotEmpty( artifact.getClassifier() ) )
                            {
                                extraExpressions.setProperty( "dashClassifier", "-" + artifact.getClassifier() );
                                extraExpressions.setProperty( "dashClassifier?", "-" + artifact.getClassifier() );
                            }
                            else
                            {
                                extraExpressions.setProperty( "dashClassifier", "" );
                                extraExpressions.setProperty( "dashClassifier?", "" );
                            }
                        }
                        valueSources.add(
                            new PrefixedPropertiesValueSource( ARTIFACT_EXPRESSION_PREFIXES, extraExpressions, true ) );

                        for ( ValueSource vs : valueSources )
                        {
                            interpolator.addValueSource( vs );
                        }

                        RecursionInterceptor recursionInterceptor =
                            new PrefixAwareRecursionInterceptor( ARTIFACT_EXPRESSION_PREFIXES );

                        try
                        {
                            if ( ManifestConfiguration.CLASSPATH_LAYOUT_TYPE_SIMPLE.equals( layoutType ) )
                            {
                                if ( config.isUseUniqueVersions() )
                                {
                                    classpath.append( interpolator.interpolate( SIMPLE_LAYOUT, recursionInterceptor ) );
                                }
                                else
                                {
                                    classpath.append(
                                        interpolator.interpolate( SIMPLE_LAYOUT_NONUNIQUE, recursionInterceptor ) );
                                }
                            }
                            else if ( ManifestConfiguration.CLASSPATH_LAYOUT_TYPE_REPOSITORY.equals( layoutType ) )
                            {
                                // we use layout /$groupId[0]/../${groupId[n]/$artifactId/$version/{fileName}
                                // here we must find the Artifact in the project Artifacts to generate the maven layout
                                if ( config.isUseUniqueVersions() )
                                {
                                    classpath.append(
                                        interpolator.interpolate( REPOSITORY_LAYOUT, recursionInterceptor ) );
                                }
                                else
                                {
                                    classpath.append(
                                        interpolator.interpolate( REPOSITORY_LAYOUT_NONUNIQUE, recursionInterceptor ) );
                                }
                            }
                            else if ( ManifestConfiguration.CLASSPATH_LAYOUT_TYPE_CUSTOM.equals( layoutType ) )
                            {
                                if ( layout == null )
                                {
                                    throw new ManifestException( ManifestConfiguration.CLASSPATH_LAYOUT_TYPE_CUSTOM
                                                                     + " layout type was declared, but custom layout expression was not specified. Check your <archive><manifest><customLayout/> element." );
                                }

                                classpath.append( interpolator.interpolate( layout, recursionInterceptor ) );
                            }
                            else
                            {
                                throw new ManifestException( "Unknown classpath layout type: '" + layoutType
                                                                 + "'. Check your <archive><manifest><layoutType/> element." );
                            }
                        }
                        catch ( InterpolationException e )
                        {
                            ManifestException error = new ManifestException(
                                "Error interpolating artifact path for classpath entry: " + e.getMessage() );

                            error.initCause( e );
                            throw error;
                        }
                        finally
                        {
                            for ( ValueSource vs : valueSources )
                            {
                                interpolator.removeValuesSource( vs );
                            }
                        }
                    }
                }
            }

            if ( classpath.length() > 0 )
            {
                // Class-Path is special and should be added to manifest even if
                // it is specified in the manifestEntries section
                addManifestAttribute( m, "Class-Path", classpath.toString() );
            }
        }

        if ( config.isAddDefaultSpecificationEntries() )
        {
            addManifestAttribute( m, entries, "Specification-Title", project.getName() );
            addManifestAttribute( m, entries, "Specification-Version", project.getVersion() );

            if ( project.getOrganization() != null )
            {
                addManifestAttribute( m, entries, "Specification-Vendor", project.getOrganization().getName() );
            }
        }

        if ( config.isAddDefaultImplementationEntries() )
        {
            addManifestAttribute( m, entries, "Implementation-Title", project.getName() );
            addManifestAttribute( m, entries, "Implementation-Version", project.getVersion() );
            // MJAR-5
            addManifestAttribute( m, entries, "Implementation-Vendor-Id", project.getGroupId() );

            if ( project.getOrganization() != null )
            {
                addManifestAttribute( m, entries, "Implementation-Vendor", project.getOrganization().getName() );
            }
        }

        String mainClass = config.getMainClass();
        if ( mainClass != null && !"".equals( mainClass ) )
        {
            addManifestAttribute( m, entries, "Main-Class", mainClass );
        }

        // Added extensions
        if ( config.isAddExtensions() )
        {
            // TODO: this is only for applets - should we distinguish them as a packaging?
            StringBuilder extensionsList = new StringBuilder();
            @SuppressWarnings( "unchecked" ) Set<Artifact> artifacts = (Set<Artifact>)project.getArtifacts();

            for ( Artifact artifact : artifacts )
            {
                if ( !Artifact.SCOPE_TEST.equals( artifact.getScope() ) )
                {
                    if ( "jar".equals( artifact.getType() ) )
                    {
                        if ( extensionsList.length() > 0 )
                        {
                            extensionsList.append( " " );
                        }
                        extensionsList.append( artifact.getArtifactId() );
                    }
                }
            }

            if ( extensionsList.length() > 0 )
            {
                addManifestAttribute( m, entries, "Extension-List", extensionsList.toString() );
            }

            for ( Object artifact1 : artifacts )
            {
                // TODO: the correct solution here would be to have an extension type, and to read
                // the real extension values either from the artifact's manifest or some part of the POM
                Artifact artifact = (Artifact) artifact1;
                if ( "jar".equals( artifact.getType() ) )
                {
                    String artifactId = artifact.getArtifactId().replace( '.', '_' );
                    String ename = artifactId + "-Extension-Name";
                    addManifestAttribute( m, entries, ename, artifact.getArtifactId() );
                    String iname = artifactId + "-Implementation-Version";
                    addManifestAttribute( m, entries, iname, artifact.getVersion() );

                    if ( artifact.getRepository() != null )
                    {
                        iname = artifactId + "-Implementation-URL";
                        String url = artifact.getRepository().getUrl() + "/" + artifact.toString();
                        addManifestAttribute( m, entries, iname, url );
                    }
                }
            }
        }

        return m;
    }

    private void addCustomEntries( Manifest m, Map<String, String> entries, ManifestConfiguration config )
        throws ManifestException
    {
        addManifestAttribute( m, entries, "Built-By", System.getProperty( "user.name" ) );
        addManifestAttribute( m, entries, "Build-Jdk", System.getProperty( "java.version" ) );

/* TODO: rethink this, it wasn't working
        Artifact projectArtifact = project.getArtifact();

        if ( projectArtifact.isSnapshot() )
        {
            Manifest.Attribute buildNumberAttr = new Manifest.Attribute( "Build-Number", "" +
                project.getSnapshotDeploymentBuildNumber() );
            m.addConfiguredAttribute( buildNumberAttr );
        }

*/
        if ( config.getPackageName() != null )
        {
            addManifestAttribute( m, entries, "Package", config.getPackageName() );
        }
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public JarArchiver getArchiver()
    {
        return archiver;
    }

    public void setArchiver( JarArchiver archiver )
    {
        this.archiver = archiver;
    }

    public void setOutputFile( File outputFile )
    {
        archiveFile = outputFile;
    }

    /**
     * @deprecated
     */
    @SuppressWarnings( "JavaDoc" )
    public void createArchive( MavenProject project, MavenArchiveConfiguration archiveConfiguration )
        throws ArchiverException, ManifestException, IOException, DependencyResolutionRequiredException
    {
        createArchive( null, project, archiveConfiguration );
    }

    public void createArchive( MavenSession session, MavenProject project,
                               MavenArchiveConfiguration archiveConfiguration )
        throws ArchiverException, ManifestException, IOException, DependencyResolutionRequiredException
    {
        // we have to clone the project instance so we can write out the pom with the deployment version,
        // without impacting the main project instance...
        // TODO use clone() in Maven 2.0.9+
        MavenProject workingProject = new MavenProject( project );

        boolean forced = archiveConfiguration.isForced();
        if ( archiveConfiguration.isAddMavenDescriptor() )
        {
            // ----------------------------------------------------------------------
            // We want to add the metadata for the project to the JAR in two forms:
            //
            // The first form is that of the POM itself. Applications that wish to
            // access the POM for an artifact using maven tools they can.
            //
            // The second form is that of a properties file containing the basic
            // top-level POM elements so that applications that wish to access
            // POM information without the use of maven tools can do so.
            // ----------------------------------------------------------------------

            if ( workingProject.getArtifact().isSnapshot() )
            {
                workingProject.setVersion( workingProject.getArtifact().getVersion() );
            }

            String groupId = workingProject.getGroupId();

            String artifactId = workingProject.getArtifactId();

            archiver.addFile( project.getFile(), "META-INF/maven/" + groupId + "/" + artifactId + "/pom.xml" );

            // ----------------------------------------------------------------------
            // Create pom.properties file
            // ----------------------------------------------------------------------

            File pomPropertiesFile = archiveConfiguration.getPomPropertiesFile();
            if ( pomPropertiesFile != null )
            {
                if ("".equals(pomPropertiesFile.getPath().trim()))
                {
                    pomPropertiesFile = null; // just generate the file instead
                }
                else if ( !pomPropertiesFile.exists() )
                {
                    throw new FileNotFoundException(pomPropertiesFile.getPath());
                }
                else if ( !pomPropertiesFile.isFile())
                {
                    throw new IOException("Not a file: " + pomPropertiesFile.getPath());
                }
                else
                {
                    final File target = new File(
                            new File( workingProject.getBuild().getDirectory(), "maven-archiver" ), "pom.properties" );
                    if ( target.exists () )
                    {
                        // overwrite specified file anyway
                        new PomPropertiesUtil().applyPomProperties( workingProject, archiver, pomPropertiesFile );
                    }
                    else
                    {
                        new PomPropertiesUtil().applyPomProperties( workingProject, archiver, pomPropertiesFile, target );
                    }
                }
            }
            if ( pomPropertiesFile == null )
            {
                File dir = new File( workingProject.getBuild().getDirectory(), "maven-archiver" );
                pomPropertiesFile = new File( dir, "pom.properties" );
                new PomPropertiesUtil().createPomProperties( workingProject, archiver, pomPropertiesFile, forced );
            }
        }

        // ----------------------------------------------------------------------
        // Create the manifest
        // ----------------------------------------------------------------------

        File manifestFile = archiveConfiguration.getManifestFile();

        if ( manifestFile != null )
        {
            archiver.setManifest( manifestFile );
        }

        Manifest manifest = getManifest( session, workingProject, archiveConfiguration );

        // Configure the jar
        archiver.addConfiguredManifest( manifest );

        archiver.setCompress( archiveConfiguration.isCompress() );

        archiver.setIndex( archiveConfiguration.isIndex() );

        archiver.setDestFile( archiveFile );

        // make the archiver index the jars on the classpath, if we are adding that to the manifest
        if ( archiveConfiguration.getManifest().isAddClasspath() )
        {
            @SuppressWarnings( "unchecked" ) List<String> artifacts = project.getRuntimeClasspathElements();
            for ( String artifact : artifacts )
            {
                File f = new File( artifact );
                archiver.addConfiguredIndexJars( f );
            }
        }

        archiver.setForced( forced );
        if ( !archiveConfiguration.isForced() && archiver.isSupportingForced() )
        {
            // TODO Should issue a warning here, but how do we get a logger?
            // TODO getLog().warn( "Forced build is disabled, but disabling the forced mode isn't supported by the archiver." );
        }

        // create archive
        archiver.createArchive();
    }

    private void addCreatedByEntry( MavenSession session, Manifest m, Map entries )
        throws ManifestException
    {
        String createdBy = "Apache Maven";
        if ( session != null ) // can be null due to API backwards compatibility
        {
            String mavenVersion = session.getExecutionProperties().getProperty( "maven.version" );
            if ( mavenVersion != null )
            {
                createdBy += " " + mavenVersion;
            }
        }
        addManifestAttribute( m, entries, "Created-By", createdBy );
    }


    private Artifact findArtifactWithFile( Set<Artifact> artifacts, File file )
    {
        for ( Artifact artifact : artifacts )
        {
            // normally not null but we can check
            if ( artifact.getFile() != null )
            {
                if ( artifact.getFile().equals( file ) )
                {
                    return artifact;
                }
            }
        }
        return null;
    }
}
