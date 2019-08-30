package com.divinitor.dn.lib.game.mod.compiler.processors;

public class Processors {

    public static Processor getProcessor(String processor) {
        switch (processor.toLowerCase()) {
            case "skn":
                return new SknProcessor();
            case "actkit":
                return new ActKitProcessor();
            case "uistring":
                return new UiStringProcessor();
            case "stagetrigger":
                return new StageTriggerProcessor();
            case "triggerv2":
                return new StageTriggerV2Processor();
            case "triggerdefinev2":
                return new StageTriggerDefineV2Processor();
            case "propinfo":
                return new PropInfoProcessor();
            case "shader":
                return new ShaderProcessor();
            default:
                throw new UnsupportedOperationException("Unsupported processor " + processor);
        }
    }

}
