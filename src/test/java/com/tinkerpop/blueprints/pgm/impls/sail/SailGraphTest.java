package com.tinkerpop.blueprints.pgm.impls.sail;

import com.tinkerpop.blueprints.BaseTest;
import com.tinkerpop.blueprints.pgm.*;
import info.aduna.iteration.CloseableIteration;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.sail.SailException;
import org.openrdf.sail.memory.MemoryStore;

import java.lang.reflect.Method;
import java.util.regex.Matcher;


/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class SailGraphTest extends BaseTest {

    private static final SuiteConfiguration config = new SuiteConfiguration();

    static {
        config.allowsDuplicateEdges = false;
        config.allowsSelfLoops = true;
        config.requiresRDFIds = true;
        config.isRDFModel = true;
        config.supportsVertexIteration = false;
        config.supportsEdgeIteration = true;
        config.supportsVertexIndex = false;
        config.supportsEdgeIndex = false;
        config.ignoresSuppliedIds = false;
    }

    public void testSailGraphFactory() {
        assertTrue(true);
        SailGraphFactory.createTinkerGraph(new MemoryStore());
    }

    public void testTypeConversion() {
        assertEquals(SailVertex.castLiteral(new LiteralImpl("marko", new URIImpl("http://www.w3.org/2001/XMLSchema#string"))).getClass(), String.class);
        assertEquals(SailVertex.castLiteral(new LiteralImpl("marko")).getClass(), String.class);
        assertEquals(SailVertex.castLiteral(new LiteralImpl("27", new URIImpl("http://www.w3.org/2001/XMLSchema#int"))).getClass(), Integer.class);
        assertEquals(SailVertex.castLiteral(new LiteralImpl("27", new URIImpl("http://www.w3.org/2001/XMLSchema#float"))).getClass(), Float.class);
        assertEquals(SailVertex.castLiteral(new LiteralImpl("27.0134", new URIImpl("http://www.w3.org/2001/XMLSchema#double"))).getClass(), Double.class);
        assertEquals(SailVertex.castLiteral(new LiteralImpl("hello", "en")), "hello");
    }

    public void testNamespaceConversion() throws Exception {
        MemoryStore sail = new MemoryStore();
        SailGraph graph = new SailGraph(sail);
        graph.addNamespace("tg", "http://tinkerpop.com#");
        graph.addNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        assertEquals(SailGraph.prefixToNamespace("tg:name", graph.getSailConnection()), "http://tinkerpop.com#name");
        assertEquals(SailGraph.prefixToNamespace("rdf:label", graph.getSailConnection()), "http://www.w3.org/1999/02/22-rdf-syntax-ns#label");
        assertEquals(SailGraph.namespaceToPrefix("http://www.w3.org/1999/02/22-rdf-syntax-ns#label", graph.getSailConnection()), "rdf:label");
        assertEquals(SailGraph.namespaceToPrefix("http://tinkerpop.com#name", graph.getSailConnection()), "tg:name");
        graph.shutdown();

    }

    public void testURIs() {
        assertFalse(SailHelper.isURI("_:1234"));
        assertFalse(SailHelper.isURI("_:abcdefghijklmnopqrstuvwxyz"));
        assertTrue(SailHelper.isURI("http://marko"));
        assertTrue(SailHelper.isURI("http://www.w3.org/2001/XMLSchema#string"));
    }

    public void testBNodes() {
        assertTrue(SailHelper.isBNode("_:1234"));
        assertTrue(SailHelper.isBNode("_:abcdefghijklmnopqrstuvwxyz"));
        assertFalse(SailHelper.isBNode("_:"));
        assertFalse(SailHelper.isBNode("http://marko"));
        assertFalse(SailHelper.isBNode("http://www.w3.org/2001/XMLSchema#string"));
    }

    public void testLiterals() {
        assertTrue(SailHelper.isLiteral("\"java\"^^<http://www.w3.org/2001/XMLSchema#string>"));
        assertFalse(SailHelper.isLiteral("http://www.w3.org/2001/XMLSchema#string"));
        assertFalse(SailHelper.isLiteral("^^<http://www.w3.org/2001/XMLSchema#string>"));
        assertTrue(SailHelper.isLiteral("\"\"^^<http://www.w3.org/2001/XMLSchema#string>"));
        assertTrue(SailHelper.isLiteral("\"\""));
        assertTrue(SailHelper.isLiteral("\"marko\""));
        assertFalse(SailHelper.isLiteral("\"marko\"marko"));
        assertFalse(SailHelper.isLiteral("\""));
        // TODO: make this true assertFalse(SesameGraph.isLiteral("\"marko\"marko\""));


        Matcher matcher = SailHelper.literalPattern.matcher("\"java\"^^<http://www.w3.org/2001/XMLSchema#string>");
        matcher.matches();
        assertNull(matcher.group(6));
        assertEquals(matcher.group(1), "java");
        assertEquals(matcher.group(4), "http://www.w3.org/2001/XMLSchema#string");

        matcher = SailHelper.literalPattern.matcher("\"java\"@en");
        matcher.matches();
        assertNull(matcher.group(4));
        assertEquals(matcher.group(1), "java");
        assertEquals(matcher.group(6), "en");
    }

    public void testLiteralProperties() {
        MemoryStore sail = new MemoryStore();
        SailGraph graph = new SailGraph(sail);
        Vertex v = graph.getVertex("\"java\"^^<http://www.w3.org/2001/XMLSchema#string>");
        assertEquals(v.getProperty(SailTokens.VALUE), "java");
        assertEquals(v.getProperty(SailTokens.DATATYPE), "http://www.w3.org/2001/XMLSchema#string");
        assertNull(v.getProperty(SailTokens.LANGUAGE));
        assertEquals(v.getProperty(SailTokens.KIND), "literal");

        v = graph.getVertex("\"10\"^^<http://www.w3.org/2001/XMLSchema#int>");
        assertEquals(v.getProperty(SailTokens.VALUE), 10);
        assertEquals(v.getProperty(SailTokens.DATATYPE), "http://www.w3.org/2001/XMLSchema#int");
        assertNull(v.getProperty(SailTokens.LANGUAGE));
        assertEquals(v.getProperty(SailTokens.KIND), "literal");

        v = graph.getVertex("\"goodbye\"@en");
        assertEquals(v.getProperty(SailTokens.VALUE), "goodbye");
        assertEquals(v.getProperty(SailTokens.LANGUAGE), "en");
        assertNull(v.getProperty(SailTokens.DATATYPE));
        assertEquals(v.getProperty(SailTokens.KIND), "literal");

    }

    public void testValueKinds() {
        MemoryStore sail = new MemoryStore();
        SailGraph graph = new SailGraph(sail);
        Vertex v = graph.getVertex("\"java\"^^<http://www.w3.org/2001/XMLSchema#string>");
        assertEquals(v.getProperty(SailTokens.KIND), "literal");

        v = graph.getVertex("http://markorodriguez.com");
        assertEquals(v.getProperty(SailTokens.KIND), "uri");

        v = graph.getVertex("_:123");
        assertEquals(v.getProperty(SailTokens.KIND), "bnode");
    }

    //// TEST SUITES

    public void testVertexSuite() throws Exception {
        doSuiteTest(new VertexTestSuite(config));
    }

    public void testEdgeSuite() throws Exception {
        doSuiteTest(new EdgeTestSuite(config));
    }

    public void testGraphSuite() throws Exception {
        doSuiteTest(new GraphTestSuite(config));
    }

    public void testIndexSuite() throws Exception {
        doSuiteTest(new IndexTestSuite(config));
    }

    private void doSuiteTest(final ModelTestSuite suite) throws Exception {
        String doTest = System.getProperty("testSail");
        if (doTest == null || doTest.equals("true")) {
            for (Method method : suite.getClass().getDeclaredMethods()) {
                if (method.getName().startsWith("test")) {
                    System.out.println("Testing " + method.getName() + "...");
                    SailGraph graph = new SailGraph(new MemoryStore());
                    method.invoke(suite, graph);
                    graph.shutdown();
                }
            }
        }
    }

    private static int countStatements(final CloseableIteration<? extends Statement, SailException> itty, final boolean print) throws SailException {
        int counter = 0;
        while (itty.hasNext()) {
            Statement s = itty.next();
            if (print)
                System.out.println(s);
            counter++;
        }
        itty.close();
        return counter;
    }

}
