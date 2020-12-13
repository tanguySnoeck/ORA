package com.ulb;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import net.lingala.zip4j.*;

public class Main {
    public static String PATH_D2J = "D:/Documents/ULB/3e/thesis/RASP/lab/dex2jar-2.0/d2j-dex2jar.bat";

    public static void main(String[] args) {
        boolean fileExists = false;
        String apkPath;
        Scanner sc = new Scanner(System.in);

        do {
            System.out.println("Path of apk to transform: ");
            apkPath = sc.nextLine();

            if (!new File(apkPath).exists())
                System.out.println("File does not exist !");
            else
                fileExists = true;
        } while(!fileExists);

        String[] apkSplit = apkPath.split("\\\\");
        String apkName = apkSplit[apkSplit.length - 1].split("\\.")[0];
        String outputJarPath = "./" + apkName + ".jar";

        try {
            System.out.println("Converting to jar ...");
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", PATH_D2J, apkPath,  "-o", outputJarPath);
            pb.start().waitFor();
            System.out.println("Jar created !");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        String apkContent = "";

        try {
            System.out.println("Unzipping apk ...");
            apkContent = apkName + "_extracted";
            new ZipFile(apkPath).extractAll(apkContent);
            System.out.println("APK extracted !");
        } catch (IOException e) {
            e.printStackTrace();
        }

        new File(apkContent + "/META-INF/CERT.RSA").delete();
        new File(apkContent + "/META-INF/CERT.SF").delete();
        new File(apkContent + "/META-INF/MANIFEST.MF").delete();

        TransformManager transformManager = new TransformManager(apkPath, apkName, outputJarPath, apkContent);

        transformManager.extractMainActivity();
        transformManager.transformAPK();
        transformManager.repackageApk();
        transformManager.clean();

        System.out.println("Repackaging done !\n APK protected an ready to be installed !");
    }
}
