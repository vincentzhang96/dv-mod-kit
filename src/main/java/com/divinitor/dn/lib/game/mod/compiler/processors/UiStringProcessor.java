package com.divinitor.dn.lib.game.mod.compiler.processors;

import com.divinitor.dn.lib.game.mod.definition.ModPackage;
import com.divinitor.dn.lib.game.mod.definition.UiStringEditDirective;
import com.divinitor.dn.lib.game.mod.util.Utils;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.xml.stream.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class UiStringProcessor implements Processor {
    @Override
    public Utils.ThrowingSupplier<byte[]> process(ModPackage modPack, String src) {
        return () -> {
            Gson gson = new GsonBuilder()
                .create();
            byte[] data = modPack.getAsset(src);
            UiStringEditDirective directive = gson.fromJson(new InputStreamReader(new ByteArrayInputStream(data)),
                UiStringEditDirective.class);
            byte[] uiStr = modPack.getKit().getAssetAccessService().getAsset("uistring.xml");
            return this.patch(directive, uiStr);
        };
    }

    private byte[] patch(UiStringEditDirective edit, byte[] uiStr) {
        LinkedHashMap<String, String> entries = new LinkedHashMap<>();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(uiStr));
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
        entries.putAll(edit.getAdd());
        for (UiStringEditDirective.UiStringEdit uiStringEdit : edit.getEdit()) {
            String match = uiStringEdit.getMatch();
            if (!Strings.isNullOrEmpty(match)) {
                //  Verify that it matches, otherwise don't modify
                if (!match.equals(entries.get(uiStringEdit.getMid()))) {
                    continue;
                }
            }

            if (entries.containsKey(uiStringEdit.getMid())) {
                entries.put(uiStringEdit.getMid(), uiStringEdit.getValue());
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
