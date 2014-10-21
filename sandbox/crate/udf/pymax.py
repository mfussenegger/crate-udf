#!/usr/bin/env python

from io.crate.udf import UserDefinedAggregationFunction
from io.crate.operation.aggregation import AggregationState
from io.crate.types import DataTypes
from io.crate.metadata import FunctionIdent, FunctionInfo
from java.util import Arrays


class PythonMax(UserDefinedAggregationFunction):

    class PythonMaxState(AggregationState):

        def __init__(self):
            self._value = 0

        def value(self):
            return self._value

        def add(self, value):
            if not value:
                return
            if not self._value:
                self._value = value
            elif value > self._value:
                self._value = value

        def reduce(self, other):
            if other:
                self.add(other.value())


    def name(self):
        return 'pymax'

    def ident(self):
        return FunctionIdent(self.name(), Arrays.asList(DataTypes.INTEGER))

    def info(self):
        return FunctionInfo(self.ident(), DataTypes.LONG, True)

    def dynamicFunctionResolver(self):
        return None

    def normalizeSymbol(self, symbol):
        return symbol

    def iterate(self, state, inputs):
        state.add(inputs[0].value())

    def newState(self):
        return self.PythonMaxState()
