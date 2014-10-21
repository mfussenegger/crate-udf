package io.crate.udf.plugin;

import io.crate.metadata.DynamicFunctionResolver;
import io.crate.metadata.FunctionIdent;
import io.crate.metadata.FunctionImplementation;
import io.crate.udf.UDFService;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.common.settings.Settings;

public class UDFModule extends AbstractModule {

    private MapBinder<FunctionIdent, FunctionImplementation> functionBinder;
    private MapBinder<String, DynamicFunctionResolver> resolverBinder;
    private Settings settings;

    public UDFModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        functionBinder = MapBinder.newMapBinder(binder(), FunctionIdent.class, FunctionImplementation.class);
        resolverBinder = MapBinder.newMapBinder(binder(), String.class, DynamicFunctionResolver.class);

        UDFService udfService = new UDFService(settings, functionBinder, resolverBinder);
        bind(UDFService.class).toInstance(udfService);
    }
}
