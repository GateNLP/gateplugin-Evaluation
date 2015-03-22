import gate.*
import gate.plugin.evaluation.tests.*
import static gate.plugin.evaluation.tests.TestUtils.*
import static gate.Utils.*;
import gate.plugin.evaluation.api.*
import gate.plugin.evaluation.resources.*

Gate.init()

def docFromFile(String file) {
  def fm = Factory.newFeatureMap()
  fm.put("sourceUrl",new File(file).toURI().toURL())
  doc = (Document)Factory.createResource("gate.corpora.DocumentImpl", fm)
  return doc
}

println("GATE initialized")


