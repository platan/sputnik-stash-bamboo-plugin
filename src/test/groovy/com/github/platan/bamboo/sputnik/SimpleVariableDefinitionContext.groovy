package com.github.platan.bamboo.sputnik

import com.atlassian.bamboo.variable.VariableDefinitionContext
import com.atlassian.bamboo.variable.VariableType


class SimpleVariableDefinitionContext implements VariableDefinitionContext {

    private String key;
    private String value;
    private VariableType variableType;

    SimpleVariableDefinitionContext(String key, String value, VariableType variableType) {
        this.key = key
        this.value = value
        this.variableType = variableType
    }

    String getKey() {
        return key
    }


    String getValue() {
        return value
    }

    VariableType getVariableType() {
        return variableType
    }

    void setKey(String key) {
        this.key = key
    }

    void setValue(String value) {
        this.value = value
    }

    void setVariableType(VariableType variableType) {
        this.variableType = variableType
    }
}
