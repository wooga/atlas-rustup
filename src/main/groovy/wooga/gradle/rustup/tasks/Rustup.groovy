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

package wooga.gradle.rustup.tasks

import com.wooga.gradle.ArgumentsSpec
import com.wooga.gradle.PlatformUtils
import com.wooga.gradle.io.LogFileSpec
import com.wooga.gradle.io.ProcessExecutor
import com.wooga.gradle.io.ProcessOutputSpec
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult
import wooga.gradle.rustup.RustupSpec

class Rustup extends DefaultTask implements RustupSpec, ArgumentsSpec, LogFileSpec, ProcessOutputSpec {

    Rustup() {
        internalArguments = project.provider({
            List<String> arguments = new ArrayList<String>()
            arguments << "-y" << "--no-modify-path"

            if (defaultHost.isPresent()) {
                arguments << "--default-host" << defaultHost.get()
            }

            arguments << "--default-toolchain" << defaultToolchain.getOrElse("none")

            if (profile.present) {
                arguments << "--profile" << profile.get()
            }

            if (components.present) {
                components.get().each {
                    arguments << "--component" << it
                }
            }

            if (targets.present) {
                targets.get().each {
                    arguments << "--target" << it
                }
            }

            arguments as List<String>
        })

        environment.set(cargoHome.zip(rustupHome, { cargoHome, rustupHome ->
            [
                    "CARGO_HOME" : cargoHome.asFile.absolutePath,
                    "RUSTUP_HOME": rustupHome.asFile.absolutePath
            ]
        }))

        rustup = cargoHome.file(providers.provider({
            PlatformUtils.isWindows() ? "bin/rustup.exe" : "bin/rustup"
        }))

        outputs.upToDateWhen { false }
    }

    private final Provider<RegularFile> rustup

    @TaskAction
    void rustupInstall() {
        File rustup = rustup.get().asFile
        if (!rustup.exists()) {
            logger.info("setup rustup")
            String postfixName = PlatformUtils.isWindows() ? ".exe" : "init.sh"
            String prefixName = PlatformUtils.isWindows() ? "rustup-init" : "rustup"
            File rustupInstallScript = File.createTempFile(prefixName, postfixName)

            def url = new URL(PlatformUtils.isWindows() ? "https://static.rust-lang.org/rustup/dist/x86_64-pc-windows-msvc/rustup-init.exe" : "https://sh.rustup.rs")
            url.withInputStream { i ->
                rustupInstallScript.withOutputStream {
                    it << i
                }
            }
            rustupInstallScript.setExecutable(true)

            ProcessExecutor executor = ProcessExecutor.from(project)
                    .withExecutable(rustupInstallScript)
                    .withArguments(this, true)
                    .withOutput(this, logFile.asFile.getOrNull())
                    .withOutputLogFile(this, this)

            executor.execute()
        } else {
            logger.info("rustup already installed.")
            if (update.present && update.get()) {
                logger.info("update rustup")
                runRustupCommand("self", "update")
                runRustupCommand("update")
            }

            targets.get().each { target ->
                runRustupCommand("target", "add", target)
            }

            components.get().each { component ->
                runRustupCommand("component", "add", component)
            }

            if (defaultToolchain.present) {
                def toolchain = defaultToolchain.get()
                runRustupCommand("toolchain", "install", toolchain)
                runRustupCommand("default", toolchain)
            }
        }
    }

    ExecResult runRustupCommand(String... arguments) {
        ProcessExecutor executor = ProcessExecutor.from(project)
                .withExecutable(rustup.get().asFile.absolutePath)
                .withArguments(arguments)
                .withEnvironment(environment.get())
                .withOutput(this, logFile.asFile.getOrNull())
                .withOutputLogFile(this, this)
        executor.execute()
    }
}
