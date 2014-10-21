package io.crate.udf;

import io.crate.metadata.DynamicFunctionResolver;
import io.crate.metadata.FunctionIdent;
import io.crate.metadata.Scalar;

import javax.annotation.Nullable;

public abstract class UserDefinedScalarFunction<ReturnType, InputType> extends Scalar<ReturnType, InputType> {

    public abstract String name();

    @Nullable
    public abstract FunctionIdent ident();

    @Nullable
    public abstract DynamicFunctionResolver dynamicFunctionResolver();

}
