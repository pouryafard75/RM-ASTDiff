/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.storage;

import com.graphhopper.util.*;
import com.graphhopper.util.shapes.BBox;

import java.io.IOException;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Peter Karich
 */
public class GraphHopperStorageTest extends AbstractGraphStorageTester
{
    @Override
    public GraphHopperStorage createGHStorage( String location, boolean enabled3D )
    {
        // reduce segment size in order to test the case where multiple segments come into the game
        GraphHopperStorage gs = newGHStorage(new RAMDirectory(location), enabled3D);
        gs.setSegmentSize(defaultSize / 2);
        gs.create(defaultSize);
        return gs;
    }

    protected GraphHopperStorage newGHStorage( Directory dir, boolean enabled3D )
    {
        return new GraphHopperStorage(dir, encodingManager, enabled3D);
    }

    @Test
    public void testNoCreateCalled() throws IOException
    {
        GraphHopperStorage gs = new GraphBuilder(encodingManager).build();
        try
        {
            ((BaseGraph) gs.getGraph(Graph.class)).ensureNodeIndex(123);
            assertFalse("AssertionError should be raised", true);
        } catch (AssertionError err)
        {
            assertTrue(true);
        } catch (Exception ex)
        {
            assertFalse("AssertionError should be raised, but was " + ex.toString(), true);
        } finally
        {
            gs.close();
        }
    }

    @Test
    public void testSave_and_fileFormat() throws IOException
    {
        graph = newGHStorage(new RAMDirectory(defaultGraphLoc, true), true).create(defaultSize);
        NodeAccess na = graph.getNodeAccess();
        assertTrue(na.is3D());
        na.setNode(0, 10, 10, 0);
        na.setNode(1, 11, 20, 1);
        na.setNode(2, 12, 12, 0.4);

        EdgeIteratorState iter2 = graph.edge(0, 1, 100, true);
        iter2.setWayGeometry(Helper.createPointList3D(1.5, 1, 0, 2, 3, 0));
        EdgeIteratorState iter1 = graph.edge(0, 2, 200, true);
        iter1.setWayGeometry(Helper.createPointList3D(3.5, 4.5, 0, 5, 6, 0));
        graph.edge(9, 10, 200, true);
        graph.edge(9, 11, 200, true);
        graph.edge(1, 2, 120, false);

        iter1.setName("named street1");
        iter2.setName("named street2");

        checkGraph(graph);
        graph.flush();
        graph.close();

        graph = newGHStorage(new MMapDirectory(defaultGraphLoc), true);
        assertTrue(graph.loadExisting());

        assertEquals(12, graph.getNodes());
        checkGraph(graph);

        assertEquals("named street1", graph.getEdgeProps(iter1.getEdge(), iter1.getAdjNode()).getName());
        assertEquals("named street2", graph.getEdgeProps(iter2.getEdge(), iter2.getAdjNode()).getName());
        graph.edge(3, 4, 123, true).setWayGeometry(Helper.createPointList3D(4.4, 5.5, 0, 6.6, 7.7, 0));
        checkGraph(graph);
    }

    protected void checkGraph( Graph g )
    {
        NodeAccess na = g.getNodeAccess();
        assertTrue(na.is3D());
        assertTrue(g.getBounds().isValid());

        assertEquals(new BBox(10, 20, 10, 12, 0, 1), g.getBounds());
        assertEquals(10, na.getLatitude(0), 1e-2);
        assertEquals(10, na.getLongitude(0), 1e-2);
        EdgeExplorer explorer = g.createEdgeExplorer(carOutFilter);
        assertEquals(2, GHUtility.count(explorer.setBaseNode(0)));
        assertEquals(GHUtility.asSet(2, 1), GHUtility.getNeighbors(explorer.setBaseNode(0)));

        EdgeIterator iter = explorer.setBaseNode(0);
        assertTrue(iter.next());
        assertEquals(Helper.createPointList3D(3.5, 4.5, 0, 5, 6, 0), iter.fetchWayGeometry(0));

        assertTrue(iter.next());
        assertEquals(Helper.createPointList3D(1.5, 1, 0, 2, 3, 0), iter.fetchWayGeometry(0));
        assertEquals(Helper.createPointList3D(10, 10, 0, 1.5, 1, 0, 2, 3, 0), iter.fetchWayGeometry(1));
        assertEquals(Helper.createPointList3D(1.5, 1, 0, 2, 3, 0, 11, 20, 1), iter.fetchWayGeometry(2));

        assertEquals(11, na.getLatitude(1), 1e-2);
        assertEquals(20, na.getLongitude(1), 1e-2);
        assertEquals(2, GHUtility.count(explorer.setBaseNode(1)));
        assertEquals(GHUtility.asSet(2, 0), GHUtility.getNeighbors(explorer.setBaseNode(1)));

        assertEquals(12, na.getLatitude(2), 1e-2);
        assertEquals(12, na.getLongitude(2), 1e-2);
        assertEquals(1, GHUtility.count(explorer.setBaseNode(2)));

        assertEquals(GHUtility.asSet(0), GHUtility.getNeighbors(explorer.setBaseNode(2)));

        EdgeIteratorState eib = GHUtility.getEdge(g, 1, 2);
        assertEquals(Helper.createPointList3D(), eib.fetchWayGeometry(0));
        assertEquals(Helper.createPointList3D(11, 20, 1), eib.fetchWayGeometry(1));
        assertEquals(Helper.createPointList3D(12, 12, 0.4), eib.fetchWayGeometry(2));
        assertEquals(GHUtility.asSet(0), GHUtility.getNeighbors(explorer.setBaseNode(2)));
    }

    @Test
    public void internalDisconnect()
    {
        GraphHopperStorage storage = createGHStorage();
        BaseGraph graph = (BaseGraph) storage.getGraph(Graph.class);
        EdgeIteratorState iter0 = graph.edge(0, 1, 10, true);
        EdgeIteratorState iter2 = graph.edge(1, 2, 10, true);
        EdgeIteratorState iter3 = graph.edge(0, 3, 10, true);

        EdgeExplorer explorer = graph.createEdgeExplorer();

        assertEquals(GHUtility.asSet(3, 1), GHUtility.getNeighbors(explorer.setBaseNode(0)));
        assertEquals(GHUtility.asSet(2, 0), GHUtility.getNeighbors(explorer.setBaseNode(1)));
        // remove edge "1-2" but only from 1 not from 2
        graph.internalEdgeDisconnect(iter2.getEdge(), -1, iter2.getBaseNode(), iter2.getAdjNode());
        assertEquals(GHUtility.asSet(0), GHUtility.getNeighbors(explorer.setBaseNode(1)));
        assertEquals(GHUtility.asSet(1), GHUtility.getNeighbors(explorer.setBaseNode(2)));
        // let 0 unchanged -> no side effects
        assertEquals(GHUtility.asSet(3, 1), GHUtility.getNeighbors(explorer.setBaseNode(0)));

        // remove edge "0-1" but only from 0
        graph.internalEdgeDisconnect(iter0.getEdge(), (long) iter3.getEdge() * graph.edgeEntryBytes, iter0.getBaseNode(), iter0.getAdjNode());
        assertEquals(GHUtility.asSet(3), GHUtility.getNeighbors(explorer.setBaseNode(0)));
        assertEquals(GHUtility.asSet(0), GHUtility.getNeighbors(explorer.setBaseNode(3)));
        assertEquals(GHUtility.asSet(0), GHUtility.getNeighbors(explorer.setBaseNode(1)));
        storage.close();
    }

    @Test
    public void testEnsureSize()
    {
        Directory dir = new RAMDirectory();
        graph = newGHStorage(dir, false).create(defaultSize);
        int testIndex = dir.find("edges").getSegmentSize() * 3;
        graph.edge(0, testIndex, 10, true);

        // test if optimize works without error
        graph.optimize();
    }

    @Test
    public void testBigDataEdge()
    {
        Directory dir = new RAMDirectory();
        GraphHopperStorage graph = new GraphHopperStorage(dir, encodingManager, false);
        graph.create(defaultSize);
        ((BaseGraph) graph.getGraph(Graph.class)).setEdgeCount(Integer.MAX_VALUE / 2);
        assertTrue(graph.getAllEdges().next());
        graph.close();
    }

    @Test
    public void testDoThrowExceptionIfDimDoesNotMatch()
    {
        graph = newGHStorage(new RAMDirectory(defaultGraphLoc, true), false);
        graph.create(1000);
        graph.flush();
        graph.close();

        graph = newGHStorage(new RAMDirectory(defaultGraphLoc, true), true);
        try
        {
            graph.loadExisting();
            assertTrue(false);
        } catch (Exception ex)
        {
        }
    }

    @Test
    public void testIdentical()
    {
        GraphHopperStorage store = new GraphHopperStorage(new RAMDirectory(), encodingManager, true);
        assertEquals(store.getNodes(), store.getGraph(Graph.class).getNodes());
        assertEquals(store.getAllEdges().getCount(), store.getGraph(Graph.class).getAllEdges().getCount());
    }
}
