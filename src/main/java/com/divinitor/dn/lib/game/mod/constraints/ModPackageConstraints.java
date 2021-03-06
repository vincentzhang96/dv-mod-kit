package com.divinitor.dn.lib.game.mod.constraints;

import com.divinitor.dn.lib.game.mod.definition.ModPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ModPackageConstraints {

    private static final Pattern ID_VALIDATOR = Pattern.compile("^[a-z](-?[a-z0-9])*$");

    private ModPackageConstraints() {
    }

    public static void check(ModPackage modPackage) throws ConstraintViolationException {
        Map<String, String> violations = new HashMap<>();
        if (!checkId(modPackage.getId())) {
            violations.put("id", "Invalid ID");
        }

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations.entrySet().stream()
                .map((e) -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", ")));
        }
    }

    public static boolean checkId(String id) {
        return ID_VALIDATOR.asPredicate().test(id);
    }


}
