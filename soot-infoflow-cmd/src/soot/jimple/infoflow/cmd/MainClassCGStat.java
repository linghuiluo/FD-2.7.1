package soot.jimple.infoflow.cmd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.xmlpull.v1.XmlPullParserException;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

/**
 * find concrete methods in the apk
 */
public class MainClassCGStat {
    public static void main(String... args) throws ParseException, XmlPullParserException, IOException {
        Options options = new Options();
        options.addOption("h", "help", false, "Print this message");
        options.addOption("apk", "apkPath", true, "The path to apk file");
        options.addOption("p", "androidPlatform", true, "The path to android platform jars");
        options.addOption("o", "outputPath", true, "The path to output file");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        HelpFormatter helper = new HelpFormatter();
        String cmdLineSyntax =
            "Process an apk \n Either -apk <apk> -p <android platform jars> \n ";
        if (cmd.hasOption('h')) {
            helper.printHelp(cmdLineSyntax, options);
            return;
        }

        if (!cmd.hasOption("apk") || !cmd.hasOption("p")) {
            helper.printHelp(cmdLineSyntax, options);
            return;
        }
        String apkPath = null;
        if (cmd.hasOption("apk") ) {
            apkPath = cmd.getOptionValue("apk");
        }
        String androidJarPath = null;
        if (cmd.hasOption("p")) {
            androidJarPath = cmd.getOptionValue("p");
        }
        File apk = new File(apkPath);
        String outputPath = apk.getParent() + File.separator + apk.getName().split(".apk")[0] + "-concrete-methods.txt";
        if (cmd.hasOption("o")) {
            outputPath = cmd.getOptionValue("o");
        }

        ProcessManifest pmf = new ProcessManifest(apkPath);
        String packageName = pmf.getPackageName();
        G.v().reset();
        soot.options.Options.v().set_include_all(true);
        soot.options.Options.v().set_no_bodies_for_excluded(true);
        soot.options.Options.v().set_allow_phantom_refs(true);

        soot.options.Options.v().set_process_dir(Collections.singletonList(apkPath));
        soot.options.Options.v().set_android_jars(androidJarPath);

        soot.options.Options.v().set_src_prec(soot.options.Options.src_prec_apk);
        soot.options.Options.v().set_keep_line_number(true);
        soot.options.Options.v().set_process_multiple_dex(true);
        soot.options.Options.v().setPhaseOption("jb", "use-original-names:true");

        soot.options.Options.v().set_soot_classpath(Scene.v().getAndroidJarPath(androidJarPath, apkPath));
        soot.options.Options.v().set_force_overwrite(true);
        Scene.v().loadNecessaryClasses();
        PrintWriter pw = new PrintWriter(new FileWriter(outputPath));
        for (SootClass cls : Scene.v().getApplicationClasses()){
            if(cls.getName().startsWith(packageName) || !isExcluded(cls.getName())) {
               for(SootMethod m : cls.getMethods()){
                   if(m.isConcrete()){
                       System.out.println(m.getSignature());
                       pw.write(m.getSignature()+"\n");
                   }
               }
            }
        }
        pw.close();
        System.out.println("write concrete methods to "+outputPath);
    }
    private static boolean isExcluded(String name){
        if (name.startsWith("java."))
            return true;
        if (name.startsWith("android.support."))
            return true;
        return false;
    }
}
