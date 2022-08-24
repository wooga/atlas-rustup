/*
 * Copyright 2022 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wooga.gradle.rustup

import com.wooga.gradle.PlatformUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import wooga.gradle.rustup.tasks.Rustup

class RustupPlugin implements Plugin<Project> {

    static final String EXTENSION_NAME = "rustup"

    @Override
    void apply(Project project) {
        project.tasks.register("rustup", Rustup) {
            def cargoHomeDefault = System.getenv("CARGO_HOME") ?: (PlatformUtils.isWindows() ? System.getenv("USERPROFILE") : System.getenv("HOME")) + "/.cargo"
            def rustupHomeDefault = System.getenv("RUSTUP_HOME") ?: (PlatformUtils.isWindows() ? System.getenv("USERPROFILE") : System.getenv("HOME")) + "/.rustup"

            cargoHome.convention(project.layout.projectDirectory.dir(cargoHomeDefault))
            rustupHome.convention(project.layout.projectDirectory.dir(rustupHomeDefault))
            logFile.convention(project.layout.buildDirectory.file("logs/${it.name}.log"))
        }
    }
}
