package org.k8sclient.crdtester.services;

import org.springframework.stereotype.Service;

@Service
public class ShellCommandService {

    public int executeShellCommand(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command.split(" "));

        Process process = pb.inheritIO().start();
        int exitCode = process.waitFor();
        if(exitCode != 0){
            throw new Exception("Failure on running: "+command);
        }
        return exitCode;
    }
}
