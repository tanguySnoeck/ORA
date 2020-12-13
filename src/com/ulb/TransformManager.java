package com.ulb;

import brut.common.BrutException;
import javassist.*;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import brut.apktool.Main;
import org.w3c.dom.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class TransformManager {
    private static String PATH_APKSIGNER = "C:/Users/tangu/AppData/Local/Android/Sdk/build-tools/29.0.2/apksigner.bat";
    private static String PATH_DX = "C:/Users/tangu/AppData/Local/Android/Sdk/build-tools/29.0.2/dx.bat";
    private static String PATH_KEYSTORE = "D:/Documents/ULB/3e/thesis/RASP/lab/rasp.keystore";
    private static String KEY_STORE_PSWD = "rasp";

    private String mainActivity;
    private String apkPath;
    private String apkName;
    private String jarPath;
    private String apkContent;

    public TransformManager(String apkPath, String apkName, String jarPath, String apkContent) {
        this.apkPath = apkPath;
        this.apkName = apkName;
        this.jarPath = jarPath;
        this.apkContent = apkContent;
    }

    public void extractMainActivity() {
        try {
            System.out.println("Extracting manifest from apk ....");
            String apktool_content = "./" + apkName + "_apktool_content";

            Main.main(new String[]{"-f", "d", apkPath, "-o", apktool_content});
            System.out.println("Manifest extracted !");

            DocumentBuilder builder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder();
            Document manifest = (Document) builder.parse(new File(apktool_content + "/AndroidManifest.xml"));
            NodeList actions = manifest.getElementsByTagName("action");

            for (int i = 0; i < actions.getLength(); i++) {
                Element element = ((Element)actions.item(i));
                if (element.getAttribute("android:name").equals("android.intent.action.MAIN")) {
                    mainActivity = ((Element)element.getParentNode().getParentNode()).getAttribute("android:name");
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrutException e) {
            e.printStackTrace();
        }
    }

    public void transformAPK(){
        try {
            ClassPool pool = ClassPool.getDefault();
            pool.insertClassPath(jarPath);
            pool.insertClassPath("C:/Users/tangu/AppData/Local/Android/Sdk/platforms/android-24/android.jar");

            CtClass cc = pool.get(mainActivity);
            CtMethod cm = cc.getDeclaredMethod("onCreate");
            cm.insertBefore("{System.exit(1);}");

            cc.writeFile();

            System.out.println("it works?");
        } catch (NotFoundException | CannotCompileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void repackageApk() {
        try {
            System.out.println("Converting back to dex file");

            String output = "--output=./" + apkContent + "/classes.dex";
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", PATH_DX, "--dex", output, jarPath);
            pb.start().waitFor();

            System.out.println("Compiling done !");

            Files.move(Paths.get(apkContent + "/classes.dex"), Paths.get(apkContent + "/classes.dex"), REPLACE_EXISTING);
            ZipFile repackaged_apk = new ZipFile("modified_" + apkName + ".apk");

            Arrays.asList(new File(apkContent).listFiles()).forEach(file -> {
                try {
                    if (file.isDirectory())
                        repackaged_apk.addFolder(file);
                    else
                        repackaged_apk.addFile(file);
                } catch (ZipException e) {
                    e.printStackTrace();
                }
            });

            System.out.println("Signing modified apk ...");

            new ProcessBuilder("cmd", "/c", PATH_APKSIGNER, "sign", "--ks-key-alias", "otacos", "--min-sdk-version", "19", "--ks-pass", "pass:" + KEY_STORE_PSWD, "--ks", PATH_KEYSTORE, "modified_" + apkName + ".apk").start().waitFor();

            System.out.println("APK signed !");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clean() {
        System.out.println("cleaning temp data ... ");

        new File("./" + apkName + ".jar").delete();
        new File("./" + apkName + "_apktool_content").delete();
        new File("./" + apkName + "_extracted").delete();
        new File("./" + apkName + "-error.zip").delete();
    }
}
