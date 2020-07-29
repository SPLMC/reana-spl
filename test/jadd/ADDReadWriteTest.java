package jadd;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ADDReadWriteTest {

    JADD jadd;
    private static final String DIR_PREFIX = "/tmp/";
    private static final int N = 1000;

    @Before
    public void setUp() throws Exception {
        jadd = new JADD();
    }

    @Test
    public void testADDConstantWriteRead() {
        ADD add = jadd.makeConstant(1.0);
        String filename = DIR_PREFIX + "const1.add";

        jadd.dumpADD(add, filename);

        ADD addRead = jadd.readADD(filename);

        Assert.assertEquals(add, addRead);
    }

    @Test
    public void testADDConstantArrayWriteRead() {
        int n = 10;
        String name = "adds-" + n;
        String filename = DIR_PREFIX + name + ".add";
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

    @Test
    public void testFunctionADDWriteRead() {
        Random rand = new Random();
        ADD a = jadd.makeConstant(rand.nextDouble());
        ADD b = jadd.makeConstant(rand.nextDouble());
        ADD x1 = jadd.getVariable("x1");
        ADD f = x1.ifThenElse(
                jadd.makeConstant(rand.nextDouble()),
                jadd.makeConstant(rand.nextDouble()))
                .times(a)
                .plus(b);
        String filename = DIR_PREFIX + "f.add";
        jadd.dumpADD(f, filename);
        
        ADD fRead = jadd.readADD(filename);
        Assert.assertEquals(f, fRead);
    }
    
    @Test
    public void testLargeADDWriteRead() {
       ADD[] functions = new ADD[N]; 
       ADD[] variables = new ADD[N];
       
       ADD t = jadd.makeConstant(1.0);
       ADD e = jadd.makeConstant(0.5);
       
       variables[0] = jadd.getVariable("v0");
       functions[0] = variables[0].ifThenElse(t, e);
       
       for (int i = 1; i < N; i++) {
          variables[i] = jadd.getVariable("v" + i)
                             .ifThenElse(functions[i-1], t);
          functions[i] = functions[i-1].times(variables[i]);
       }
      
       String filename = DIR_PREFIX + "add-" + N + ".add";
       jadd.dumpADD(functions[N-1], filename);
       
       ADD functionRead = jadd.readADD(filename);
       
       Assert.assertEquals(functions[N-1], functionRead);
    }
}
