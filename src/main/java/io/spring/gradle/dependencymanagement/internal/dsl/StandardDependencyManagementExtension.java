/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.dependencymanagement.internal.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import io.spring.gradle.dependencymanagement.internal.DependencyManagementSettings;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementSettings.PomCustomizationSettings;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementConfigurer;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementHandler;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer;
import io.spring.gradle.dependencymanagement.internal.StandardPomDependencyManagementConfigurer;

/**
 * Standard implementation of {@link DependencyManagementExtension}.
 *
 * @author Andy Wilkinson
 */
public class StandardDependencyManagementExtension extends GroovyObjectSupport implements DependencyManagementExtension {

    private final DependencyManagementContainer dependencyManagementContainer;

    private final Project project;

    private final DependencyManagementConfigurationContainer configurationContainer;

    private final DependencyManagementSettings dependencyManagementSettings;

    /**
     * Creates a new {@code StandardDependencyManagementExtension} that is associated with the given {@code project}.
     *
     * @param dependencyManagementContainer the container for the project's dependency management
     * @param configurationContainer the container used to create dependency management-specific configurations
     * @param project the project
     * @param dependencyManagementSettings the settings that control dependency management behavior
     */
    public StandardDependencyManagementExtension(DependencyManagementContainer dependencyManagementContainer,
            DependencyManagementConfigurationContainer configurationContainer, Project project,
            DependencyManagementSettings dependencyManagementSettings) {
        this.dependencyManagementContainer = dependencyManagementContainer;
        this.configurationContainer = configurationContainer;
        this.project = project;
        this.dependencyManagementSettings = dependencyManagementSettings;
    }

    @Override
    public void imports(Closure closure) {
        new StandardDependencyManagementConfigurer(this.dependencyManagementContainer).imports(closure);
    }

    @Override
    public void dependencies(Closure closure) {
        new StandardDependencyManagementConfigurer(this.dependencyManagementContainer).dependencies(closure);
    }

    @Override
    public Map<String, String> getImportedProperties() {
        return this.dependencyManagementContainer.importedPropertiesForConfiguration(null);
    }

    @Override
    public Map<String, String> getManagedVersions() {
        return this.dependencyManagementContainer.getManagedVersionsForConfiguration(null);
    }

    @Override
    public Map<String, String> getOwnManagedVersions() {
        return this.dependencyManagementContainer.getManagedVersionsForConfiguration(null, false);
    }

    @Override
    public void resolutionStrategy(final Closure closure) {
        this.configurationContainer.apply(new Action<Configuration>() {

            @Override
            public void execute(Configuration configuration) {
                configuration.resolutionStrategy(closure);
            }

        });
    }

    @Override
    public void generatedPomCustomization(Closure closure) {
        closure.setDelegate(new StandardGeneratedPomCustomizationHandler(
                this.dependencyManagementSettings.getPomCustomizationSettings()));
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
    }

    @Override
    public StandardPomDependencyManagementConfigurer getPomConfigurer() {
        return new StandardPomDependencyManagementConfigurer(
                this.dependencyManagementContainer.getGlobalDependencyManagement(),
                this.dependencyManagementSettings.getPomCustomizationSettings());
    }

    /**
     * Handles missing methods. Calls the closure (the final method argument) with
     * a {@link DependencyManagementHandler} for one or more configurations as its delegate.
     *
     * @param name the name of the method
     * @param args the arguments passed to the method
     * @return the value returned from the closure when it is called
     */
    public Object methodMissing(String name, Object args) {
        Object[] argsArray = (Object[]) args;
        Closure closure;
        if ("configurations".equals(name)) {
            closure = (Closure) argsArray[argsArray.length - 1];
            closure.setDelegate(new CompoundDependencyManagementConfigurer(extractConfigurers(argsArray)));
        }
        else {
            Configuration configuration = this.project.getConfigurations().getAt(name);
            closure = (Closure) argsArray[0];
            closure.setDelegate(new StandardDependencyManagementConfigurer(this.dependencyManagementContainer, configuration));
        }
        closure.setResolveStrategy(Closure.DELEGATE_ONLY);
        return closure.call();
    }

    private List<DependencyManagementHandler> extractConfigurers(Object[] objects) {
        List<DependencyManagementHandler> configurers = new ArrayList<DependencyManagementHandler>();
        for (Object object: objects) {
            if (object instanceof DependencyManagementHandler) {
                configurers.add((DependencyManagementHandler) object);
            }
        }
        return configurers;
    }

    /**
     * Handles missing properties by returning a {@link DependencyManagementConfigurer} for the configuration identified
     * by {@code name}.
     *
     * @param name the name of the configuration
     * @return the {@code DependencyManagementConfigurer} for the configuration
     */
    public Object propertyMissing(String name) {
        return forConfiguration(name);
    }

    private DependencyManagementConfigurer forConfiguration(String name) {
        return new StandardDependencyManagementConfigurer(this.dependencyManagementContainer,
                this.project.getConfigurations().getAt(name));
    }

    @Override
    public void setApplyMavenExclusions(boolean applyMavenExclusions) {
        this.dependencyManagementSettings.setApplyMavenExclusions(applyMavenExclusions);
    }

    @Override
    public void applyMavenExclusions(boolean applyMavenExclusions) {
        this.dependencyManagementSettings.setApplyMavenExclusions(applyMavenExclusions);
    }

    @Override
    public void setOverriddenByDependencies(boolean overriddenByDependencies) {
        this.dependencyManagementSettings.setOverriddenByDependencies(overriddenByDependencies);
    }

    @Override
    public void overriddenByDependencies(boolean overriddenByDependencies) {
        this.dependencyManagementSettings.setOverriddenByDependencies(overriddenByDependencies);
    }

    /**
     * Returns the settings for pom customization.
     *
     * @return the pom customization settings
     */
    public PomCustomizationSettings getPomCustomizationSettings() {
        return this.dependencyManagementSettings.getPomCustomizationSettings();
    }

}
