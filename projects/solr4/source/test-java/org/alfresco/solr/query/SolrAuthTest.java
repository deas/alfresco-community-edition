/*
 * #%L
 * Alfresco Data model classes
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.alfresco.solr.query;

import org.alfresco.solr.AlfrescoSolrTestCaseJ4;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.common.params.ModifiableSolrParams;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class SolrAuthTest extends AlfrescoSolrTestCaseJ4 {

    @BeforeClass
    public static void beforeClass() throws Exception {
        initAlfrescoCore("solrconfig-afts.xml", "schema-afts.xml");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        // if you override setUp or tearDown, you better call
        // the super classes version
        super.setUp();
        clearIndex();
        assertU(commit());
    }

    @Test
    public void testAuth() throws Exception {

        assertU(delQ("*:*"));
        assertU(commit());

        String[] acldoc = {"id", "100",  "READER",  "GROUP_R1", "READER", "GROUP_R2", "ACLID", "5000"};
        assertU(adoc(acldoc));
        assertU(commit());

        String[] acldoc1 = {"id", "101",  "READER", "GROUP_R3", "READER", "GROUP_R4", "ACLID", "6000", "DENIED", "GROUP_D1"};
        assertU(adoc(acldoc1));
        assertU(commit());

        String[] acldoc2 = {"id", "102",  "READER", "GROUP_R3", "READER", "GROUP_R2", "ACLID", "7000"};
        assertU(adoc(acldoc2));
        assertU(commit());

        String[] acldoc3 = {"id", "103",  "READER", "GROUP_R3", "READER", "GROUP_R5", "ACLID", "8000"};
        assertU(adoc(acldoc3));
        assertU(commit());

        String[] acldoc4 = {"id", "104",  "READER", "GROUP_R5", "READER", "GROUP_R1", "ACLID", "9000"};
        assertU(adoc(acldoc4));
        assertU(commit());

        //Index Main Documents
        String[] doc = {"id", "1",  "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY", "ACLID", "5000", "OWNER", "jim"};
        assertU(adoc(doc));
        assertU(commit());
        String[] doc1 = {"id", "2", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY", "ACLID", "6000", "OWNER", "dave"};
        assertU(adoc(doc1));

        String[] doc2 = {"id", "3", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY", "ACLID", "7000", "OWNER", "mary"};
        assertU(adoc(doc2));
        assertU(commit());
        String[] doc3 = {"id", "4", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY", "ACLID", "8000", "OWNER", "bill"};
        assertU(adoc(doc3));

        String[] doc4 = {"id", "5", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY", "ACLID", "9000", "OWNER", "steve"};
        assertU(adoc(doc4));
        assertU(commit());
        String[] doc5 = {"id", "6", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY", "ACLID", "10000", "OWNER", "sara"};
        assertU(adoc(doc5));
        assertU(commit());

        Thread.sleep(20000);
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("q", "t1:YYYY");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq", "{!afts}AUTHORITY_FILTER_FROM_JSON");
        SolrServletRequest req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"GROUP_R2\",\"GROUP_R4\" ], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=3]",
                "//result/doc[1]/str[@name='id'][.='1']",
                "//result/doc[2]/str[@name='id'][.='2']",
                "//result/doc[3]/str[@name='id'][.='3']");

        //Turning off the postfilter
        System.setProperty("alfresco.postfilter", "false");

        params = new ModifiableSolrParams();
        params.add("q", "t1:YYYY");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq","{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"GROUP_R2\",\"GROUP_R4\" ], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=3]",
                "//result/doc[1]/str[@name='id'][.='1']",
                "//result/doc[2]/str[@name='id'][.='2']",
                "//result/doc[3]/str[@name='id'][.='3']");

        //Test with owner
        System.setProperty("alfresco.postfilter", "true");

        params = new ModifiableSolrParams();
        params.add("q", "t1:YYYY");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq","{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"GROUP_R2\",\"GROUP_R4\", \"steve\" ], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=4]",
                "//result/doc[1]/str[@name='id'][.='1']",
                "//result/doc[2]/str[@name='id'][.='2']",
                "//result/doc[3]/str[@name='id'][.='3']",
                "//result/doc[4]/str[@name='id'][.='5']");

        //Test with owner
        System.setProperty("alfresco.postfilter", "false");

        params = new ModifiableSolrParams();
        params.add("q", "t1:YYYY");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq","{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"GROUP_R2\",\"GROUP_R4\", \"steve\" ], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=4]",
                "//result/doc[1]/str[@name='id'][.='1']",
                "//result/doc[2]/str[@name='id'][.='2']",
                "//result/doc[3]/str[@name='id'][.='3']",
                "//result/doc[4]/str[@name='id'][.='5']");



        // Test Deny
        System.setProperty("alfresco.postfilter", "true");

        params = new ModifiableSolrParams();
        params.add("q", "t1:YYYY");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq","{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"GROUP_R2\",\"GROUP_R4\", \"GROUP_D1\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=2]",
                "//result/doc[1]/str[@name='id'][.='1']",
                "//result/doc[2]/str[@name='id'][.='3']");


        System.setProperty("alfresco.postfilter", "false");

        params = new ModifiableSolrParams();
        params.add("q", "t1:YYYY");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq","{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"GROUP_R2\",\"GROUP_R4\", \"GROUP_D1\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=2]",
                "//result/doc[1]/str[@name='id'][.='1']",
                "//result/doc[2]/str[@name='id'][.='3']");

        //Test global read authority
        System.setProperty("alfresco.postfilter", "true");
        params = new ModifiableSolrParams();
        params.add("q", "t1:YYYY");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq","{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"ROLE_ADMINISTRATOR\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=6]");

        System.setProperty("alfresco.postfilter", "false");
        params = new ModifiableSolrParams();
        params.add("q", "t1:YYYY");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq","{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"ROLE_ADMINISTRATOR\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=6]");


        //Test zero hits
        System.setProperty("alfresco.postfilter", "true");
        params = new ModifiableSolrParams();
        params.add("q", "t1:YYYY");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq","{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"blah\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=0]");

        System.setProperty("alfresco.postfilter", "false");
        params = new ModifiableSolrParams();
        params.add("q", "t1:YYYY");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq","{!afts}AUTHORITY_FILTER_FROM_JSON");
        req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [\"blah\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=0]");
    }
}
