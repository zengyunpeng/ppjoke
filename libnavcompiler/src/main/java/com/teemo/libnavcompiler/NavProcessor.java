package com.teemo.libnavcompiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPObject;
import com.google.auto.service.AutoService;
import com.teemo.libnavannotation.ActivityDestination;
import com.teemo.libnavannotation.FragmentDestination;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"com.teemo.libnavannotation.ActivityDestination", "com.teemo.libnavannotation.ActivityDestination"})
public class NavProcessor extends AbstractProcessor {
    Messager messager;
    Filer filer;
    private static final String OUTPUT_FILE_NAME = "destnation.json";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element> fragmentElements = roundEnvironment.getElementsAnnotatedWith(FragmentDestination.class);
        Set<? extends Element> activityElements = roundEnvironment.getElementsAnnotatedWith(ActivityDestination.class);

        if (!fragmentElements.isEmpty() || !activityElements.isEmpty()) {
            HashMap<String, JSONObject> destMap = new HashMap<>();
            handleDestination(fragmentElements, FragmentDestination.class, destMap);
            handleDestination(activityElements, ActivityDestination.class, destMap);

            FileOutputStream fos = null;
            OutputStreamWriter writer = null;
            try {
                FileObject resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", OUTPUT_FILE_NAME);
                String path = resource.toUri().getPath();
                messager.printMessage(Diagnostic.Kind.NOTE, "path: " + path);
                String appPath = path.substring(0, path.indexOf("app") + 4);
                String assetsPath = appPath + "src/main/assets/";

                File assetsFile = new File(assetsPath);
                if (!assetsFile.exists()) {
                    assetsFile.mkdir();
                }

                File outPutFile = new File(assetsFile, OUTPUT_FILE_NAME);
                if (outPutFile.exists()) {
                    outPutFile.delete();
                }
                outPutFile.createNewFile();
                String content = JSON.toJSONString(destMap);
                fos = new FileOutputStream(outPutFile);
                writer = new OutputStreamWriter(fos, "UTF-8");
                writer.write(content);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }

    private void handleDestination(Set<? extends Element> elements, Class<? extends Annotation> annotationClass, HashMap<String, JSONObject> destMap) {
        for (Element element : elements) {
            TypeElement typeElement = (TypeElement) element;
            String pageUrl = null;
            String clazzName = typeElement.getQualifiedName().toString();
            int id = Math.abs(clazzName.hashCode());
            boolean needLogin = false;
            boolean asStarter = false;
            boolean isFragment = false;

            Annotation annotation = typeElement.getAnnotation(annotationClass);
            if (annotation instanceof FragmentDestination) {
                FragmentDestination fragmentDestination = (FragmentDestination) annotation;
                pageUrl = fragmentDestination.pageUrl();
                needLogin = fragmentDestination.needLogin();
                asStarter = fragmentDestination.asStarter();
                isFragment = true;
            } else if (annotation instanceof ActivityDestination) {
                ActivityDestination activityDestination = (ActivityDestination) annotation;
                pageUrl = activityDestination.pageUrl();
                needLogin = activityDestination.needLogin();
                asStarter = activityDestination.asStarter();
                isFragment = false;
            }

            if (destMap.containsKey(pageUrl)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "不同页面不允许使用不同的pageUrl: " + pageUrl);
            } else {
                JSONObject jsonpObject = new JSONObject();
                jsonpObject.put("id", id);
                jsonpObject.put("pageUrl", pageUrl);
                jsonpObject.put("needLogin", needLogin);
                jsonpObject.put("asStarter", asStarter);
                jsonpObject.put("isFragment", isFragment);
                destMap.put(clazzName, jsonpObject);
            }


        }

    }
}
