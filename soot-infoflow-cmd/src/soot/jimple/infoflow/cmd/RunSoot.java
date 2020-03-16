package soot.jimple.infoflow.cmd;

import java.io.File;
import java.util.Collections;

import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.options.Options;

public class RunSoot {

	public static void main(String... args) {
		String taintBench = "E:\\Git\\Github\\taintbench\\taint-benchmark\\apps\\android";
		String androidJarPath = "E:\\Git\\androidPlatforms";
		for (File benchmark : new File(taintBench).listFiles()) {
			if (benchmark.isDirectory()) {
				System.out.println(benchmark);
				String pathToBenchmark = benchmark.toString();
				String benchmarkName = benchmark.getName();
				String apk = pathToBenchmark + File.separator + benchmarkName + ".apk";
				String output = pathToBenchmark + File.separator + "jimple";
				new File(output).mkdir();
				G.reset();
				Options.v().set_no_bodies_for_excluded(true);
				Options.v().set_allow_phantom_refs(true);
				Options.v().set_process_dir(Collections.singletonList(apk));
				Options.v().set_android_jars(androidJarPath);
				Options.v().set_src_prec(Options.src_prec_apk);
				Options.v().set_keep_line_number(true);
				Options.v().set_process_multiple_dex(true);
				Options.v().setPhaseOption("jb", "use-original-names:true");
				Options.v().set_soot_classpath(Scene.v().getAndroidJarPath(androidJarPath, apk));
				Options.v().set_force_overwrite(true);
				Options.v().set_output_dir(output);
				Options.v().set_output_format(Options.output_format_jimple);
				Scene.v().loadNecessaryClasses();
				PackManager.v().runBodyPacks();
				PackManager.v().writeOutput();
			}
		}
	}
}