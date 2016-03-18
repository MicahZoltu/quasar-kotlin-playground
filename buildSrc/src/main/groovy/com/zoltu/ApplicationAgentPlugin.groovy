package com.zoltu

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

class ApplicationAgentPlugin implements Plugin<Project> {
	void apply(Project project) {
		project.extensions.create("applicationAgent", ApplicationAgentPluginExtension)

		project.configurations {
			agent
		}

		project.startScripts {
			doLast {
				if (!project.applicationAgent.applyToStartScripts)
					return

				String agentFileName = project.configurations.agent.singleFile.name

				String forwardSlash = "/"
				String unixRegex = $/exec "$$JAVACMD" "$${JVM_OPTS[@]}"/$
				String unixReplacement = $/exec "$$JAVACMD" -javaagent:"$$APP_HOME/lib${forwardSlash}${agentFileName}" "$${JVM_OPTS[@]}"/$
				unixScript.text = unixScript.text.replace(unixRegex, unixReplacement)

				String windowsRegex = $/"%JAVA_EXE%" %DEFAULT_JVM_OPTS%/$
				String windowsReplacement = $/"%JAVA_EXE%" -javaagent:"%APP_HOME%\lib\$agentFileName" %DEFAULT_JVM_OPTS%/$
				windowsScript.text = windowsScript.text.replace(windowsRegex, windowsReplacement)
			}
		}

		project.tasks.withType(Test) {
			doFirst {
				if (!project.applicationAgent.applyToTests)
					return

				jvmArgs "-javaagent:${project.configurations.agent.singleFile.path}"
			}
		}

		project.tasks.run {
			doFirst {
				if (!project.applicationAgent.applyToRun)
					return

				project.applicationDefaultJvmArgs += [
						"-javaagent:${project.configurations.agent.singleFile.path}"
				]
			}
		}
	}
}

class ApplicationAgentPluginExtension {
	Boolean applyToTests = true
	Boolean applyToRun = true
	Boolean applyToStartScripts = true
}
