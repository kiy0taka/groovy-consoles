import javax.swing.UIManager
import groovy.ui.Console
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.*
import groovy.transform.*
import groovy.beans.Bindable
import javax.swing.*
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.classgen.*
import org.codehaus.groovy.ast.stmt.*

class SecureModel {
    static final TOKENS = [
        PLUS:Types.PLUS, MINUS:Types.MINUS, MULTIPLY:Types.MULTIPLY, DIVIDE:Types.DIVIDE,
        MOD:Types.MOD, POWER:Types.POWER, PLUS_PLUS:Types.PLUS_PLUS, MINUS_MINUS:Types.MINUS_MINUS,
        COMPARE_EQUAL:Types.COMPARE_EQUAL, COMPARE_NOT_EQUAL:Types.COMPARE_NOT_EQUAL,
        COMPARE_LESS_THAN:Types.COMPARE_LESS_THAN, COMPARE_LESS_THAN_EQUAL:Types.COMPARE_LESS_THAN_EQUAL,
        COMPARE_GREATER_THAN:Types.COMPARE_GREATER_THAN, COMPARE_GREATER_THAN_EQUAL:Types.COMPARE_GREATER_THAN_EQUAL]
    static final constTypes = [
        'Integer':Integer, 'Float':Float, 'Long':Long, 'Double':Double, 'BigDecimal':BigDecimal,
        'Integer.TYPE':Integer.TYPE, 'Long.TYPE':Long.TYPE, 'Float.TYPE':Float.TYPE, 'Double.TYPE':Double.TYPE]
    static final receiversClasses = [Math:Math, Integer:Integer, Float:Float, Double:Double, Long:Long, BigDecimal:BigDecimal]
    @Bindable boolean closuresAllowed
    @Bindable boolean methodDefinitionAllowed
    @Bindable boolean packageAllowed
    @Bindable boolean indirectImportCheckEnabled

    @Bindable boolean importsWhite
    @Bindable List importsList
    @Bindable boolean starImportsWhite
    @Bindable List starImportsList
    @Bindable boolean staticImportsWhite
    @Bindable List staticImportsList = []
    @Bindable boolean staticStarImportsWhite
    @Bindable List staticStarImportsList = []
    @Bindable boolean tokensWhite
    @Bindable List tokensList = []
    @Bindable boolean constantTypesClassesWhite
    @Bindable List constantTypesClassesList = []
    @Bindable boolean receiversClassesWhite
    @Bindable List receiversClassesList = []

    def toCustomizer() {
        def secure = new SecureASTCustomizer(closuresAllowed:closuresAllowed, methodDefinitionAllowed:methodDefinitionAllowed)
        ['imports', 'starImports', 'staticImports', 'staticStarImports', 'tokens', 'constantTypesClasses', 'receiversClasses'].each { propName ->
            if (this."${propName}List") {
                secure[this."${propName}White" ? "${propName}Whitelist" : "${propName}Blacklist"] = this."${propName}List"
            }
        }
        secure
    }
}
def secureModel = new SecureModel()

UIManager.lookAndFeel = UIManager.systemLookAndFeelClassName

def console = new Console(Console.class.classLoader.getRootLoader())
def beforeExecution = {
    def secure = delegate.config.compilationCustomizers.find { it in SecureASTCustomizer }
    if (secure) delegate.config.compilationCustomizers.remove secure
    delegate.config.addCompilationCustomizers(secureModel.toCustomizer())
}
beforeExecution.delegate = console
console.beforeExecution = beforeExecution

final TRANSFORMATIONS = [
    AutoClone, AutoExternalize, Canonical, EqualsAndHashCode,
    Immutable, InheritConstructors, ToString, TupleConstructor
].collectEntries { [(it.simpleName):new ASTTransformationCustomizer(it)] }

final EXPRESSIONS = [
    ArrayExpression, BinaryExpression, BitwiseNegationExpression, BooleanExpression,
    BytecodeExpression, CastExpression, ClassExpression, ClosureExpression, ConstantExpression,
    ConstructorCallExpression, EmptyExpression, FieldExpression, GStringExpression,
    ListExpression,MapEntryExpression, MapExpression, MethodCallExpression, MethodPointerExpression,
    PostfixExpression, PrefixExpression, PropertyExpression, RangeExpression, SpreadExpression,
    SpreadMapExpression, StaticMethodCallExpression, TernaryExpression, TupleExpression,
    UnaryMinusExpression, UnaryPlusExpression, VariableExpression
]

final STATEMENTS = [
    AssertStatement, BlockStatement, BreakStatement, BytecodeSequence, CaseStatement,
    CatchStatement, ContinueStatement, DoWhileStatement, EmptyStatement, ExpressionStatement,
    ForStatement, IfStatement, ReturnStatement, SwitchStatement, SynchronizedStatement,
    ThrowStatement, TryCatchStatement, WhileStatement
]

def menu = {
    menu 'Customizers', {
        menu 'AST Transformation', {
            TRANSFORMATIONS.each { entry ->
                checkBoxMenuItem entry.key, itemStateChanged: { e ->
                    if (e.source.selected) {
                        console.config.addCompilationCustomizers entry.value
                    } else {
                        console.config.compilationCustomizers.remove entry.value
                    }
                }
            }
        }
        def securePanel = panel {
            def listPanel = { title ->
                def propertyPrefix = title.split(' ')*.capitalize().join().replaceAll('^.', { it.toLowerCase() })
                panel(name:title) {
                    vbox {
                        hbox {
                            def bg = buttonGroup()
                            radioButton 'White', buttonGroup:bg, selected:bind(target:secureModel, "${propertyPrefix}White")
                            radioButton 'Black', buttonGroup:bg
                        }
                        scrollPane {
                            textArea(rows:20, columns:30, text:bind(target:secureModel, "${propertyPrefix}List", converter: {
                                it ? it.split('\n') : []
                            }))
                        }
                    }
                }
            }
            def checkBoxPanel = { sources, title ->
                panel(name:title) {
                    panel {
                        gridBagLayout()
                        if (sources in List) {
                            sources.eachWithIndex { type, i ->
                                checkBox type.simpleName, constraints:gbc(gridx:i%2, gridy:i/2, anchor:NORTHWEST)
                            }
/*                            sources.size().times { i ->
                                checkBox sources[i].simpleName, constraints:gbc(gridx:i%2, gridy:i/2, anchor:NORTHWEST)
                            }
*/                        } else {
                            sources.entrySet().eachWithIndex { entry, i ->
                                checkBox entry.key, constraints:gbc(gridx:i%2, gridy:i/2, anchor:NORTHWEST)
                            }
                        }
                    }
                }
            }
            vbox {
                tabbedPane {
                    panel(name:'Allow') {
                        vbox {
                            checkBox 'Closures', selected:bind(target:secureModel, 'closuresAllowed')
                            checkBox 'Method definition', selected:bind(target:secureModel, 'methodDefinitionAllowed')
                            checkBox 'Package', selected:bind(target:secureModel, 'packageAllowed')
                            checkBox 'Enable indirect import check', selected:bind(target:secureModel, 'indirectImportCheckEnabled')
                        }
                    }
                    checkBoxPanel EXPRESSIONS, 'Expression'
                    checkBoxPanel STATEMENTS, 'Statement'
                    checkBoxPanel SecureModel.TOKENS, 'Token'
                    listPanel 'Imports'
                    listPanel 'Star imports'
                    listPanel 'Static imports'
                    listPanel 'Static star imports'
                    listPanel 'Tokens'
                    listPanel 'Constant types classes'
                    listPanel 'Receivers classes'
                }
            }
        }
        menuItem 'Secure AST', actionPerformed: {
            optionPane(message:securePanel, optionType:JOptionPane.DEFAULT_OPTION).with {
                createDialog('Secure AST').show()
            }
        }
    }
}

console.run(
    Console.frameConsoleDelegates << [menuBarDelegate: {arg->
        current.JMenuBar = build(arg)
        current.JMenuBar.add(build(menu))
    }
])
