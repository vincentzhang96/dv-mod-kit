package com.divinitor.dn.lib.game.mod.compiler.processors;

public class Processors {

    public static Processor getProcessor(String processor) {
        switch (processor.toLowerCase()) {
            case "skn":
                return new SknProcessor();
            case "actkit":
                return new ActKitProcessor();
            default:
                throw new UnsupportedOperationException("Unsupported processor " + processor);
        }
    }

}
