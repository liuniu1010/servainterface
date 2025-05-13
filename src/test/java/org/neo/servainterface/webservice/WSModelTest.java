package org.neo.servainterface.webservice;

import java.util.*;
import java.sql.SQLException;

import okhttp3.*;
import java.io.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.neo.servaframe.util.*;
import org.neo.servaframe.interfaces.*;
import org.neo.servaframe.model.*;

import org.neo.servainterface.webservice.*;

/**
 * Unit test 
 */
public class WSModelTest 
    extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public WSModelTest( String testName ) {
        super( testName );
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Code to set up resources or initialize variables before each test method
    }

    @Override
    protected void tearDown() throws Exception {
        // Code to clean up resources after each test method
        super.tearDown();
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite( WSModelTest.class );
    }

    public void testJsonConvert() {
        WSModel.AIGameFactoryParams params = new WSModel.AIGameFactoryParams();
        params.setPrompt("the requirement");
        params.setCode("the code");

        String json = params.toJson();
        System.out.println("json = \n" + json);

        WSModel.AIGameFactoryParams params2 = WSModel.AIGameFactoryParams.fromJson(json);

        assertEquals(params, params2);
    }

}

