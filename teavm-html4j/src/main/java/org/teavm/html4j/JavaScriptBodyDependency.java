/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.html4j;

import net.java.html.js.JavaScriptBody;
import org.teavm.dependency.*;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class JavaScriptBodyDependency implements DependencyListener {
    private DependencyNode allClassesNode;

    @Override
    public void started(DependencyChecker dependencyChecker) {
        allClassesNode = dependencyChecker.createNode();
        allClassesNode.setTag("JavaScriptBody:global");
    }

    @Override
    public void classAchieved(DependencyChecker dependencyChecker, String className) {
        allClassesNode.propagate(className);
    }

    @Override
    public void methodAchieved(DependencyChecker dependencyChecker, MethodReference methodRef) {
        ClassHolder cls = dependencyChecker.getClassSource().get(methodRef.getClassName());
        MethodHolder method = cls.getMethod(methodRef.getDescriptor());
        AnnotationReader annot = method.getAnnotations().get(JavaScriptBody.class.getName());
        if (annot != null) {
            AnnotationValue javacall = annot.getValue("javacall");
            MethodGraph graph = dependencyChecker.attachMethodGraph(methodRef);
            if (graph.getResult() != null) {
                allClassesNode.connect(graph.getResult());
            }
            for (int i = 0; i < graph.getParameterCount(); ++i) {
                graph.getVariable(i).connect(allClassesNode);
                allClassesNode.getArrayItem().connect(graph.getVariable(i).getArrayItem());
                allClassesNode.getArrayItem().getArrayItem().connect(graph.getVariable(i)
                        .getArrayItem().getArrayItem());
            }
            if (javacall != null && javacall.getBoolean()) {
                String body = annot.getValue("body").getString();
                new GeneratorJsCallback(dependencyChecker.getClassSource(), dependencyChecker).parse(body);
            }
        }
    }

    @Override
    public void fieldAchieved(DependencyChecker dependencyChecker, FieldReference field) {
    }

    private static MethodReader findMethod(ClassReaderSource classSource, String clsName, MethodDescriptor desc) {
        while (clsName != null) {
            ClassReader cls = classSource.get(clsName);
            for (MethodReader method : cls.getMethods()) {
                if (method.getName().equals(desc.getName()) && sameParams(method.getDescriptor(), desc)) {
                    return method;
                }
            }
            clsName = cls.getParent();
        }
        return null;
    }
    private static boolean sameParams(MethodDescriptor a, MethodDescriptor b) {
        if (a.parameterCount() != b.parameterCount()) {
            return false;
        }
        for (int i = 0; i < a.parameterCount(); ++i) {
            if (!a.parameterType(i).equals(b.parameterType(i))) {
                return false;
            }
        }
        return true;
    }

    private class GeneratorJsCallback extends JsCallback {
        private ClassReaderSource classSource;
        private DependencyChecker dependencyChecker;
        public GeneratorJsCallback(ClassReaderSource classSource, DependencyChecker dependencyChecker) {
            this.classSource = classSource;
            this.dependencyChecker = dependencyChecker;
        }
        @Override protected CharSequence callMethod(String ident, String fqn, String method, String params) {
            MethodDescriptor desc = MethodDescriptor.parse(method + "V");
            MethodReader reader = findMethod(classSource, fqn, desc);
            if (reader != null) {
                if (reader.hasModifier(ElementModifier.STATIC) || reader.hasModifier(ElementModifier.FINAL)) {
                    MethodGraph graph = dependencyChecker.attachMethodGraph(reader.getReference());
                    for (int i = 0; i <= graph.getParameterCount(); ++i) {
                        allClassesNode.connect(graph.getVariable(i));
                    }
                } else {
                    allClassesNode.addConsumer(new VirtualCallbackConsumer(classSource, dependencyChecker, desc));
                }
            }
            return "";
        }
    }

    private class VirtualCallbackConsumer implements DependencyConsumer {
        private ClassReaderSource classSource;
        private DependencyChecker dependencyChecker;
        private MethodDescriptor desc;
        public VirtualCallbackConsumer(ClassReaderSource classSource, DependencyChecker dependencyChecker,
                MethodDescriptor desc) {
            this.classSource = classSource;
            this.dependencyChecker = dependencyChecker;
            this.desc = desc;
        }
        @Override public void consume(String type) {
            MethodReader reader = findMethod(classSource, type, desc);
            if (reader != null) {
                MethodGraph graph = dependencyChecker.attachMethodGraph(reader.getReference());
                for (int i = 0; i <= graph.getParameterCount(); ++i) {
                    allClassesNode.connect(graph.getVariable(i));
                }
            }
        }
    }
}