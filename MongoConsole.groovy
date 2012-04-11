@Grab('com.gmongo:gmongo:0.9.5')
@GrabConfig(systemClassLoader=true)
import com.gmongo.GMongo
import com.mongodb.*
import javax.swing.UIManager
import groovy.ui.Console
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import groovy.transform.*

def templates = [
'group': '''\
    |def dbName = 'test'
    |def colName = 'test'
    |def key = [:]
    |def cond = [:]
    |def initial = [:]
    |def reduce = \'''
    |function(obj, prev) {
    |
    |}\'''
    |def db = mongo.getDB(dbName)
    |db[colName].group(key, cond, initial, reduce)'''.stripMargin(),
'mapReduce': '''\
    |import static com.mongodb.MapReduceCommand.OutputType.*
    |
    |def dbName = 'test'
    |def colName = 'test'
    |def map = \'''
    |function() {
    |    emit(this._id, 1)
    |}\'''
    |def reduce = \'''
    |function(key, vals) {
    |    print('key: ' + key + ', vals: ' + vals)
    |    var sum = 0
    |    vals.forEach(function() {
    |       sum += val
    |    })
    |    return sum
    |}\'''
    |def outputTarget = 'my_output'
    |def outputType = REPLACE
    |def query = [:]
    |def db = mongo.getDB(dbName)
    |db[colName].mapReduce(map, reduce, outputTarget, outputType, query)
    |db[outputTarget].find()'''.stripMargin()
]

def mongoMenu = {
    menu('Mongo') {
        menu('Templates') {
            templates.each { name, template ->
                menuItem name, actionPerformed: { inputEditor.textEditor.text = template }
            }
            def templateDir = new File(System.getProperty('user.home')+'/.mongo')
            if (templateDir.exists()) {
                separator()
                templateDir.eachFileMatch(groovy.io.FileType.FILES, ~/.*\.groovy/) { file ->
                    menuItem file.name - ~/\.groovy$/, actionPerformed: { inputEditor.textEditor.text = file.text }
                }
            }
        }
    }
}

Console.metaClass.newScript = { ClassLoader parent, Binding binding ->
    def config = new CompilerConfiguration()
    def importCustomizer = new ImportCustomizer()
    importCustomizer.addImports('com.gmongo.GMongo')
    config.addCompilationCustomizers(importCustomizer)
    binding.mongo = new GMongo()
    delegate.shell = new GroovyShell(parent, binding, config)
}

UIManager.lookAndFeel = UIManager.systemLookAndFeelClassName
new Console(Console.class.classLoader.getRootLoader()).run(
    Console.frameConsoleDelegates << [menuBarDelegate: {arg->
        current.JMenuBar = build(arg)
        current.JMenuBar.add(build(mongoMenu))
    }]
)
