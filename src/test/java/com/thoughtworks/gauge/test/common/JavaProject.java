package com.thoughtworks.gauge.test.common;

import com.thoughtworks.gauge.Table;
import com.thoughtworks.gauge.TableRow;
import com.thoughtworks.gauge.test.StepImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaProject extends GaugeProject {
    public static final String DEFAULT_AGGREGATION = "AND";

    public JavaProject(String projName) throws IOException {
        super("java", projName);
    }

    public Map<String, String> getLanguageSpecificFiles() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("src", "dir");
        map.put("libs", "dir");
        map.put(Util.combinePath("src", "test", "java", "StepImplementation.java"), "file");
        map.put(Util.combinePath("env", "default", "java.properties"), "file");
        return map;
    }

    public void implementStep(StepImpl stepImpl) throws Exception {
        List<String> paramTypes = new ArrayList<>();
        StepValueExtractor.StepValue stepValue = new StepValueExtractor().getFor(stepImpl.getStepText());
        String className = Util.getUniqueName();
        StringBuilder classText = createClassTeplate(className, stepValue.value);
        if (stepImpl.isContinueOnFailure()) {
            classText.insert(0, "import com.thoughtworks.gauge.ContinueOnFailure;\n");
            classText.append("\n@ContinueOnFailure");
            classText.append("(").append(String.join(",", stepImpl.getErrorTypes())).append(")\n");
        }
        classText.append("public void ").append("stepImplementation(");
        addParameters(classText, paramTypes, stepValue);
        String implementation = stepImpl.getImplementation();
        implementation = getStepImplementation(stepValue, implementation, paramTypes, stepImpl.isValidStatement());
        classText.append(") {\n").append(implementation).append("\n}\n");
        classText.append("}");
        Util.writeToFile(Util.combinePath(getStepImplementationsDir(), className + ".java"), classText.toString());
    }

    @Override
    public String getDataStoreWriteStatement(TableRow row, List<String> columnNames) {
        String dataStoreType = row.getCell("datastore type");
        String key = row.getCell("key");
        String value = row.getCell("value");
        return "com.thoughtworks.gauge.datastore.DataStoreFactory.get" + dataStoreType + "DataStore().put(\"" + key + "\",\"" + value + "\");";
    }

    @Override
    public String getDataStorePrintValueStatement(TableRow row, List<String> columnNames) {
        String dataStoreType = row.getCell(columnNames.get(3));
        String key = row.getCell(columnNames.get(1));
        return "System.out.println(com.thoughtworks.gauge.datastore.DataStoreFactory.get" + dataStoreType + "DataStore().get(\"" + key + "\"));";
    }

    @Override
    public void configureCustomScreengrabber(String stubScreenshot) throws IOException {
        String className = Util.getUniqueName();
        StringBuilder sb = new StringBuilder();
        sb.append("import com.thoughtworks.gauge.screenshot.ICustomScreenshotGrabber;\n");
        sb.append("\n");
        sb.append("public class " + className + " implements ICustomScreenshotGrabber {\n");
        sb.append("\n");
        sb.append("    public byte[] takeScreenshot() {\n");
        sb.append("        return \"" + stubScreenshot + "\".getBytes();\n");
        sb.append("    }\n");
        sb.append("}");
        Util.writeToFile(Util.combinePath(getStepImplementationsDir(), className + ".java"), sb.toString());
    }

    @Override
    public String getStepImplementation(StepValueExtractor.StepValue stepValue, String implementation, List<String> paramTypes, boolean appendCode) {
        StringBuilder builder = new StringBuilder();
        if (implementation.toLowerCase().equals(PRINT_PARAMS)) {
            builder.append("System.out.println(");
            for (int i = 0; i < stepValue.paramCount; i++) {
                builder.append("\"param").append(i).append("=\"+").append("param").append(i);
                if (i != stepValue.paramCount - 1) {
                    builder.append("+\",\"+");
                }
            }
            builder.append(");\n");
        } else if (implementation.toLowerCase().equals(THROW_EXCEPTION)) {
            return "throw new RuntimeException();";
        } else if (implementation.toLowerCase().startsWith("throw")) {
            return "throw new " + implementation.trim().substring(implementation.indexOf(' ')) + "();";
        } else {
            if (appendCode) {
                builder.append(implementation);
            } else {
                builder.append("System.out.println(").append(implementation).append(");\n");
            }
        }
        return builder.toString();
    }

    @Override
    public void createHookWithPrint(String hookLevel, String hookType, String printStatement) throws Exception {
        String implementation = String.format("System.out.println(\"%s\");", printStatement);
        String method = createHookMethod(hookLevel, hookType, implementation, DEFAULT_AGGREGATION, new ArrayList<String>());
        createHook(hookLevel, hookType, method);
    }

    @Override
    public void createHookWithException(String hookLevel, String hookType) throws IOException {
        createHook(hookLevel, hookType, createHookMethod(hookLevel, hookType, "throw new RuntimeException();", DEFAULT_AGGREGATION, new ArrayList<String>()));
    }

    @Override
    public void createHooksWithTagsAndPrintMessage(String hookLevel, String hookType, String printString, String aggregation, Table tagsTable) throws IOException {
        String implementation = String.format("System.out.println(\"%s\");", printString);
        String method = createHookMethod(hookLevel, hookType, implementation, aggregation, tagsTable.getColumnValues("tags"));
        createHook(hookLevel, hookType, method);
    }

    private void createHook(String hookLevel, String hookType, String method) throws IOException {
        StringBuilder classText = new StringBuilder();
        classText.append(String.format("import com.thoughtworks.gauge.%s;\n", hookName(hookLevel, hookType)));
        classText.append("import com.thoughtworks.gauge.Operator;");
        String className = Util.getUniqueName();
        classText.append("public class ").append(className).append("{\n");
        classText.append(method);
        classText.append("\n}");
        Util.writeToFile(Util.combinePath(getStepImplementationsDir(), className + ".java"), classText.toString());
    }

    private String createHookMethod(String hookLevel, String hookType, String implementation, String aggregation, List<String> tags) {
        StringBuilder methodText = new StringBuilder();
        String hookAttributes = isSuiteHook(hookLevel) ? "" : hookAttributesString(tags, aggregation);
        methodText.append(String.format("@%s(%s)\n", hookName(hookLevel, hookType), hookAttributes));
        methodText.append(String.format("public void hook() {\n"));
        methodText.append(String.format("%s\n", implementation));
        methodText.append("\n}\n");
        return methodText.toString();
    }

    private boolean isSuiteHook(String hookLevel) {
        return hookLevel.trim().equals("suite");
    }

    private String hookName(String hookLevel, String hookType) {
        return String.format("%s%s", Util.capitalize(hookType), Util.capitalize(hookLevel));
    }

    private String hookAttributesString(List<String> tags, String aggregation) {
        return String.format("tags = {%s}, tagAggregation = Operator.%s ", Util.joinList(Util.quotifyValues(tags)), aggregation);
    }

    private String getStepImplementationsDir() {
        return Util.combinePath(getProjectDir().getAbsolutePath(), "src", "test", "java");
    }

    private void addParameters(StringBuilder classText, List<String> paramTypes, StepValueExtractor.StepValue stepValue) {
        for (int i = 0; i < stepValue.paramCount; i++) {
            if (i + 1 == stepValue.paramCount) {
                classText.append("Object param").append(i);
            } else {
                classText.append("Object param").append(i).append(", ");
            }
            paramTypes.add("Object");
        }
    }

    private StringBuilder createClassTeplate(String className, String stepText) {
        StringBuilder classText = new StringBuilder();
        classText.append("import com.thoughtworks.gauge.Step;\n");
        classText.append("public class ").append(className).append("{\n");
        classText.append("@Step(\"").append(stepText).append("\")\n");
        return classText;
    }
}
