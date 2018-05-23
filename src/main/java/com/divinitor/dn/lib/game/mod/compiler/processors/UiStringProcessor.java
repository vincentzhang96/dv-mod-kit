package com.divinitor.dn.lib.game.mod.compiler.processors;

import com.divinitor.dn.lib.game.mod.ModKit;
import com.divinitor.dn.lib.game.mod.definition.ModPackage;
import com.divinitor.dn.lib.game.mod.definition.UiStringEditDirective;
import com.divinitor.dn.lib.game.mod.util.Utils;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.xml.stream.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class UiStringProcessor implements Processor {
    private Gson gson = new GsonBuilder()
        .create();

    @Override
    public Utils.ThrowingSupplier<byte[]> process(ModPackage modPack, String src) {
        return () -> {
            UiStringEditDirective directive = this.load(modPack, src);
            byte[] uiStr = modPack.getKit().getAssetAccessService().getAsset("uistring.xml");
            return this.patch(directive, uiStr);
        };
    }

    private UiStringEditDirective load(ModPackage modPack, String src) throws IOException {
        byte[] data = modPack.getAsset(src);
        UiStringEditDirective directive = gson.fromJson(new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8),
            UiStringEditDirective.class);
        String prefix = directive.getIncludePrefix();
        if (prefix == null) {
            prefix = "";
        }

        if (directive.getAdd() == null) {
            directive.setAdd(new HashMap<>());
        }

        if (directive.getEdit() == null) {
            directive.setEdit(new ArrayList<>());
        }

        Set<UiStringEditDirective.UiStringEdit> edits = new HashSet<>(directive.getEdit());

        if (directive.getInclude() != null) {
            for (String s : directive.getInclude()) {
                String path = prefix + s;
                UiStringEditDirective subDirective = this.load(modPack, path);

                // Top level directives override lower level ones
                if (subDirective.getAdd() != null) {
                    subDirective.getAdd().forEach(directive.getAdd()::putIfAbsent);
                }

                if (subDirective.getEdit() != null) {
                    // Set<> ensures that if the element already exists then it is not updated
                    edits.addAll(subDirective.getEdit());
                }
            }
        }

        directive.setEdit(new ArrayList<>(edits));

        return directive;
    }

    private byte[] patch(UiStringEditDirective edit, byte[] uiStr) {
        LinkedHashMap<String, String> entries = new LinkedHashMap<>();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(uiStr), "UTF-8");
            String localName;
            String mid = "";
            String content = "";
            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        localName = reader.getLocalName();
                        if ("message".equalsIgnoreCase(localName)) {
                            mid = reader.getAttributeValue(0);
                        }
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        content = reader.getText();
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        localName = reader.getLocalName();
                        if ("message".equalsIgnoreCase(localName)) {
                            entries.put(mid, content);
                        }
                        break;
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        //  Apply changes
        //  TODO MID collisions for adds? or do we consider this a forced update
        if (edit.getAdd() != null) {
            entries.putAll(edit.getAdd());
        }

        if (edit.getEdit() != null) {
            for (UiStringEditDirective.UiStringEdit uiStringEdit : edit.getEdit()) {
                String match = uiStringEdit.getMatch();
                if (!Strings.isNullOrEmpty(match)) {
                    //  Verify that it matches, otherwise don't modify
                    String originalValue = entries.get(uiStringEdit.getMid());
                    // BYTE COMPARE
                    byte[] matchBytes = match.getBytes(StandardCharsets.UTF_8);
                    byte[] originalBytes = originalValue.getBytes(StandardCharsets.UTF_8);
                    if (!Arrays.equals(matchBytes, originalBytes)) {
                        ModKit.LOGGER.warn("MID {} does not match: Got \"{}\", expected \"{}\". Skipping.",
                            uiStringEdit.getMid(), new String(originalBytes, StandardCharsets.UTF_8), new String(matchBytes, StandardCharsets.UTF_8));
                        continue;
                    }
                }

                if (entries.containsKey(uiStringEdit.getMid())) {
                    entries.put(uiStringEdit.getMid(), uiStringEdit.getValue());
                } else {
                    ModKit.LOGGER.warn("MID {} is not in the original uistring.xml. Skipping.",
                        uiStringEdit.getMid());
                }
            }
        }

        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            XMLStreamWriter writer = outputFactory.createXMLStreamWriter(out, "UTF-8");
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeCharacters("\n");
            writer.writeStartElement("messages");
            writer.writeAttribute("name", "UIString");
            writer.writeAttribute("lang", "US_FIRST");
            writer.writeCharacters("\n");
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                writer.writeStartElement("message");
                writer.writeAttribute("mid", entry.getKey());
                writer.writeCData(entry.getValue());
                writer.writeEndElement();
                writer.writeCharacters("\n");
            }

            writer.writeEndElement();
            writer.flush();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        return out.toByteArray();
    }
}
