@Grab('org.codehaus.groovy.modules.remote:remote-transport-http:0.3')
import groovyx.remote.transport.http.HttpTransport
import groovyx.remote.client.*
import javax.swing.UIManager
import groovy.ui.Console
import org.codehaus.groovy.control.*
import groovy.beans.Bindable
import java.security.*
import org.codehaus.groovy.ast.ClassNode
import javax.swing.JOptionPane

class AppRemoteControl extends RemoteControl {
    public AppRemoteControl(Transport transport, BytesCachedGroovyClassLoader classLoader) {
        super(transport, new RemoteCommandGenerator(classLoader))
    }
}

class BytesCachedClassCollector extends GroovyClassLoader.ClassCollector {

    def cache = [:]

    protected BytesCachedClassCollector(GroovyClassLoader.InnerLoader cl, CompilationUnit unit, SourceUnit su) {
        super(cl, unit, su)
    }

    @Override
    protected Class createClass(byte[] code, ClassNode classNode) {
        cache[classNode.name] = code
        return super.createClass(code, classNode)
    }
}

class BytesCachedGroovyClassLoader extends GroovyClassLoader {

    def classCollector;

    protected GroovyClassLoader.ClassCollector createCollector(CompilationUnit unit, SourceUnit su) {
        GroovyClassLoader.InnerLoader loader = AccessController.doPrivileged(new PrivilegedAction<GroovyClassLoader.InnerLoader>() {
            public GroovyClassLoader.InnerLoader run() {
                return new GroovyClassLoader.InnerLoader(BytesCachedGroovyClassLoader.this)
            }
        });
        classCollector = new BytesCachedClassCollector(loader, unit, su)
    }
}

class RemoteCommandGenerator extends CommandGenerator {

    public RemoteCommandGenerator(BytesCachedGroovyClassLoader cl) {
        super(cl)
    }

    protected byte[] getClassBytes(Class closureClass) {
        def classBytes = classLoader.classCollector.cache[closureClass.name]
        if (classBytes == null) {
            throw new IllegalStateException("Could not find class file for class [${closureClass.name}]");
        }
        classBytes
    }

    @Override
    protected List<byte[]> getSupportingClassesBytes(Class closureClass) {
        def className = closureClass.name
        classLoader.classCollector.cache.inject([]) { classes, entry ->
            if (entry.key != className && entry.key.startsWith(className)) {
                classes << entry.value
            }
            classes
        }
    }
}

class Model {
    @Bindable remoteUrl
}
def model = new Model(remoteUrl:args ? args[0] : 'http://localhost:8080/plugin/groovy-remote/')

def remoteMenu = {
    def configPanel = panel {
        gridLayout(cols:1, rows:2)
        label 'Remote Receiver URL:'
        textField(text:bind('remoteUrl', source:model, mutual:true))
    }
    menu('Remote Control') {
        menuItem('Configuration', actionPerformed:{
            optionPane(message:configPanel, optionType:JOptionPane.DEFAULT_OPTION).with {
                createDialog('Configuration').show()
            }
        })
    }
}

Console.metaClass.newScript = { ClassLoader parent, Binding binding ->
    delegate.shell = new GroovyShell(parent, binding)
    delegate.shell.metaClass.run = { String scriptText, String fileName, List list ->
        def cl = new BytesCachedGroovyClassLoader()
        binding.remote = new AppRemoteControl(new HttpTransport(model.remoteUrl), cl)
        Script s = (Script) cl.parseClass(scriptText).newInstance()
        s.binding = binding
        s.run()
    }
}

UIManager.lookAndFeel = UIManager.systemLookAndFeelClassName
new Console(Console.class.classLoader.getRootLoader()).run(
    Console.frameConsoleDelegates << [menuBarDelegate: {arg->
        current.JMenuBar = build(arg)
        current.JMenuBar.add(build(remoteMenu))
    }
])
