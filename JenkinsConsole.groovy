@GrabResolver('http://repo.jenkins-ci.org/public')
@Grab('org.jenkins-ci.main:cli:1.459')
import hudson.cli.CLI
import javax.swing.*
import groovy.beans.Bindable
import groovy.ui.Console

class JenkinsMenuModel {
    @Bindable String url
    @Bindable String args = ''
    @Bindable String username
    @Bindable String password
    @Bindable String command
}

def model = new JenkinsMenuModel(url:(args ? args[0] : 'http://localhost:8080'))

def cli = { String... args ->
    new CLI(model.url.toURL()).execute(args as List, System.in, System.out, System.err)
}

def jenkinsMenu = {
    def configPanel = panel {
        gridLayout(cols:1, rows:4)
        label 'Jenkins URL:'
        textField(columns:20, text:bind('url', source:model, mutual:true))
        label 'Script Arguments:'
        textField(columns:20, text:bind('args', source:model, mutual:true))
    }
    def loginPanel = panel {
        gridLayout(cols:2, rows:2)
        label 'User Name'
        textField(text:bind('username', source:model, mutual:true))
        label 'Password'
        textField(text:bind('password', source:model, mutual:true))
    }
    def commandPanel = panel {
        gridLayout(cols:1, rows:2)
        label 'Command:'
        textField(columns:30, text:bind('command', source:model, mutual:true))
    }
    def showDialog = { title, panel, optionType = JOptionPane.DEFAULT_OPTION, closure ->
        optionPane(message:panel, optionType:optionType).with {
            createDialog(title).show()
            if (value == JOptionPane.OK_OPTION) closure()
        }
    }
    menu(text:'Jenkins') {
        menuItem('Login', actionPerformed:{
            showDialog('Login', loginPanel, JOptionPane.OK_CANCEL_OPTION) {
                cli 'login', '--username', model.username, '--password', model.password
            }
        })
        menuItem('Logout', actionPerformed: { cli 'logout' })
        menuItem('Run Command', actionPerformed: {
            showDialog('Run Command', commandPanel) {
                cli model.command.split()
            }
        })
        separator()
        menuItem('Run Configuration', actionPerformed:{
            showDialog('Run Configuration', configPanel, {})
        })
    }
}

Console.metaClass.newScript = { ClassLoader parent, Binding binding ->
    delegate.shell = new GroovyShell(parent, binding)
    delegate.shell.metaClass.run = { String scriptText, String fileName, List list ->
        def file = File.createTempFile('jenkinsgroovyconsole', '.groovy')
        file.text = scriptText
        cli(*['groovy', file.absolutePath, *(model.args.split())])
        file.delete()
        null
    }
}

UIManager.lookAndFeel = UIManager.systemLookAndFeelClassName

new Console(Console.class.classLoader.getRootLoader()).run(
    Console.frameConsoleDelegates << [menuBarDelegate: {arg->
        current.JMenuBar = build(arg)
        current.JMenuBar.add(build(jenkinsMenu))
    }
])
