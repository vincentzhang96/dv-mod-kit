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
    private String includePrefix;
    private List<String> include;

    @Getter
    @Setter
    public static class UiStringEdit {
        private String mid;
        private String match;
        private String value;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            UiStringEdit that = (UiStringEdit) o;

            return mid != null ? mid.equals(that.mid) : that.mid == null;
        }

        @Override
        public int hashCode() {
            return mid != null ? mid.hashCode() : 0;
        }
    }
}
