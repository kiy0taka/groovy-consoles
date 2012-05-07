import javax.swing.UIManager
import groovy.ui.Console
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import groovy.transform.*

class Model {
    List transformations = []
}

def model = new Model()

def astTransformations = [AutoClone, AutoExternalize, Canonical, EqualsAndHashCode,
    Immutable, InheritConstructors, ToString, TupleConstructor]

def menu = {
    menu 'Customizers', {
        menu 'AST Transformation', {
            astTransformations.each { atf ->
                checkBoxMenuItem atf.simpleName, itemStateChanged: { e ->
                    if (e.source.selected) {
                        model.transformations << atf
                    } else {
                        model.transformations.remove atf
                    }
                }
            }
        }
    }
}

UIManager.lookAndFeel = UIManager.systemLookAndFeelClassName

def console = new Console(Console.class.classLoader.getRootLoader())
def beforeExecution = {
    delegate.config.addCompilationCustomizers(*(model.transformations.collect{new ASTTransformationCustomizer(it)}))
}
beforeExecution.delegate = console
console.beforeExecution = beforeExecution
console.run(
    Console.frameConsoleDelegates << [menuBarDelegate: {arg->
        current.JMenuBar = build(arg)
        current.JMenuBar.add(build(menu))
    }
])
