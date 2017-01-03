/*
 * Copyright 2014-2017 the original author or authors.
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

package io.spring.gradle.dependencymanagement.internal.maven

import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer
import io.spring.gradle.dependencymanagement.internal.DependencyManagementSettings.PomCustomizationSettings
import io.spring.gradle.dependencymanagement.internal.StandardPomDependencyManagementConfigurer
import io.spring.gradle.dependencymanagement.internal.pom.Coordinates
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
/**
 * Tests for {@link StandardPomDependencyManagementConfigurer}
 *
 * @author Andy Wilkinson
 */
class StandardPomDependencyManagementConfigurerSpec extends Specification {

    Project project

    DependencyManagementContainer dependencyManagement

    def setup() {
        project = new ProjectBuilder().build()
        project.repositories {
            mavenCentral()
        }
        DependencyManagementConfigurationContainer configurationContainer = new
                DependencyManagementConfigurationContainer(project)
        this.dependencyManagement = new DependencyManagementContainer(project, new MavenPomResolver(project,
                configurationContainer))
    }

    def "An imported bom is imported in the pom"() {
        given: 'Dependency management that imports a bom'
            this.dependencyManagement.importBom(null, new Coordinates('io.spring.platform', 'platform-bom',
                    '1.0.3.RELEASE'), [:]);
        when: 'The pom is configured'
            Node pom = new XmlParser().parseText("<project></project>")
            new StandardPomDependencyManagementConfigurer(dependencyManagement.globalDependencyManagement,
                    new PomCustomizationSettings()).configurePom(pom)
        then: 'The imported bom has been added'
            pom.dependencyManagement.dependencies.dependency.size() == 1
            def dependency = pom.dependencyManagement.dependencies.dependency[0]
            dependency.groupId[0].value() == 'io.spring.platform'
            dependency.artifactId[0].value() == 'platform-bom'
            dependency.version[0].value() == '1.0.3.RELEASE'
            dependency.scope[0].value() == 'import'
            dependency.type[0].value() == 'pom'
    }

    def "Multiple imports are imported in the order in which they were imported"() {
        given: 'Dependency management that imports two boms'
            this.project.repositories {
                maven { url new File("src/test/resources/maven-repo").toURI().toURL().toString() }
            }
            this.dependencyManagement.importBom(null, new Coordinates('test', 'bravo-pom-customization-bom', '1.0'), [:])
            this.dependencyManagement.importBom(null, new Coordinates('test', 'alpha-pom-customization-bom', '1.0'), [:])

        when: 'The pom is configured'
            Node pom = new XmlParser().parseText("<project></project>")
            PomCustomizationSettings settings = new PomCustomizationSettings()
            new StandardPomDependencyManagementConfigurer(dependencyManagement.globalDependencyManagement, settings).configurePom(pom)
        then: 'The imported boms have been imported in their imported order'
            pom.dependencyManagement.dependencies.dependency.size() == 2
            def dependency1 = pom.dependencyManagement.dependencies.dependency[0]
            dependency1.groupId[0].value() == 'test'
            dependency1.artifactId[0].value() == 'bravo-pom-customization-bom'
            dependency1.version[0].value() == '1.0'
            dependency1.scope[0].value() == 'import'
            dependency1.type[0].value() == 'pom'

            def dependency2 = pom.dependencyManagement.dependencies.dependency[1]
            dependency2.groupId[0].value() == 'test'
            dependency2.artifactId[0].value() == 'alpha-pom-customization-bom'
            dependency2.version[0].value() == '1.0'
            dependency2.scope[0].value() == 'import'
            dependency2.type[0].value() == 'pom'
    }

    def "Customization of published poms can be disabled"() {
        given: 'Dependency management that imports a bom'
            this.dependencyManagement.importBom(null, new Coordinates('io.spring.platform', 'platform-bom',
                    '1.0.3.RELEASE'), [:]);
        when: 'The pom is configured'
            Node pom = new XmlParser().parseText("<project></project>")
            PomCustomizationSettings settings = new PomCustomizationSettings()
            settings.enabled = false
            new StandardPomDependencyManagementConfigurer(dependencyManagement.globalDependencyManagement, settings).configurePom(pom)
        then: 'The imported bom has not been added'
            pom.dependencyManagement.dependencies.dependency.size() == 0
    }

    def "Individual dependency management is added to the pom"() {
        given: 'Dependency management that manages a dependency'
            this.dependencyManagement.addManagedVersion(null, 'org.springframework', 'spring-core', '4.1.3.RELEASE',[])
        when: 'The pom is configured'
            Node pom = new XmlParser().parseText("<project></project>")
            new StandardPomDependencyManagementConfigurer(dependencyManagement.globalDependencyManagement,
                    new PomCustomizationSettings()).configurePom(pom)
        then: 'The managed dependency has been added'
            pom.dependencyManagement.dependencies.dependency.size() == 1
            def dependency = pom.dependencyManagement.dependencies.dependency[0]
            dependency.groupId[0].value() == 'org.springframework'
            dependency.artifactId[0].value() == 'spring-core'
            dependency.version[0].value() == '4.1.3.RELEASE'
            dependency.type.size() == 0
    }

    def "Dependency management can be added to a pom with existing dependency management"() {
        given: 'Dependency management that imports a bom'
            this.dependencyManagement.importBom(null, new Coordinates('io.spring.platform', 'platform-bom',
                    '1.0.3.RELEASE'), [:]);
        when: 'The pom with existing dependency management is configured'
            Node pom = new XmlParser().parseText("<project><dependencyManagement><dependencies></dependencies></dependencyManagement></project>")
            new StandardPomDependencyManagementConfigurer(this.dependencyManagement.globalDependencyManagement,
                    new PomCustomizationSettings()).configurePom(pom)
        then: 'The imported bom has been added'
            pom.dependencyManagement.dependencies.dependency.size() == 1
            def dependency = pom.dependencyManagement.dependencies.dependency[0]
            dependency.groupId[0].value() == 'io.spring.platform'
            dependency.artifactId[0].value() == 'platform-bom'
            dependency.version[0].value() == '1.0.3.RELEASE'
            dependency.scope[0].value() == 'import'
            dependency.type[0].value() == 'pom'
    }

    def "Dependency management exclusions are added to the pom"() {
        given: 'Dependency management that manages a dependency with an exclusion'
            this.dependencyManagement.addManagedVersion(null, 'org.springframework', 'spring-core', '4.1.3.RELEASE',
                    ['commons-logging:commons-logging', 'foo:bar'])
        when: 'The pom is configured'
            Node pom = new XmlParser().parseText("<project></project>")
            new StandardPomDependencyManagementConfigurer(dependencyManagement.globalDependencyManagement,
                    new PomCustomizationSettings()).configurePom(pom)
        then: 'The managed dependency has been added with its exclusions'
            pom.dependencyManagement.dependencies.dependency.size() == 1
            def dependency = pom.dependencyManagement.dependencies.dependency[0]
            dependency.groupId[0].value() == 'org.springframework'
            dependency.artifactId[0].value() == 'spring-core'
            dependency.version[0].value() == '4.1.3.RELEASE'
            def exclusions = [dependency.exclusions.exclusion.groupId[0].value() + ":" +
                                      dependency.exclusions.exclusion.artifactId[0].value(),
                              dependency.exclusions.exclusion.groupId[1].value() + ":" +
                                      dependency.exclusions.exclusion.artifactId[1].value()]
            exclusions.contains("commons-logging:commons-logging")
            exclusions.contains("foo:bar")
    }
}
