package org.bardsoftware.impl.eclipsito;

import org.bardsoftware.eclipsito.Boot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class BootImpl extends Boot {

    private static final ThreadGroup topThreadGroup = new ThreadGroup("TopThreadGroup") {
        public void uncaughtException(Thread t, Throwable e) {
            Boot.LOG.log(Level.WARNING, "[uncaughtException]" + e, e);
        }
    };

    private PlatformImpl myPlatform;

    public void run(String application, File modulesDir, String descriptorPattern, List<String> args) {
        myPlatform = new PlatformImpl();
        Boot.LOG.fine("Eclipsito platform is running.");
        ShutdownHook.install();

        File versionDir = getVersionDir(modulesDir);
        PluginDescriptor[] plugins = getPlugins(versionDir, descriptorPattern);
        run(plugins, application, args.toArray(new String[args.size()]));
        // start all bundles to let them initialize their services,
        // this should be done before an application is started
    }

    protected PluginDescriptor[] getPlugins(File pluginDirFile, String descriptorPattern) {
      assert pluginDirFile.exists() && pluginDirFile.isDirectory() : "Plugin directory doesn't exist or is not a directory: " + pluginDirFile;
      Boot.LOG.fine("Searching for plugins in " + pluginDirFile);
      PluginDescriptor[] plugins = ModulesDirectoryProcessor.process(pluginDirFile, descriptorPattern);
      return plugins;
    }

  private File getVersionDir(File modulesDir){
    File versionFile = new File(modulesDir.getPath() + File.separator + "VERSION");
    String version = null;
    try {
      BufferedReader br = new BufferedReader(new FileReader(versionFile));
      version = br.readLine();
      assert version == null : "Empty version file";
    } catch (IOException e) {
      Boot.LOG.severe("No version file found");
    }
    File versionDir = new File(modulesDir.getPath() + File.separator + version);
    assert versionDir.exists() : String.format("Directory %s doesn't exist", versionDir.getAbsolutePath());
    assert versionDir.isDirectory() && versionDir.canRead() : String.format("File %s is not a directory or is not readable", versionDir.getAbsolutePath());

    return versionDir;
  }

    public void run(PluginDescriptor[] plugins, final String application, final String[] args) {
      if (plugins.length == 0) {
        Boot.LOG.severe("No plugins found");
      }
      Boot.LOG.fine("Command line args: " + Arrays.asList(args));
        myPlatform.setup(plugins);
        new Thread(topThreadGroup, "Start") {
            public void run() {
                   myPlatform.start();
                   // after all bundles are started up, we can
                // launch an application which could use all supplied bundle services
                if (application != null && application.length() > 0) {
                    ApplicationLauncher.launchApplication(application, args);
                }
            }
        }.start();

    }
    public void shutdown() {
        myPlatform.stop();
        Boot.LOG.fine("Eclipsito platform is shut down.");
    }

    private static class ShutdownHook extends Thread {
        public ShutdownHook() {
            super(topThreadGroup, "ShutdownHook-Thread");
            setPriority(Thread.MAX_PRIORITY);
        }

        public void run() {
            Boot.getInstance().shutdown();
        }

        public static void install() {
            Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        }
    }

}
