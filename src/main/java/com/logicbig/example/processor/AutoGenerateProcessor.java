package com.logicbig.example.processor;

import com.logicbig.example.annotation.ImplementCrud;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.*;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

@SupportedAnnotationTypes({"com.logicbig.example.annotation.ImplementCrud"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AutoGenerateProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (annotations.isEmpty()) {
            return false;
        }

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(ImplementCrud.class);

        for (Element element : elements) {
            if (element.getKind() == ElementKind.CLASS) {

                ImplementCrud implementCrud = element.getAnnotation(ImplementCrud.class);
//                Element e = element.getEnclosingElement();
                for (AnnotationMirror annotationMirror : element.getEnclosingElement().getAnnotationMirrors()) {
                    System.out.println("**  " + annotationMirror.getAnnotationType());
                }
                String pkg = getPackageName(element);

                Properties props = new Properties();
                URL url = this.getClass().getClassLoader().getResource("velocity.properties");

                try {
                    props.load(url.openStream());
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }

                VelocityEngine velocityEngine = new VelocityEngine(props);
                velocityEngine.init();

                VelocityContext velocityContext = new VelocityContext();
                velocityContext.put("packageImport", pkg);
                velocityContext.put("pathRest", implementCrud.pathRest());
                velocityContext.put("classNameId", implementCrud.classNameId());
                velocityContext.put("packageName", implementCrud.packageSave());
                velocityContext.put("className", element.getSimpleName());
                velocityContext.put("objectName", ucFirst(element.getSimpleName().toString()));

                try {
                    generateClassRepository(element, velocityEngine, velocityContext, pkg);
                    generateClassService(element, velocityEngine, velocityContext, pkg);
                    generateClassController(element, velocityEngine, velocityContext, pkg);
                    //generateClassServiceInterface(element, velocityEngine, velocityContext, pkg);
                } catch (Exception e) {
                    error(e.getMessage(), null);
                }
            } else {
                error("The annotation @ImplementCrud can only be applied on class: ", element);
            }
        }
        return false;
    }

    private void generateClassRepository(Element element, VelocityEngine velocityEngine, VelocityContext velocityContext, String pkg) throws IOException {
        Template template = velocityEngine.getTemplate("templates/repositoryInfo.vm");
        generateClass(element.getSimpleName() + "Repository", pkg, template, velocityContext);
    }

    private void generateClassService(Element element, VelocityEngine velocityEngine, VelocityContext velocityContext, String pkg) throws IOException {
        Template template = velocityEngine.getTemplate("templates/serviceInfo.vm");
        generateClass(element.getSimpleName() + "Service", pkg, template, velocityContext);
    }

    private void generateClassController(Element element, VelocityEngine velocityEngine, VelocityContext velocityContext, String pkg) throws IOException {
        Template template = velocityEngine.getTemplate("templates/controllerInfo.vm");
        generateClass(element.getSimpleName() + "Controller", pkg, template, velocityContext);
    }

//    private void generateClassServiceInterface(Element element, VelocityEngine velocityEngine, VelocityContext velocityContext, String pkg) throws IOException {
//        Template template = velocityEngine.getTemplate("templates/serviceInterfaceInfo.vm");
//        generateClass(pkg + "." + element.getSimpleName() + "Service", template, velocityContext);
//    }
    private void generateClass(String fileName, String pkg, Template template, VelocityContext velocityContext) throws IOException {
        JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(pkg + "." + fileName);
        String ruta = sourceFile.toUri().toString();
        ruta = ruta.replace("file:", "");
        ruta = ruta.replace("target/generated-sources/annotations", "src/main/java");
        String packageName = velocityContext.get("packageName").toString();
        packageName = packageName.replace(".", "/");
        ruta = ruta.replace("com/logicbig/example", packageName);
        String theDir = ruta;
        theDir = theDir.replace(fileName + ".java", "");
        System.out.println(theDir);
        File fileTheDir = new File(theDir);
        if (!fileTheDir.exists()) {
            fileTheDir.mkdirs();
        }
        File archivo = new File(ruta);

        //velocityContext.put("packageName", "com.logicbig.Annotations");
        if (!archivo.exists()) {
            BufferedWriter bw = new BufferedWriter(new FileWriter(archivo));
            StringWriter writer = new StringWriter();
            template.merge(velocityContext, writer);
            bw.write(writer.toString());
            bw.close();
        }
    }

    public static String ucFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        } else {
            return str.substring(0, 1).toLowerCase() + str.substring(1);
        }
    }

    private String getPackageName(Element element) {
        List<PackageElement> packageElements = ElementFilter.packagesIn(Arrays.asList(element.getEnclosingElement()));

        Optional<PackageElement> packageElement = packageElements.stream().findAny();

        return packageElement.isPresent() ? packageElement.get().getQualifiedName().toString() : null;

    }

    private void error(String msg, Element e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, e);
    }
}
