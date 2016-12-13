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

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import io.spring.gradle.dependencymanagement.dsl.ImportsHandler;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer;
import io.spring.gradle.dependencymanagement.internal.pom.Coordinates;

/**
 * Standard implementation of {@link ImportsHandler}.
 *
 * @author Andy Wilkinson
 */
class StandardImportsHandler extends GroovyObjectSupport implements ImportsHandler {

    private final DependencyManagementContainer container;

    private final Configuration configuration;

    StandardImportsHandler(DependencyManagementContainer container, Configuration configuration) {
        this.container = container;
        this.configuration = configuration;
    }

    @Override
    public void mavenBom(String coordinates) {
        this.mavenBom(coordinates, null);
    }

    @Override
    public void mavenBom(String coordinates, Closure closure) {
        StandardMavenBomHandler mavenBomHandler = new StandardMavenBomHandler();
        if (closure != null) {
            closure.setDelegate(mavenBomHandler);
            closure.setResolveStrategy(Closure.DELEGATE_ONLY);
            closure.call();
        }
        String[] components = coordinates.split(":");
        if (components.length != 3) {
            throw new IllegalArgumentException("Bom coordinates must be of the form groupId:artifactId:version");
        }
        this.container.importBom(this.configuration, new Coordinates(components[0], components[1], components[2]),
                mavenBomHandler.getBomProperties());
    }

    /**
     * Handlers missing properties by returning the {@link Project} property with the given {@code name}.
     *
     * @param name the name of the property
     * @return the value of the project property
     */
    public Object propertyMissing(String name) {
        return this.container.getProject().property(name);
    }

}
