package com.divinitor.dn.lib.game.mod.compiler;

import com.google.common.collect.Multimap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class BuildComputeResults {

    /**
     * Resource conflicts. Conflicts occur when two mods attempt to override the same resource, resulting in only
     * one mod's files being used.
     */
    protected List<BuildConflict> conflicts;

    /**
     * Resources that could not be found. Mapping from ModPackage ID to one or more missing resources.
     */
    protected Multimap<String, String> missing;

    /**
     * Resources that cannot have the given transformation applied to them.
     */
    protected Multimap<String, BuildReject> rejected;

    @Getter(value = AccessLevel.PROTECTED)
    protected List<ModCompiler.FileBuildStep> steps;

    public boolean isOk() {
        return !(hasConflictingResources() || hasMissingResources() || hasRejectedTransforms());
    }

    public boolean hasConflictingResources() {
        return !conflicts.isEmpty();
    }

    public boolean hasMissingResources() {
        return !missing.isEmpty();
    }

    public boolean hasRejectedTransforms() {
        return !rejected.isEmpty();
    }

    public String report() {
        if (isOk()) {
            return "[OK] No issues.";
        }

        StringBuilder builder = new StringBuilder();

        if (hasConflictingResources()) {
            builder.append(this.reportConflicts()).append("\n");
        }

        if (hasMissingResources()) {
            builder.append(this.reportMissing());
        }

        return builder.toString();
    }

    public String reportConflicts() {

        if (hasConflictingResources()) {
            StringBuilder builder = new StringBuilder();
            conflicts.forEach(c -> {
                builder.append("[WARN] File \"")
                    .append(c.getFile())
                    .append("\" is modified by multiple mods: ")
                    .append(c.conflictingModIds.stream().collect(Collectors.joining(", ")))
                    .append("\n\t")
                    .append("Only the changes made by the first mod (")
                    .append(c.getConflictingModIds().stream().findFirst().orElse("???"))
                    .append(") will apply.")
                    .append("\n");
            });

            return builder.toString();
        } else {
            return "[OK] No conflicts.";
        }
    }

    public String reportMissing() {

        if (hasMissingResources()) {
            StringBuilder builder = new StringBuilder();
            missing.asMap().forEach((k, v) -> {
                builder.append("[WARN] Mod \"")
                    .append(k)
                    .append("\" is missing the following files: ")
                    .append(v.stream().collect(Collectors.joining(", ")))
                    .append("\n\t")
                    .append("Please double check that all paths and names are correct. ")
                    .append("Some mod features may be missing or not work as expected.")
                    .append("\n");
            });

            return builder.toString();
        } else {
            return "[OK] No missing resources.";
        }
    }


    public String reportRejected() {

        if (hasMissingResources()) {
            StringBuilder builder = new StringBuilder();
            rejected .asMap().forEach((k, v) -> {
                builder.append("[ERROR] Mod \"")
                    .append(k)
                    .append("\" has the following rejected transformations: ")
                    .append(v.stream().map(r -> String.format("%s: %s", r.getPath(), r.getReason()))
                        .collect(Collectors.joining(", ")))
                    .append("\n\t")
                    .append("Please remove the offending transformations.")
                    .append("\n");
            });

            return builder.toString();
        } else {
            return "[OK] No rejected resources.";
        }
    }

    /**
     * Returns the results as a string by calling {@link BuildComputeResults#report()}.
     * @return A string representation of this object.
     */
    @Override
    public String toString() {
        return report();
    }
}
