package org.simplify4u.plugins;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.testng.annotations.Test;

import java.util.Set;

import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.simplify4u.plugins.MavenCompilerUtils.checkCompilerPlugin;
import static org.simplify4u.plugins.MavenCompilerUtils.extractAnnotationProcessors;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

@SuppressWarnings({"ConstantConditions", "SameParameterValue"})
public final class MavenCompilerUtilsTest {

    @Test
    public void testCheckCompilerPlugin() {
        assertThrows(NullPointerException.class, () -> checkCompilerPlugin(null));
        final Plugin compilerPlugin = mock(Plugin.class);
        when(compilerPlugin.getGroupId()).thenReturn("org.apache.maven.plugins");
        when(compilerPlugin.getArtifactId()).thenReturn("maven-compiler-plugin");
        when(compilerPlugin.getVersion()).thenReturn("3.8.1");
        assertTrue(checkCompilerPlugin(compilerPlugin));
        final Plugin otherPlugin = mock(Plugin.class);
        when(otherPlugin.getGroupId()).thenReturn("org.apache.maven.plugin");
        when(otherPlugin.getArtifactId()).thenReturn("some-other-plugin");
        when(otherPlugin.getVersion()).thenReturn("3.5.9");
        assertFalse(checkCompilerPlugin(otherPlugin));
    }

    @Test
    public void testExtractAnnotationProcessorsIllegalInputs() {
        assertThrows(NullPointerException.class, () -> extractAnnotationProcessors(null, null));
        final Plugin badPlugin = mock(Plugin.class);
        when(badPlugin.getGroupId()).thenReturn("org.my-bad-plugin");
        when(badPlugin.getArtifactId()).thenReturn("bad-plugin");
        when(badPlugin.getVersion()).thenReturn("1.1.1");
        assertThrows(NullPointerException.class, () -> extractAnnotationProcessors(null, badPlugin));
        final RepositorySystem repository = mock(RepositorySystem.class);
        assertThrows(NullPointerException.class, () -> extractAnnotationProcessors(repository, null));
        assertThrows(IllegalArgumentException.class, () -> extractAnnotationProcessors(repository, badPlugin));
    }

    @Test
    public void testExtractAnnotationProcessorsNoConfiguration() {
        final RepositorySystem repository = mock(RepositorySystem.class);
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getGroupId()).thenReturn("org.apache.maven.plugins");
        when(plugin.getArtifactId()).thenReturn("maven-compiler-plugin");
        when(plugin.getVersion()).thenReturn("3.8.1");
        assertEquals(extractAnnotationProcessors(repository, plugin), emptySet());
    }

    @Test
    public void testExtractAnnotationProcessorsUnsupportedConfigurationType() {
        final RepositorySystem repository = mock(RepositorySystem.class);
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getGroupId()).thenReturn("org.apache.maven.plugins");
        when(plugin.getArtifactId()).thenReturn("maven-compiler-plugin");
        when(plugin.getVersion()).thenReturn("3.8.1");
        when(plugin.getConfiguration()).thenReturn("Massive configuration encoded in magic \"Hello World!\" string.");
        assertThrows(UnsupportedOperationException.class, () -> extractAnnotationProcessors(repository, plugin));
    }

    @Test
    public void testExtractAnnotationProcessors() {
        final RepositorySystem repository = mock(RepositorySystem.class);
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getGroupId()).thenReturn("org.apache.maven.plugins");
        when(plugin.getArtifactId()).thenReturn("maven-compiler-plugin");
        when(plugin.getVersion()).thenReturn("3.8.1");
        when(plugin.getConfiguration()).thenReturn(createConfiguration());
        when(repository.createArtifact(anyString(), anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            final Artifact artifact = mock(Artifact.class);
            when(artifact.getGroupId()).thenReturn(invocation.getArgument(0));
            when(artifact.getArtifactId()).thenReturn(invocation.getArgument(1));
            when(artifact.getVersion()).thenReturn(invocation.getArgument(2));
            return artifact;
        });
        final Set<Artifact> result = extractAnnotationProcessors(repository, plugin);
        assertEquals(result.size(), 1);
        final Artifact resultElement = result.iterator().next();
        assertEquals(resultElement.getGroupId(), "myGroupId");
        assertEquals(resultElement.getArtifactId(), "myArtifactId");
        assertEquals(resultElement.getVersion(), "1.2.3");
    }

    private static Xpp3Dom createConfiguration() {
        final Xpp3Dom config = new Xpp3Dom("configuration");
        final Xpp3Dom annotationProcessorPaths = new Xpp3Dom("annotationProcessorPaths");
        annotationProcessorPaths.addChild(createPath("myGroupId", "myArtifactId", "1.2.3"));
        annotationProcessorPaths.addChild(createPath("", "myArtifactId", "1.2.3"));
        annotationProcessorPaths.addChild(createPath("myGroupId", "", "1.2.3"));
        annotationProcessorPaths.addChild(createPath(null, "myArtifactId", "1.2.3"));
        annotationProcessorPaths.addChild(createPath("myGroupId", null, "1.2.3"));
        annotationProcessorPaths.addChild(createPath("myGroupId", "myArtifactId", null));
        config.addChild(annotationProcessorPaths);
        return config;
    }

    private static Xpp3Dom createPath(String groupId, String artifactId, String version) {
        final Xpp3Dom path = new Xpp3Dom("path");
        if (groupId != null) {
            final Xpp3Dom groupIdNode = new Xpp3Dom("groupId");
            groupIdNode.setValue(groupId);
            path.addChild(groupIdNode);
        }
        if (artifactId != null) {
            final Xpp3Dom artifactIdNode = new Xpp3Dom("artifactId");
            artifactIdNode.setValue(artifactId);
            path.addChild(artifactIdNode);
        }
        if (version != null) {
            final Xpp3Dom versionNode = new Xpp3Dom("version");
            versionNode.setValue(version);
            path.addChild(versionNode);
        }
        return path;
    }
}