package io.crate.udf.plugin;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.ArrayList;
import java.util.Collection;

public class UDFPlugin extends AbstractPlugin {

    private final Settings settings;

    public UDFPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "crate-udf";
    }

    @Override
    public String description() {
        return "adds support for loading user defined functions to crate";
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = new ArrayList<>();
        if (!settings.getAsBoolean("node.client", false)) {
            modules.add(UDFModule.class);
        }
        return modules;
    }
}
