package com.yahoo.squidb.processor.writers;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.properties.factory.PropertyGeneratorFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;


public class SyncedTableModelFileWriter extends TableModelFileWriter {

    private boolean isSyncable;

    public SyncedTableModelFileWriter(TypeElement element, PropertyGeneratorFactory propertyGeneratorFactory, AptUtils utils) {
        super(element, propertyGeneratorFactory, utils);
        this.isSyncable = modelSpec.isSyncable();
    }

    @Override
    protected Collection<DeclaredTypeName> getModelSpecificImports() {
        List<DeclaredTypeName> imports = (List<DeclaredTypeName>) super.getModelSpecificImports();
        if(isSyncable) {
            imports.add(TypeConstants.INTEGER_PROPERTY);
        }
        return imports;
    }

    @Override
    protected int getPropertiesArrayLength() {
        return super.getPropertiesArrayLength() + (isSyncable ? 4 : 0);
    }

    @Override
    protected void emitAllProperties() throws IOException {
        super.emitAllProperties();
        if(isSyncable) {
            emitOriginIdPropertyDeclaration();
            emitRowStatusPropertyDeclaration();
        }
    }

    private void emitOriginIdPropertyDeclaration() throws IOException {
        Expression fieldConstructor = Expressions.callConstructor(TypeConstants.LONG_PROPERTY, "origin_id", "\"DEFAULT 0 NOT NULL\"");
        writer.writeFieldDeclaration(TypeConstants.LONG_PROPERTY, "ORIGIN_ID", fieldConstructor);
    }

    private void emitRowStatusPropertyDeclaration() throws IOException {
        Expression fieldConstructor = Expressions.callConstructor(TypeConstants.INTEGER_PROPERTY, "row_status", "\"DEFAULT 1 NOT NULL\"");
        writer.writeFieldDeclaration(TypeConstants.INTEGER_PROPERTY, "ROW_STATUS", fieldConstructor);
    }

    @Override
    protected void writePropertiesInitializationBlock() throws IOException {
        super.writePropertiesInitializationBlock();
        if(isSyncable) {
            int startIndex = propertyGenerators.size() + 1;
            writer.writeStatement(Expressions
                    .assign(Expressions.arrayReference(PROPERTIES_ARRAY_NAME, startIndex), Expressions.fromString("ORIGIN_ID")));
            writer.writeStatement(Expressions
                    .assign(Expressions.arrayReference(PROPERTIES_ARRAY_NAME, startIndex + 1), Expressions.fromString("ROW_STATE")));
        }
    }

    @Override
    protected void emitGettersAndSetters() throws IOException {
        super.emitGettersAndSetters();
        if(isSyncable) {
            emitOriginIdGetter();
            emitConfirmOriginInsertionMethod();
            emitRowStateGetter();
            emitMarkUpdatingMethod();
            emitMarkDeletingMethod();
            emitMarkIdleMethod();
        }
    }

    private void emitOriginIdGetter() throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setReturnType(CoreTypes.JAVA_LONG)
                .setMethodName("getOriginId")
                .setModifiers(Modifier.PUBLIC);
        writer.beginMethodDefinition(params)
                .writeStringStatement("return get(ORIGIN_ID)")
                .finishMethodDefinition();
    }

    private void emitConfirmOriginInsertionMethod() throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setModifiers(Modifier.PUBLIC)
                .setReturnType(generatedClassName)
                .setMethodName("confirmOriginInsertion")
                .setArgumentTypes(CoreTypes.PRIMITIVE_LONG)
                .setArgumentNames("originId");
        writer.beginMethodDefinition(params)
                .writeStringStatement("set(ORIGIN_ID, originId)")
                .writeStringStatement("set(ROW_STATE, 0)")
                .writeStringStatement("return this")
                .finishMethodDefinition();
    }

    private void emitRowStateGetter() throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setReturnType(CoreTypes.JAVA_INTEGER)
                .setMethodName("getRowState")
                .setModifiers(Modifier.PUBLIC);
        writer.beginMethodDefinition(params)
                .writeStringStatement("return get(ROW_STATE)")
                .finishMethodDefinition();
    }

    private void emitMarkUpdatingMethod() throws IOException {
        emitRowStateChangeMethod("markUpdating", 2);
    }

    private void emitMarkDeletingMethod() throws IOException {
        emitRowStateChangeMethod("markDeleting", 3);
    }

    private void emitMarkIdleMethod() throws IOException {
        emitRowStateChangeMethod("markIdle", 0);
    }

    private void emitRowStateChangeMethod(String methodName, int assigningState) throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
                .setReturnType(generatedClassName)
                .setMethodName(methodName)
                .setModifiers(Modifier.PUBLIC);
        writer.beginMethodDefinition(params)
                .writeStringStatement(String.format("set(ROW_STATE, %d)", assigningState))
                .writeStringStatement("return this")
                .finishMethodDefinition();
    }
}
