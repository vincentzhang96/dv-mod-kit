package com.divinitor.dn.lib.game.mod.compiler.processors.models;

import co.phoenixlab.dn.subfile.stage.trigger.*;
import co.phoenixlab.dn.subfile.stage.triggerdefine.TriggerDefine;
import co.phoenixlab.dn.subfile.stage.triggerdefine.TriggerDefineEntry;
import co.phoenixlab.dn.subfile.stage.triggerdefine.TriggerDefineWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class StageTriggerModels {
    public static class TriggerScript {
        List<String> variables;
        List<String> eventAreas;
        List<TriggerScriptTrigger> triggers;

        public TriggerScript() {
            this.variables = new ArrayList<>();
            this.eventAreas = new ArrayList<>();
            this.triggers = new ArrayList<>();
        }
    }

    public static class TriggerScriptTrigger {
        String decl;
        String comment;
        List<String> conditions;
        List<String> actions;
        List<String> events;

        public TriggerScriptTrigger() {
            this.conditions = new ArrayList<>();
            this.actions = new ArrayList<>();
            this.events = new ArrayList<>();
        }
    }

    public static class EventAreaDefine {
        String name;
        int id;

        private static EventAreaDefine valueOf(String s) {
            EventAreaDefine ret = new EventAreaDefine();
            if (!s.startsWith("area ")) {
                throw new IllegalArgumentException("Not an area");
            }

            s = s.substring("area ".length());
            String[] split = s.split(" ");
            if (split.length != 2) {
                throw new IllegalArgumentException("Invalid area");
            }

            ret.name = split[0];
            ret.id = Integer.parseInt(split[1]);

            return ret;
        }
    }

    public static byte[] writeTriggers(TriggerScript script, TriggerDefineEntry[] defEntries) {
        StageTriggers triggers = new StageTriggers();
        triggers.setNumTriggers(script.triggers.size());
        Trigger[] triggerEntries = new Trigger[triggers.getNumTriggers()];
        triggers.setTriggers(triggerEntries);
        triggers.setNumTriggers(triggerEntries.length);
        List<TriggerScriptTrigger> triggerScriptTriggers = script.triggers;
        EventAreaDefine[] areas = script.eventAreas.stream().map(EventAreaDefine::valueOf).toArray(EventAreaDefine[]::new);
        for (int i = 0, triggerScriptTriggersSize = triggerScriptTriggers.size(); i < triggerScriptTriggersSize; i++) {
            TriggerScriptTrigger t = triggerScriptTriggers.get(i);
            Trigger trigger = new Trigger();
            trigger.setComment(t.comment);
            trigger.setRepeatable(getTriggerRepeatable(t.decl));
            trigger.setTriggerName(getTriggerName(t.decl));
            trigger.setTriggerParentName(getTriggerParent(t.decl));

            List<TriggerScriptCall> conditions = new ArrayList<>();
            for (int j = 0, len = t.conditions.size(); j < len; j++) {
                String condition = t.conditions.get(j);
                TriggerScriptCall c = parseCall(condition, defEntries, areas, TriggerScriptCall.TriggerScriptCallType.CONDITION);
                if (c != null) {
                    conditions.add(c);
                }
            }

            trigger.setConditionCalls(conditions.toArray(new TriggerScriptCall[0]));

            List<TriggerScriptCall> actions = new ArrayList<>();
            for (int j = 0, len = t.actions.size(); j < len; j++) {
                String action = t.actions.get(j);
                TriggerScriptCall c = parseCall(action, defEntries, areas, TriggerScriptCall.TriggerScriptCallType.ACTION);
                if (c != null) {
                    actions.add(c);
                }
            }

            trigger.setActionCalls(actions.toArray(new TriggerScriptCall[0]));

            List<TriggerScriptCall> events = new ArrayList<>();
            for (int j = 0, len = t.events.size(); j < len; j++) {
                String event = t.events.get(j);
                TriggerScriptCall c = parseCall(event, defEntries, areas, TriggerScriptCall.TriggerScriptCallType.EVENT);
                if (c != null) {
                    events.add(c);
                }
            }

            trigger.setEventCalls(events.toArray(new TriggerScriptCall[0]));

            triggerEntries[i] = trigger;
        }

        StageTriggersWriter writer = new StageTriggersWriter();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(triggers, baos);
        return baos.toByteArray();
    }

    public static  Map.Entry<byte[], TriggerDefine> writeTriggerVariableDefinitions(TriggerScript script) throws IOException {
        TriggerDefine define = new TriggerDefine();
        define.setNumEntries(script.variables.size());
        TriggerDefineEntry[] defineEntries = new TriggerDefineEntry[define.getNumEntries()];
        List<String> variables = script.variables;
        for (int i = 0, variablesSize = variables.size(); i < variablesSize; i++) {
            String variable = variables.get(i);
            defineEntries[i] = defineFromString(variable);
        }

        define.setEntries(defineEntries);

        TriggerDefineWriter writer = new TriggerDefineWriter();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(define, baos);

        return new AbstractMap.SimpleEntry<>(baos.toByteArray(), define);
    }

    static TriggerDefineEntry defineFromString(String s) {
        StringTokenizer tokenizer = new StringTokenizer(s, " ");
        TriggerDefineEntry ret = new TriggerDefineEntry();
        if (tokenizer.hasMoreTokens()) {
            String type = tokenizer.nextToken();
            switch (type) {
                case "int":
                    ret.setParamType(0);
                    break;
                case "float":
                    ret.setParamType(1);
                    break;
                case "string":
                    ret.setParamType(2);
                    break;
                default:
                    throw new IllegalArgumentException(type + " is not a valid variable type");
            }

            if (tokenizer.hasMoreTokens()) {
                String name = unescapeTickedString(tokenizer.nextToken());
                ret.setName(name);
                if (tokenizer.hasMoreTokens()) {
                    int index = Integer.parseInt(tokenizer.nextToken());
                    ret.setIndex(index);
                    if (tokenizer.hasMoreTokens()) {
                        if ("=".equals(tokenizer.nextToken())) {
                            if (tokenizer.hasMoreTokens()) {
                                StringBuilder b = new StringBuilder();
                                while (tokenizer.hasMoreTokens()) {
                                    b.append(tokenizer.nextToken());
                                }

                                String strVal = b.toString();
                                switch (ret.getParamType()) {
                                    case 0:
                                        ret.setIntValue(Integer.parseInt(strVal));
                                        break;
                                    case 1:
                                        ret.setFloatValue(Float.parseFloat(strVal));
                                        break;
                                    case 2: {
                                        ret.setStringValue(unescapeTickedString(strVal));
                                        break;
                                    }
                                }

                                return ret;
                            }
                        }
                    }
                }
            }
        }

        throw new IllegalArgumentException(s + " is not a valid variable declaration");
    }

    static boolean getTriggerRepeatable(String decl) {
        return decl.startsWith("repeatable ");
    }

    static String getTriggerName(String decl) {
        String[] split = decl.split(" ");
        int index = 0;
        if (getTriggerRepeatable(decl)) {
            index = 1;
        }
        return unescapeTickedString(split[index]);
    }

    static String getTriggerParent(String decl) {
        String[] split = decl.split(" ");
        int index = 2;
        if (getTriggerRepeatable(decl)) {
            index = 3;
        }
        return unescapeTickedString(split[index]);
    }

    static TriggerScriptCall parseCall(String call, TriggerDefineEntry[] entries, EventAreaDefine[] areas, TriggerScriptCall.TriggerScriptCallType type) {
        if (call.startsWith("#")) {
            return null;
        }
        TriggerScriptCall ret = new TriggerScriptCall();
        ret.setType(type);
        ret.setScriptType(TriggerScriptCall.TriggerScriptType.SCRIPT_FILE);
        if (call.startsWith("|")) {
            int idxNext = call.indexOf('|', 1);
            if (idxNext == -1) {
                throw new IllegalArgumentException("Invalid call " + call);
            }

            String orIndex = call.substring(1, idxNext);
            if (orIndex.isEmpty()) {
                throw new IllegalArgumentException("Invalid call " + call);
            }

            int opIndex = Integer.parseInt(orIndex);
            ret.setOperatorType(TriggerScriptCall.TriggerOperatorType.OR);
            ret.setOperatorIndex(opIndex);

            call = call.substring(idxNext + 1).trim();
        } else {
            ret.setOperatorType(TriggerScriptCall.TriggerOperatorType.AND);
        }

        if (call.startsWith("[")) {
            int idxNext = call.indexOf(']');
            if (idxNext == -1) {
                throw new IllegalArgumentException("Invalid call " + call);
            }

            String arr = call.substring(1, idxNext);
            String[] split = arr.split(",");
            int[] value = new int[split.length];
            if (value.length != TriggerScriptCall.OPERATOR_VALUE_SZ) {
                throw new IllegalArgumentException("Invalid call " + call);
            }
            for (int i = 0; i < split.length; i++) {
                String s = split[i].trim();
                value[i] = Integer.parseInt(s);
            }

            ret.setOperatorValue(value);
            call = call.substring(idxNext + 1).trim();
        }

        int openParen = call.indexOf('(');
        if (openParen == -1) {
            throw new IllegalArgumentException("Invalid call " + call);
        }

        String luaName = call.substring(0, openParen) + ".lua";
        ret.setScriptName(luaName);
        String params = call.substring(openParen + 1, call.length() - 1);
        List<TriggerCallParameter> callParameters = new ArrayList<>();
        if (!params.trim().isEmpty()) {
            // Assume there's no commas in text (lol)
            String[] split = params.split(",");
            for (int i = 0; i < split.length; i++) {
                String param = split[i].trim();
                TriggerCallParameter callParam = parseTriggerCallParameter(entries, param);
                callParameters.add(callParam);
            }
        }

        ret.setParams(callParameters.toArray(new TriggerCallParameter[0]));
        ret.setNumParams(callParameters.size());

        return ret;
    }

    static TriggerCallParameter parseTriggerCallParameter(TriggerDefineEntry[] entries, String param) {
        TriggerCallParameter callParam = null;
        if (param.matches("[+-]?[0-9]*\\.?[0-9]+f")) {
            String trimmed = param.substring(0, param.length() - 1);
            callParam = new TriggerCallFloatParameter(Float.parseFloat(trimmed));
            callParam.setType(TriggerCallParameter.TriggerCallParameterType.FLOAT);
        } else if (param.startsWith("'")) {
            callParam = new TriggerCallStringParameter(unescapeTickedString(param) + '\u0000');
            callParam.setType(TriggerCallParameter.TriggerCallParameterType.STRING);
        } else if (param.startsWith("z~")) {

        } else if (param.matches("-?[0-9]+[a-z]?")) {
            TriggerCallParameter.TriggerCallParameterType t = TriggerCallParameter.TriggerCallParameterType.INTEGER;
            if (param.endsWith("o")) {
                t = TriggerCallParameter.TriggerCallParameterType.OPERATOR;
            } else if (param.endsWith("p")) {
                t = TriggerCallParameter.TriggerCallParameterType.PROP;
            } else if (param.endsWith("z")) {
                t = TriggerCallParameter.TriggerCallParameterType.EVENT_AREA;
            }

            if (t != TriggerCallParameter.TriggerCallParameterType.INTEGER) {
                param = param.substring(0, param.length() - 1);
            }

            callParam = new TriggerCallIntegerParameter(Integer.parseInt(param));
            callParam.setType(t);
        } else {
            for (TriggerDefineEntry entry : entries) {
                if (entry.getName().equals(param)) {
                    callParam = new TriggerCallIntegerParameter(entry.getIndex());
                    callParam.setType(TriggerCallParameter.TriggerCallParameterType.INDEX);
                    break;
                }
            }

            if (callParam == null) {
                throw new NoSuchElementException("Missing variable " + param);
            }
        }
        return callParam;
    }

    static String unescapeTickedString(String s) {
        String ret = s.substring(1, s.length() - 1);
        ret = ret.replace("\\'", "'").replace("%20", " ");
        return ret;
    }
}
