package io.crate.udf;

import io.crate.metadata.DynamicFunctionResolver;
import io.crate.metadata.FunctionIdent;
import io.crate.operation.aggregation.AggregationFunction;
import io.crate.operation.aggregation.AggregationState;

public abstract class UserDefinedAggregationFunction<TAggState extends AggregationState> extends AggregationFunction<TAggState> {

    public abstract String name();

    public abstract FunctionIdent ident();

    public abstract DynamicFunctionResolver dynamicFunctionResolver();
}
