import javax.swing.UIManager
import groovy.ui.Console
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import groovy.transform.*

def transformations = [ThreadInterrupt, Canonical]

Console.metaClass.newScript = { ClassLoader parent, Binding binding ->
    def config = new CompilerConfiguration()
    config.addCompilationCustomizers(*transformations.collect{new ASTTransformationCustomizer(it)})
    delegate.shell = new GroovyShell(parent, binding, config)
}

UIManager.lookAndFeel = UIManager.systemLookAndFeelClassName
new Console(Console.class.classLoader.getRootLoader()).run()
