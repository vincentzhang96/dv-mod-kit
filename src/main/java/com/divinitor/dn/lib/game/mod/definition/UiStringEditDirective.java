package com.divinitor.dn.lib.game.mod.definition;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class UiStringEditDirective {

    private Map<String, String> add;
    private List<UiStringEdit> edit;

    @Getter
    @Setter
    public static class UiStringEdit {
        private String mid;
        private String match;
        private String value;
    }
}
