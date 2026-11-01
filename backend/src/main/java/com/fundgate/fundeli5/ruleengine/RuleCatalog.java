package com.fundgate.fundeli5.ruleengine;

import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.ruleengine.plugin.RuleKindPlugin;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The plug-ins the engine knows, in the order their outcomes are shown. */
public final class RuleCatalog {

    private final List<RuleKindPlugin<?>> plugins;

    public RuleCatalog(List<RuleKindPlugin<?>> plugins) {
        Set<Class<?>> seen = new HashSet<>();
        for (RuleKindPlugin<?> plugin : plugins) {
            if (!seen.add(plugin.conditionType())) {
                throw new IllegalArgumentException("Two plug-ins for " + plugin.conditionType().getSimpleName());
            }
        }
        this.plugins = List.copyOf(plugins);
    }

    public List<RuleKindPlugin<?>> plugins() {
        return plugins;
    }

    public Optional<RuleKindPlugin<?>> pluginFor(Condition condition) {
        return plugins.stream().filter(p -> p.conditionType().isInstance(condition)).findFirst();
    }

    /** One line per plug-in: the pipeline assembles its drafting prompt from these. */
    public List<String> draftingLines() {
        return plugins.stream().map(RuleKindPlugin::draftingLine).toList();
    }
}
