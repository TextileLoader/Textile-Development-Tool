package textile.devtool;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {
    static Runtime runtime = Runtime.getRuntime();

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 2) {
            switch (args[0]) {
                case "download": {
                    System.out.println("Starting download...");

                    File file = new File("FabricMC/"+args[1]);
                    if (!file.mkdirs()) {
                        System.out.println("Unable to create needed directories");
                        System.exit(1);
                    }

                    URL url = new URL("https://codeload.github.com/FabricMC/"+args[1]+"/zip/master");
                    ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                    FileOutputStream fileOutputStream = new FileOutputStream(file.getPath()+"/"+args[1]+".zip");
                    fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

                    readableByteChannel.close();
                    fileOutputStream.close();

                    System.out.println("Done!");
                    break;
                }
                case "makePatches": {
                    System.out.println("Making patch files...");

                    File file = new File("FabricMC-Patches/"+args[1]);
                    if (file.exists() && !file.delete()) {
                        System.out.println("Unable to delete \"FabricMC-Patches/"+args[1]+"\". Please delete it yourself and run the program again.");
                        System.exit(1);
                    }

                    if (!file.mkdirs()) {
                        System.out.println("Unable to create needed directories");
                        System.exit(1);
                    }

                    run("git remote add -f "+args[1]+" https://github.com/FabricMC/"+args[1]+".git");
                    run("git remote update");

                    File[] listedFiles = new File("FabricMC/"+args[1]).listFiles();
                    if (listedFiles == null) {
                        System.out.println("Repository is empty");
                        System.exit(1);
                    }

                    List<String> directoryPaths = new ArrayList<>();
                    while (true) {
                        for (File listedFile : listedFiles) {
                            if (listedFile.isDirectory()) {
                                directoryPaths.add(listedFile.getPath());
                            } else {
                                String patchPath = listedFile.getPath().replace(File.separator, "/").replaceFirst("FabricMC/fabric-loader/", "");
                                String patch = runWithOutput("git diff remotes/"+args[1]+"/master:"+patchPath+" FabricMC/"+args[1]+"/"+patchPath);
                                if (patch != null) {
                                    System.out.println("Creating patch for "+patchPath);
                                    File patchFile = new File("FabricMC-Patches"+File.separator+args[1]+File.separator+patchPath.replace("/", File.separator)+".patch");
                                    String patchFilePath = patchFile.getPath();
                                    File patchDirectories;
                                    if (
                                            patchPath.contains("/")
                                            && !(patchDirectories = new File(patchFilePath.substring(0, patchFilePath.lastIndexOf(File.separator)))).exists()
                                            && !patchDirectories.mkdirs()
                                    ) {
                                        System.out.println("Unable to create needed directories for patch file");
                                        System.exit(1);
                                    }
                                    if (!patchFile.createNewFile()) {
                                        System.out.println("Unable to create patch file");
                                        System.exit(1);
                                    }
                                    FileOutputStream fileOutputStream = new FileOutputStream(patchFile);
                                    fileOutputStream.write(patch.getBytes());
                                    fileOutputStream.close();
                                }
                            }
                        }
                        if (directoryPaths.isEmpty()) {
                            break;
                        } else {
                            listedFiles = new File(directoryPaths.get(0)).listFiles();
                            directoryPaths.remove(0);
                        }
                    }
                    run("git remote rm "+args[1]);

                    System.out.println("Done!");
                    break;
                }
                case "applyPatches": {
                    //TODO
                    break;
                }
            }
        }
    }

    public static void run(String command) throws IOException, InterruptedException {
        Process process = runtime.exec(command);
        process.waitFor(10, TimeUnit.SECONDS);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) System.out.println(line);
        reader.close();
    }

    public static String runWithOutput(String command) throws IOException, InterruptedException {
        Process process = runtime.exec(command);
        process.waitFor(10, TimeUnit.SECONDS);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) stringBuilder.append(line).append("\n");
        reader.close();
        String string = stringBuilder.toString();
        if (string.isEmpty()) return null;
        return string.substring(0, string.length()-1);
    }
}
