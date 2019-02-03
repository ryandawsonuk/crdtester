package org.k8sclient.crdtester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.k8sclient.crdtester.services.ShellCommandService;

public class ShellCommandServiceTest {

    @Test
    public void successfulCommand() throws Exception {
        //date should work on unix or windows
       assertEquals(0,new ShellCommandService().executeShellCommand("date"));
    }

    @Test
    public void failingCommand() throws Exception {
        //date should work on unix or windows
        try{
            new ShellCommandService().executeShellCommand("notacommand");
            fail();
        }catch (Exception ex){
            //do nothing - expected exception
        }
    }

}
