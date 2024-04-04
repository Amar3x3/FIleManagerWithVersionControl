





import java.io.*;
import java.util.*;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;


public class MangementSystem {
    public void ReadfilesList(String DirectoryPath){
        File directory = new File(DirectoryPath);
        if (directory.exists() && directory.isDirectory()){
            File [] files = directory.listFiles();
            if (files!=null && files.length > 0){
                int fileNo = 0;
                for (File file : files){
                    System.out.println(fileNo+") "+file.getName()
                    );
                    fileNo++;
                }
            }else {
                System.out.println("no files in system");
            }
        }else {
            System.out.println("no such directory exists");
        }
    }

    public static String readFile(String filePath) {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content.toString();
    }

    public boolean CreateAndSaveFileToLocal(String Directorypath,String FileName,String content){
        File directory = new File(Directorypath);
        if (!directory.exists()){
            System.out.println("directory does not exist check again");
            return false;
        }
        File file = new File(Directorypath,FileName);

        if (file.exists()){
            System.out.println("file name already exists try another");
            return false;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))){
                writer.write(content);
            System.out.println("file created at "+file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public boolean deleteFileFromLocal(String directoryPath, String fileName){
        File directory = new File(directoryPath);
        if (!directory.exists()){
            System.out.println("directory does not exist check path again");
            return false;
        }
        File file = new File(directoryPath,fileName);
        if (file.exists() && file.isFile()){
            if (file.delete()){
                System.out.println("file deleted successfully");
                return true;
            }else {
                System.out.println("failed to delete");
                return false;
            }
        }else {
            System.out.println("file not found");
            return false;
        }
    }

    public void listVersionsAndSwitchToFileVersion(String directoryPath, String fileName) throws GitAPIException, IOException {
        Map<String,String> previousVersions = previousVersions(directoryPath, fileName);

        System.out.println("Select a version to reuse:");
        for (int i = 0; i < previousVersions.size(); i++) {
            System.out.println((i + 1) + ". " + previousVersions.get(i));
            viewFileContentAtCommit(directoryPath,previousVersions.get(i),fileName);
        }

        Scanner scanner = new Scanner(System.in);
        int selectedIndex = scanner.nextInt();


        if (selectedIndex > 0 && selectedIndex <= previousVersions.size()) {
            String selectedCommitId = previousVersions.get(selectedIndex - 1);
            switchToFileVersion(directoryPath, selectedCommitId, fileName);
        } else {
            System.out.println("Invalid selection.");
        }
    }
    public Map<String,String> previousVersions(String dirPath, String fileName) throws IOException, GitAPIException {
        Map<String,String> previousVersions = new HashMap<>();
        try (Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(dirPath + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build()) {

            try (Git git = new Git(repository)) {
                Iterable<RevCommit> commitIds = git.log()
                        .add(repository.resolve("HEAD"))
                        .addPath(fileName)
                        .call();


                for (RevCommit commit : commitIds) {
                    previousVersions.put(commit.getName(),commit.getFullMessage());

                }
            }
        }
        return previousVersions;
    }
    public  String getCommitId(String dirPath, String commitMessage) throws IOException, GitAPIException {
        try (Repository repository = new FileRepository(dirPath + "/.git")) {
            try (Git git = new Git(repository)) {
                Iterable<RevCommit> commits = git.log().all().call();
                for (RevCommit commit : commits) {
                    if (commit.getFullMessage().equals(commitMessage)) {
                        System.out.println(commit.getName());
                        return commit.getName(); // Return the commit ID
                    }
                }
            }
        }
        return null; // Return null if commit ID is not found
    }
    public String viewFileContentAtCommit(String repoDirPath, String commitId, String filePath) throws IOException, GitAPIException {
        String Content = null;
        try (Repository repository = new FileRepository(repoDirPath + "/.git")) {
            try (Git git = new Git(repository)) {
                // Get the commit object
                RevWalk walk = new RevWalk(repository);
                RevCommit commit = walk.parseCommit(ObjectId.fromString(commitId));

                // Get the commit's tree and find the path of the file
                RevTree tree = commit.getTree();
                try (TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, tree)) {
                    if (treeWalk != null) {
                        // Use the object loader to read the file content
                        ObjectId objectId = treeWalk.getObjectId(0);
                        ObjectLoader loader = repository.open(objectId);
                        Content = loader.toString();
                    } else {
                        System.out.println("File not found in commit: " + commitId);
                    }
                }
            }
        }
        return Content;
    }

    // You
    private static void switchToFileVersion(String repoDirPath, String commitId, String filePath) throws IOException, GitAPIException {
        try (Repository repository = new FileRepository(repoDirPath + "/.git")) {
            try (Git git = new Git(repository)) {
                git.checkout().setName(commitId).call();
                System.out.println("Switched to version with commit ID: " + commitId + " for file: " + filePath);
            }
        }
    }

private static void commitToGitRepository(String dirPath, String fileName) {
    try {
        File gitDir = new File(dirPath + File.separator + ".git");
        if (!gitDir.exists()) {
            Git.init().setDirectory(new File(dirPath)).call();
        }

        try (Git git = Git.open(new File(dirPath))) {
            // Add file to the index
            git.add().addFilepattern(fileName).call();

            // Commit changes
            git.commit().setMessage("Updated file: " + fileName).call();
            System.out.println("Changes committed to Git repository.");
        }
    } catch (IOException | GitAPIException e) {
        throw new RuntimeException(e);
    }
}

    public void editIntoNotepadAndSave(String dirPath,String fileName) throws InterruptedException, IOException {
        String filePath = dirPath +"\\"+ fileName;
        System.out.println(filePath);
        String oldFileContent = readFile(filePath);
            Process process = Runtime.getRuntime().exec("notepad.exe "+filePath);
            process.waitFor();
        String newFileContent = readFile(filePath);

        if (!oldFileContent.equals(newFileContent)){
            commitToGitRepository(dirPath,fileName);
        }
        else {
            System.out.println("no changes");
        }


    }
}
