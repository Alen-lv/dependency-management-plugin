/*
 * Copyright 2014 the original author or authors.
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

package io.spring.platform.gradle.dependencymanagement

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ModuleDependency


class DependencyManagementPlugin implements Plugin<Project> {

	DependencyManagement dependencyManagement

	@Override
	public void apply(Project project) {
		Configuration configuration = project.getConfigurations().detachedConfiguration()

		dependencyManagement = new DependencyManagement(configuration: configuration, project: project)

		project.extensions.add("dependencyManagement", DependencyManagementExtension)
		project.extensions.configure(DependencyManagementExtension) { DependencyManagementExtension extension ->
			extension.configuration = configuration
			extension.dependencies = project.dependencies
			extension.dependencyManagement = dependencyManagement
		}

		project.configurations.all { Configuration c ->
			c.incoming.beforeResolve {
				dependencies.findAll { it in ModuleDependency }.each {
					if (it.version) {
						dependencyManagement.versions["$it.group:$it.name"] = it.version
					}
				}
			}
		}

		project.configurations.all {
			resolutionStrategy {
				eachDependency { DependencyResolveDetails details ->
					dependencyManagement.apply(details)
				}
			}
		}
	}
}
