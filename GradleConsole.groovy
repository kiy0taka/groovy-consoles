@GrabResolver('http://repo.gradle.org/gradle/libs/')
@Grab('org.gradle:gradle-tooling-api:1.0')
import org.gradle.tooling.BuildException
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProgressListener
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.GradleTask

import javax.swing.*
import groovy.ui.Console
import groovy.beans.Bindable

def templates = [:]

def browse = java.awt.Desktop.desktop.&browse

class Model {
    @Bindable boolean autoSave
    @Bindable String defaultTasks
}

def model = new Model()

def gradleMenu = {
    def showDialog = { title, panel, optionType = JOptionPane.DEFAULT_OPTION ->
        optionPane(message:panel, optionType:optionType).with {
            createDialog(title).show()
        }
    }
    def settingsPanel = panel {
        gridLayout(rows:2, cols:2)
        label 'Auto save before run script.'
        checkBox selected:bind('autoSave', source:model, mutual:true)
        label 'Specify task(s) to run separated by spaces.'
        textField text:bind('defaultTasks', source:model, mutual:true)
    }
    menu('Gradle') {
        menuItem 'Settings', actionPerformed: {
            showDialog 'Gradle settings', settingsPanel
        }
        menu('Templates') {
            templates.each { name, template ->
                menuItem name, actionPerformed: { inputEditor.textEditor.text = template }
            }
            def templateDir = new File(System.getProperty('user.home')+'/.gradle/templates')
            if (templateDir.exists()) {
                separator()
                templateDir.eachFileMatch(groovy.io.FileType.FILES, ~/.*\.gradle/) { file ->
                    menuItem file.name - ~/\.gradle$/, actionPerformed: { inputEditor.textEditor.text = file.text }
                }
            }
        }
        menu('Documents') {
            menuItem 'User Guide', actionPerformed: { browse 'http://www.gradle.org/docs/current/userguide/userguide.html'.toURI() }
        }
    }
}

Console.metaClass.newScript = { ClassLoader parent, Binding binding ->
    def console = delegate
    delegate.shell = new GroovyShell(parent, binding)
    delegate.shell.metaClass.run = { String scriptText, String fileName, List list ->
        // need to save as...
        if (console.scriptFile == null && !fileSaveAs()) {
            return
        }
        // needs to save current script.
        if (model.autoSave) {
            fileSave()
        } else if (!console.askToSaveFile() || console.dirty) {
            return
        }
        def tasks = model.defaultTasks?.split()
        if (!tasks) {
            console.swing.edt {
                tasks = JOptionPane.showInputDialog("Task(s) to run.")?.split()
            }
        }
        if (!tasks) {
            return
        }
        console.swing.doOutside {
            def connector = GradleConnector.newConnector()
            connector.forProjectDirectory(console.scriptFile.parentFile)
            def connection
            try {
                connection = connector.connect()
                connection.newBuild().forTasks(tasks).run()
            } catch (BuildException ignore) {
                // NOP
            } finally {
                connection?.close()
            }
        }
        null
    }
}

UIManager.lookAndFeel = UIManager.systemLookAndFeelClassName
new Console(Console.class.classLoader.getRootLoader()).run(
    Console.frameConsoleDelegates << [menuBarDelegate: {arg->
        current.JMenuBar = build(arg)
        current.JMenuBar.add(build(gradleMenu))
    }]
)
