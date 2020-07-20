package jadd;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ADDReadWriteTest {

    JADD jadd;
    
    @Before
    public void setUp() throws Exception {
        jadd = new JADD();
    }

    @Test
    public void testADDConstantWriteRead() {
        ADD add = jadd.makeConstant(1.0);
        String filename = "const1.add";
        
        jadd.dumpADD(add, filename);
        
        ADD addRead = jadd.readADD(filename);
        
        Assert.assertEquals(add, addRead);
    }
    
    @Test
    public void testADDConstantArrayWriteRead() {
        int n = 10;
        String name = "adds-" + n;
        String filename = name + ".add";
        String functionNames[] = new String[n];
        ADD adds[] = new ADD[n];

        for (int i = 0; i < n; i++) {
            adds[i] = jadd.makeConstant(i);
            functionNames[i] = "add-" + i;
        }
        
        jadd.dumpADD(name, functionNames, adds, filename);
        
        ADD addsRead[] = jadd.readADDs(filename);
        
        Assert.assertArrayEquals(adds, addsRead);
    }
}
