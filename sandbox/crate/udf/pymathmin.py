#!/usr/bin/env python

from io.crate.udf import UserDefinedScalarFunction
from io.crate.types import DataTypes
from io.crate.metadata import FunctionIdent, FunctionInfo
from java.util import Arrays
from java.lang import Long


class MathMin(UserDefinedScalarFunction):

    def name(self):
        return 'math_min'

    def ident(self):
        return FunctionIdent(self.name(),
                             Arrays.asList(DataTypes.LONG, DataTypes.LONG))

    def info(self):
        return FunctionInfo(self.ident(), DataTypes.LONG)

    def normalizeSymbol(self, symbol):
        return symbol

    def dynamicFunctionResolver(self):
        return None

    def evaluate(self, args):
        return Long(min(args[0].value(), args[1].value()))
