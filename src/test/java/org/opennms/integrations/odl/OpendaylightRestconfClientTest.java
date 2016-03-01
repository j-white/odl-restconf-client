/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.integrations.odl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opennms.integrations.odl.OpendaylightRestconfClient;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class OpendaylightRestconfClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig()
            .withRootDirectory(Paths.get("src", "test", "resources").toString())
            .dynamicPort());

    @Test
    public void canGetOperationalNetworkTopology() throws Exception {
        stubFor(get(urlEqualTo("/restconf/operational/network-topology:network-topology/"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "Content-Type: application/yang.data+json; charset=utf-8")
                    .withBodyFile("operational-network-topology.json")));

        // Make the call
        OpendaylightRestconfClient client = new OpendaylightRestconfClient("localhost", wireMockRule.port());
        NetworkTopology networkTopology = client.getOperationalNetworkTopology();

        // Verify
        List<Topology> topologies = networkTopology.getTopology();
        assertEquals(1, topologies.size());
        Topology topology = topologies.get(0);
        assertEquals("flow:1", topology.getTopologyId().getValue());
        List<Node> nodes = topology.getNode();
        assertEquals(15, nodes.size());
        Set<String> nodeIds = nodes.stream()
                .map(n -> n.getNodeId().getValue())
                .collect(Collectors.toSet());
        assertEquals(15, nodeIds.size());
        assertTrue(nodeIds.contains("host:00:00:00:00:00:08"));
        assertTrue(nodeIds.contains("openflow:6"));
    }

    @Test
    public void canGetOperationalTopology() throws Exception {
        stubFor(get(urlEqualTo("/restconf/operational/network-topology:network-topology/topology/flow:1"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "Content-Type: application/yang.data+json; charset=utf-8")
                    .withBodyFile("operational-topology.json")));

        // Make the call
        OpendaylightRestconfClient client = new OpendaylightRestconfClient("localhost", wireMockRule.port());
        Topology topology = client.getOperationalTopology("flow:1");

        // Verify
        assertEquals("flow:1", topology.getTopologyId().getValue());
    }

    @Test
    public void canGetNodeFromOperationalTopology() throws Exception {
        stubFor(get(urlEqualTo("/restconf/operational/network-topology:network-topology/topology/flow:1/node/openflow:1"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "Content-Type: application/yang.data+json; charset=utf-8")
                    .withBodyFile("operational-topology-node.json")));

        // Make the call
        OpendaylightRestconfClient client = new OpendaylightRestconfClient("localhost", wireMockRule.port());
        Node node = client.getNodeFromOperationalTopology("flow:1", "openflow:1");

        // Verify
        assertEquals("openflow:1", node.getNodeId().getValue());
    }
}
