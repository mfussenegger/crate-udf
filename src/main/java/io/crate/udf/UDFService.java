package io.crate.udf;

import com.google.common.io.Files;
import io.crate.metadata.DynamicFunctionResolver;
import io.crate.metadata.FunctionIdent;
import io.crate.metadata.FunctionImplementation;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.python.core.PyJavaType;
import org.python.core.PyObject;
import org.python.core.PyType;

import javax.script.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UDFService {

    private final ESLogger logger = Loggers.getLogger(getClass());
    private final Settings settings;
    private final MapBinder<FunctionIdent, FunctionImplementation> functionBinder;
    private final MapBinder<String, DynamicFunctionResolver> resolverBinder;
    private final ScriptEngineManager scriptEngineManager;
    private final List<UserDefinedScalarFunction<?, ?>> scalars = new ArrayList<>();
    private final List<UserDefinedAggregationFunction<?>> aggregations = new ArrayList<>();

    public UDFService(Settings settings,
                      MapBinder<FunctionIdent, FunctionImplementation> functionBinder,
                      MapBinder<String, DynamicFunctionResolver> resolverBinder) {

        this.settings = settings;
        this.functionBinder = functionBinder;
        this.resolverBinder = resolverBinder;
        scriptEngineManager = new ScriptEngineManager();
        loadPlugins();
    }

    private void loadPlugins() {
        Environment env = new Environment(settings);
        File udfFile = new File(env.homeFile(), "udf");
        if (!udfFile.exists()) {
            return;
        }
        File[] files = udfFile.listFiles();
        if (files == null) {
            logger.debug("failed to list udf functions from {}. Check your right access.",
                    udfFile.getAbsolutePath());
            return;
        }
        for (File file : files) {
            try {
                loadUDF(file);
            } catch (IOException | ScriptException e) {
                logger.warn(e.getMessage(), e);
            }
        }
        this.registerUDFS();
    }

    protected void loadUDF(File udfFile) throws IOException, ScriptException {
        assert udfFile != null;
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByExtension(Files.getFileExtension(udfFile.getName()));
        if (scriptEngine == null) {
            return;
        }
        ScriptContext scriptContext = scriptEngine.getContext();
        scriptEngine.eval(
                new BufferedReader(new InputStreamReader(new FileInputStream(udfFile))),
                scriptContext
        );
        for (Integer scope : scriptContext.getScopes()) {
            Bindings bindings = scriptContext.getBindings(scope);
            if (bindings == null) {
                continue;
            }
            for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                Object value = entry.getValue();
                tryLoadClass(value);
            }
        }
    }

    private void tryLoadClass(Object value) {
        //if (value instanceof RubyClass) {
        //    // TODO: check if userDefinedScalar or aggregation
        //    IRubyObject instance = ((RubyClass) value).newInstance(threadContext, Block.NULL_BLOCK);
        //    Object o = instance.toJava(UserDefinedScalarFunction.class);
        //    scalars.add((UserDefinedScalarFunction)o);
        //}
        if (value instanceof PyType && !(value instanceof PyJavaType)) {
            if (((PyType) value).isSubType(PyJavaType.fromClass(UserDefinedAggregationFunction.class))) {
                PyObject udf = ((PyType) value).__call__();
                UserDefinedAggregationFunction<?> userDefinedFunction =
                        (UserDefinedAggregationFunction<?>) udf.__tojava__(UserDefinedAggregationFunction.class);
                aggregations.add(userDefinedFunction);
            } else if (((PyType) value).isSubType(PyJavaType.fromClass(UserDefinedScalarFunction.class))) {
                PyObject udf = ((PyType) value).__call__();
                UserDefinedScalarFunction userDefinedFunction =
                        (UserDefinedScalarFunction) udf.__tojava__(UserDefinedScalarFunction.class);
                scalars.add(userDefinedFunction);
            }
        }
    }

    public void registerUDFS() {
        for (UserDefinedScalarFunction<?, ?> udf : scalars) {
            FunctionIdent ident = udf.ident();
            if (ident != null) {
                functionBinder.addBinding(ident).toInstance(udf);
            }
            DynamicFunctionResolver dynamicFunctionResolver = udf.dynamicFunctionResolver();
            if (dynamicFunctionResolver != null) {
                resolverBinder.addBinding(udf.name()).toInstance(dynamicFunctionResolver);
            }
        }

        for (UserDefinedAggregationFunction<?> udf : aggregations) {
            FunctionIdent ident = udf.ident();
            if (ident != null) {
                functionBinder.addBinding(ident).toInstance(udf);
            }
            DynamicFunctionResolver dynamicFunctionResolver = udf.dynamicFunctionResolver();
            if (dynamicFunctionResolver != null) {
                resolverBinder.addBinding(udf.name()).toInstance(dynamicFunctionResolver);
            }
        }
    }
}
