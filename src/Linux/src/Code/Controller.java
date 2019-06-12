package Code;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;

import Code.Windows.*;
import sun.rmi.runtime.Log;

import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Format USB code...
*
*  sudo umount /dev/sdb1
*  sudo mkfs.vfat -n "Volume label" -S 4096 /dev/sdb1
*  sudo mkfs.ntfs -n "Volume label" -S 4096 /dev/sdb1
*  sudo mkfs.ext4 -n "Volume label" -S 4096 /dev/sdb1
* */

public class Controller implements Initializable {

    @FXML
    private Button browseButton;
    @FXML
    private ComboBox usbDriveComBo;
    @FXML
    private ComboBox fileComBo;
    @FXML
    private ComboBox partitionCombo;
    @FXML
    private ComboBox fileSystemCombo;
    @FXML
    private ComboBox clusterCombo;
    @FXML
    private ComboBox targetCombo;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private TextArea logTextArea;
    @FXML
    private ImageView aboutButton;
    @FXML
    private ImageView webButton;
    @FXML
    private ImageView hashButton;
    @FXML
    private ImageView updateButton;
    @FXML
    private ImageView ddsave;
    @FXML
    private Button startCancelButton;
    @FXML
    private Label statusLabel;
    @FXML
    private Label proglabel;
    @FXML
    private Group mainpanel;
    @FXML
    private Group statusbarItems;
    @FXML
    private TextField volumeText;
    @FXML
    private CheckBox quickformat;
    @FXML
    private CheckBox createicon;

    // Change this with new release
    String CURRENT_VERSION="0150";

    DetectUsb usb; Thread usbtask; StartTimer timerthread; Timer maintimer; String username,password;
    ArrayList<String> usbDrivelist; ExtractISO mainiso;
    ArrayList<String> size, filenames;  int min = 0, sec = 0;
    ArrayList<String> location; String clustersize="",volumelabel="",filename="",syslinux="";
    double volumeSize; String temppath="/tmp/iso2usb",updatelink = "https://www.dropbox.com/s/6cup6huzd0y42rd/iso2usb.ini?dl=1";;
    Stage stage; boolean bool; String filesystemtype; boolean isStarted,isfullstarted;
    Process isoextract, ddprocess; FXMLLoader mainFXMLLoader;

    public void setStage(Stage stage, FXMLLoader loader) {
        this.stage = stage;
        this.mainFXMLLoader = loader;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        usbDrivelist = new ArrayList<>();
        filenames = new ArrayList<>();

        File f = new File(".iso2usb");
        Log(f.getAbsolutePath());

        // Making the log box read only
        logTextArea.setEditable(false);

        // Getting username of current user
        username = System.getProperty("user.name");

        // Set sudo password first
        sudoAskPassword();

        // Setting update settings
        if (!CommonClass.keyExistSetting("autoupdate")) {
            CommonClass.writeSetting("autoupdate","yes");
        }

        // Create temporary directory for work
        ExecuteShell("sudo rm -rf "+temppath);
        new File(temppath).mkdirs();

        // Setting events of components
        partitionCombo.getItems().addAll("MBR","GPT"); partitionCombo.getSelectionModel().select(0);
        partitionCombo.disableProperty().setValue(true);
       /*  Disabling partitionCombo since converting between MBR and GPT is not recommended and used for only
        *  windows iso. But new Win10 iso does not look usb as gpt and can work in both ways.
        *  Linux prefers mostly mbr part hence formatting will automatically change to mbr.*/
        targetCombo.getItems().add("BIOS or UFEI"); targetCombo.getSelectionModel().select(0);
        /* Disabling target combo as modern unix and linux prefers UFEI over BIOS */
        targetCombo.disableProperty().setValue(true);
        /* Disabling quick format checkBox since all format is done with mkfs i.e there is no such thing
        *  like quick format */
        quickformat.disableProperty().setValue(false);

        fileSystemCombo.getItems().addAll("FAT32/vfat (Default)","NTFS","EXT4");
        fileSystemCombo.getSelectionModel().select(0);
        clusterCombo.valueProperty().addListener((observable, oldValue, newValue) -> setClusterCombo());
        usbDriveComBo.valueProperty().addListener((observable, oldValue, newValue) -> setUsbDriveComBo());
        fileComBo.valueProperty().addListener((observable, oldValue, newValue) -> setFileComBo());
        browseButton.setOnAction(e->setBrowseButton());
        startCancelButton.setOnAction(e->setStartCancelButton());
        hashButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> setHashButton());
        ddsave.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> setDdsave());
        aboutButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> showAbout());
        updateButton.addEventHandler(MouseEvent.MOUSE_CLICKED,event -> setUpdateButton());
        webButton.addEventHandler(MouseEvent.MOUSE_CLICKED,event -> {
            // Load website into default browser
            try {
                new ProcessBuilder("x-www-browser", "https://github.com/KaustubhPatange/Iso2Usb").start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Disabling controls first
        hashButton.disableProperty().setValue(true);
        ddsave.disableProperty().setValue(true);
        Disable();

        // Checking for updates
        checkForUpdates();

    }

    // This event will show update dialog
    private void setUpdateButton() {
        Parent root = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Windows/updates.fxml"));
            root = fxmlLoader.load();
            Stage primaryStage = new Stage();
            primaryStage.setTitle("Updates");
            ((updates) fxmlLoader.getController()).setStage(primaryStage,mainFXMLLoader);
            Scene scene = new Scene(root, 362, 155);
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This event will occur when dd_save button is clicked
    private void setDdsave() {
        // Setting stuff
        Disable(true);
        String file = filenames.get(fileComBo.getSelectionModel().getSelectedIndex());
        String mount_point = usbDrivelist.get(usbDriveComBo.getSelectionModel().getSelectedIndex());

        // Show file chooser save  dialog
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("dd out", "*.dd");
        fileChooser.getExtensionFilters().add(filter);
        fileChooser.setTitle("Set output for dd file");
        fileChooser.setSelectedExtensionFilter(filter);
        fileChooser.setInitialFileName(volumeText.getText().replace(" ","_")+".dd");
        File savepath = fileChooser.showSaveDialog(stage);
        if (savepath != null) {
            startCancelButton.disableProperty().setValue(false);
            startCancelButton.setText("CANCEL");
            Log("Saving dd: "+savepath.getPath(),true);
            new Thread(new Task<Void>() {

                @Override
                protected Void call() throws Exception {
                    Log("dd if="+mount_point+" of=\""+savepath.getPath()+"\"");
                    ddprocess = ExecuteProcessNoReturn(
                            "dd if="+mount_point+" of=\""+savepath.getPath()+"\"");
                    Thread.sleep(2000);
                    do {
                        double calc = savepath.length()*100/(volumeSize*100);
                        updateProgress(calc,0);
                        Log("Progress: "+calc);
                        Thread.sleep(2000);
                    }while (ddprocess.isAlive());
                    return null;
                }

                @Override
                protected void updateProgress(double workDone, double max) {
                    progressBar.setProgress(workDone);
                    super.updateProgress(workDone, max);
                }

                @Override
                protected void succeeded() {
                    progressBar.setProgress(0);
                    Log("Done",true);
                    ExecuteShell("chmod 777 \""+savepath.getPath()+"\"");
                    ExecuteShell("chmod +x \""+savepath.getPath()+"\"");
                    startCancelButton.setText("START");
                    Enable();
                    super.succeeded();
                }
            }).start();
        }
    }

    // This event will occur when hash button is clicked
    private void setHashButton() {
        // Set indeterminate (marquee) progress
        progressBar.setProgress(-1);
        Disable(true);
        // Get the file name
        String file = filenames.get(fileComBo.getSelectionModel().getSelectedIndex());
        new Thread(new Task<Void> () {

            String md5,sha1;

            @Override
            protected Void call() throws Exception {
                // Calculate md5 and sha1 sum
                md5 = ExecuteShell("md5sum \""+file+"\"").split(" ")[0].trim();
                sha1 = ExecuteShell("sha1sum \""+file+"\"").split(" ")[0].trim();
                return null;
            }

            @Override
            protected void succeeded() {
                progressBar.setProgress(0);
                Enable();
                Parent root = null;
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Windows/md5class.fxml"));
                    root = fxmlLoader.load();
                    Stage primaryStage = new Stage();
                    primaryStage.setTitle("MD5 & SHA1");
                    ((md5class) fxmlLoader.getController()).setStage(primaryStage,md5,sha1);
                    Scene scene = new Scene(root, 480, 145);
                    primaryStage.setScene(scene);
                    primaryStage.setResizable(false);
                    primaryStage.showAndWait();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                super.succeeded();
            }
        }).start();
    }

    // This event will occur when Start or Cancel button is clicked
    private void setStartCancelButton() {
        if (startCancelButton.getText().contains("START")) {
            if (createAlert()) {
                // Setting something before
                Disable(true);
                isStarted = true;
                isfullstarted = true;
                maintimer.cancel();
                min = 0; sec = 0;
                timerthread = new StartTimer();
                new Thread(timerthread).start();
                progressBar.setProgress(-1);
                startCancelButton.setText("CANCEL");
                // Step 0: Getting drive letter and setting up...
                filename = filenames.get(fileComBo.getSelectionModel().getSelectedIndex());
                String loc = usbDrivelist.get(usbDriveComBo.getSelectionModel().getSelectedIndex());
                clustersize = ((String) clusterCombo.getValue()).split(" ")[0];
                filesystemtype="mkfs.vfat";
                switch (fileSystemCombo.getSelectionModel().getSelectedIndex()) {
                    case 1: filesystemtype="mkfs.ntfs"; break;
                    case 2: filesystemtype="mkfs.ext4"; break;
                }

                /*
                 * While labeling a volume, there are some limitations. If you are labeling a FAT volume, you can use 11 characters,
                 * while NTFS volumes can use up to 32 characters. Your labels cannot include tabs but you can use spaces.
                 * If you are labeling an NTFS drive, you can use all characters, however, FAT volumes cannot be labeled with the
                 * following characters { * ? / \ | . , ; : + = [ ] < > " }
                 */

                if (filesystemtype=="mkfs.vfat") {
                    String[] exclude = new String[] { "*", "?", "/", "\\", "|", ".", ",", ";", ":", "+", "=", "[", "]", "<", ">", "\"" };
                    volumelabel = volumeText.getText();
                    for(String str : exclude)
                    {
                        if (volumelabel.contains(str))
                            volumelabel = volumelabel.replace(str, "_");
                    }
                    if (volumelabel.length()>11) {
                        volumelabel = volumelabel.substring(0,11);
                    }
                }else {
                    if (volumelabel.length()>32) {
                        volumelabel = volumelabel.substring(0,32);
                    }
                }
                volumelabel = volumelabel.trim().toUpperCase();

                // Step 1: Formating the USB drive...
                Log("Formatting '"+loc+"'",true);
                new Thread( new Task<Void>() {

                    @Override
                    protected Void call() throws Exception {
                        Log("Format-1-Begin");
                        ExecuteShell("sudo umount "+loc);
                        // Removing previous mount point
                        Log("Format-2-RemoveMount");
                        String previous_mountpoint = location.get(usbDriveComBo.getSelectionModel().getSelectedIndex());
                        ExecuteShell("sudo umount "+previous_mountpoint);
                        ExecuteShell("sudo rmdir "+previous_mountpoint);
                        //  sudo mkfs.ext4 -n "Volume label" -S 4096 /dev/sdb1
                        ExecuteShell("sudo "+ filesystemtype +" -n \""+volumelabel+"\" -S "+ clustersize + " " + loc);
                        Log("Format-3-Done");
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        progressBar.setProgress(0);
                        startCancelButton.disableProperty().setValue(false);
                        // Saving index before
                        int index = usbDriveComBo.getSelectionModel().getSelectedIndex();
                        usbDrivelist.clear(); location.clear(); size.clear();
                        Log("Format-4-Succeed-"+index);
                        ExecuteShell("sudo umount /media/"+username+"/"+volumelabel);
                        ExecuteShell("sudo rmdir /media/"+username+"/"+volumelabel);
                        ExecuteShell("sudo mkdir /media/"+username+"/"+volumelabel);
                        ExecuteShell("sudo mount "+loc+" /media/"+username+"/"+volumelabel);
                        ExecuteShell("sudo chmod +x /media/"+username+"/"+volumelabel);

                        // Step 2: Copying proper syslinux...
                        switch(syslinux)
                        {
                            case "6.03":
                                ExportResource("ldlinux603.bin","/media/"+username+"/"
                                        +volumelabel+"/Idlinux.sys",false);
                                break;
                            case "6.04":
                                ExportResource("ldlinux604.bin","/media/"+username+"/"
                                        +volumelabel+"/Idlinux.sys",false);
                                break;
                        }
                        // Step 3: Extracting iso to location...
                        String location = "/media/"+username+"/"+volumelabel;
                        Log("Extracting '"+new File(filename).getName()+"'",true);
                        mainiso = new ExtractISO(filename,location);
                        new Thread(mainiso).start();
                        new Thread(new ExtractISOProgress(filename,location)).start();
                        // Next steps will be continued in ExtractISO method
                        super.succeeded();

                    }
                }).start();

            }
        } else {
            // Killing dd save process...
            if (ddprocess!=null) {
                ddprocess.destroy();
                ddprocess=null;
                startCancelButton.setText("START");
                progressBar.setProgress(0);
                Enable();
                return;
            }
            // Killing 7zip process...
            String output = ExecuteShell("ps aux | grep /7z");
            for(String line : output.split("\r|\n")) {
               String l = line.replace("└─","").replaceAll("[ ]{2,}"," ");
               ExecuteShell("sudo kill -9 "+l.split(" ")[1]);
            }
            isoextract.destroy();
            mainiso.cancel(true);
            // Step 4 will be automatically executed since ExtractISO's thread is killed
        }
    }

    // This event will occur when browse button is clicked
    private void setBrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an Iso or dd image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("iso or dd", "*.iso","*.dd")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file!=null) {
            // Getting information from file and adding it to fileCombo
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            filename = file.getPath();
            fileComBo.getItems().add(new File(filename).getName());
            filenames.add(filename);
            fileComBo.getSelectionModel().select(0);
            progressBar.setProgress(0);
        }
    }

    // This event will occur when main application is closed
    public void onClose() {
        ExecuteShell("rm -rf "+temppath);

        // Disabling background events
        usb.cancel(true);
        try {
            usbtask.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        maintimer.cancel();
        Platform.exit();
        System.exit(0);
    }

    // This event will occur when value in fileCombo will change
    private void setFileComBo() {
        // Getting file details
        LoadFile(filenames.get(fileComBo.getSelectionModel().getSelectedIndex()));
    }

    // This event will occur when value in UsbCombo box will change
    private void setUsbDriveComBo() {
        try {
           if (usbDriveComBo.getItems().size()>0) {
               int index = usbDriveComBo.getSelectionModel().getSelectedIndex();
               Log(usbDrivelist.get(index));
               String output = ExecuteShell("blockdev --getsize64 " + usbDrivelist.get(index));
               volumeSize = Double.parseDouble(output);
               DetectClusterSize();
           }
        }catch (Exception e) {Log(e.getMessage());}
    }

    // This event will occur when value in clustor combo box will change
    private void setClusterCombo() {
       try {
           clustersize = (String) clusterCombo.getValue();
           clustersize = clustersize.split(" ")[0];
       }catch (Exception e){Log(e.getMessage());}
    }

    //This method is used to timer calculation
    private class StartTimer extends Task<Void> {

        @Override
        protected Void call() throws Exception {
           do {
               sec++;
               if (sec>=60) { min++; sec = 0; }
               Thread.sleep(1000);
           }while (isfullstarted);
            return null;
        }

    }
    // This is step 4 Finalizing moments and stopping
    private void Step4(String to) {
        isStarted=false;
        Log("Removing temp files & dirs",true);
        ExecuteShell("rm -rf "+to+"/[BOOT]");
        if (createicon.isSelected()) {
            Log("Creating extended labels",true);
            ExportResource("autorun.bin",to+"/autorun.ico",false);
            CommonClass.write("/tmp/autorun.txt",
                    ";Created using Iso2Usb - https://kaustubhpatange.github.io\n" +
                            "[autorun]\n" +
                            "icon = autorun.ico\n" +
                            "label = "+volumelabel);
            ExecuteShell("mv /tmp/autorun.txt "+to+"/autorun.inf");
        }
        isfullstarted = false;
        timerthread.cancel();
        Log("Done",true);
        Log("--- Ran for "+min+" min "+sec+" sec ---",true);
        proglabel.setText("");
        statusLabel.setText("Ready...");
        startCancelButton.setText("START");
        Enable();
        createAlert("Information",
                "Information"
                ,"Bootable media has been created!",Alert.AlertType.INFORMATION,ButtonType.OK);
        progressBar.setProgress(0);
        runUsbTask(false);
    }

    // This method is used to extract iso from file
    private class ExtractISO extends Task<Void> {

        String from,to;

        public ExtractISO(String filename, String location) {
            this.from = filename;
            this.to = location;
            ExportResource("7z","",true);
            ExportResource("7z.so","",true);
            ExecuteShell("sudo chmod +x /tmp/iso2usb/7z");
            ExecuteShell("sudo chmod 777 '"+location+"'");
            ExecuteShell("sudo chmod 777 '"+filename+"'");
            Log("/tmp/iso2usb/7z x -y \""+filename+"\" -o\""+location+"\"");
        }

        @Override
        protected Void call() throws Exception {
            ExecuteShellISO("/tmp/iso2usb/7z x -y '"+from+"' -o'"+to+"'");
            return null;
        }

        @Override
        protected void succeeded() {
            Step4(to);
            super.succeeded();
        }
    }

    // This method is to calculate progress from ExtractISO method
    // literally i can't find any other method than calculating progress
    private class ExtractISOProgress extends Task<Void> {

        String from,to;

        public ExtractISOProgress(String filename, String location) {
            this.from = filename;
            this.to = location;
            progressBar.setProgress(0);
        }

        @Override
        protected Void call() throws Exception {
            long filelength = new File(filename).length();
            do {
                long calc = folderSize(new File(to))*100/filelength;
                double progress = calc/100.00;
                Thread.sleep(1000);
                Log("Progress: " + progress);
                updateProgress(progress,100);
            }while (isStarted);
            return null;
        }

        @Override
        protected void updateProgress(double workDone, double max) {
            progressBar.setProgress(workDone);
            super.updateProgress(workDone, max);
        }
    }

    // This background task function will be used to detect USB devices...
    private class DetectUsb extends Task<Void> {
        ArrayList<String> alldrivelist;

        boolean torunonce; int index;

        public DetectUsb(boolean runonce) {
            torunonce = runonce;
            index = usbDriveComBo.getSelectionModel().getSelectedIndex();
        }

        public DetectUsb(boolean runonce,int index) {
            torunonce = runonce;
            this.index = index;
        }

        @Override
        protected Void call() {
            // Main logic of detecting
            alldrivelist = new ArrayList<>();
            size = new ArrayList<>();
            location = new ArrayList<>();

            // Get output of list blocks
            String output = ExecuteShell("lsblk | grep 'sdb'");
            if (output.isEmpty()) output = ExecuteShell("lsblk | grep 'sdc'");
            if (output!=null && !output.isEmpty()) {
                for(String line: output.split("\r|\n")) {
                    // Remove the first treeView
                    if (!line.contains("sdb ") && (!line.contains("sdc "))) {
                        line = line.replace("└─","").replaceAll("[ ]{2,}"," ");
                        if (line.split(" ").length>=7) {
                            // Adding them in respective list
                            alldrivelist.add("/dev/"+line.split(" ")[0]);
                            size.add(line.split(" ")[3]);
                            location.add(line.split(" ")[6]);
                        }else {
                            Log("USB: /dev/"+line.split(" ")[0]+" is not mounted");
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void succeeded() {
            super.succeeded();


            // Update the original list if change is made
            if (alldrivelist.size()!=usbDrivelist.size()) {
                usbDrivelist = alldrivelist;
                updateUsbCombo(index);
            }
            // Creating new timer task to async wait for 1 seconds before launching
            // same task again...
           if (!torunonce) {
               maintimer = new Timer();
               maintimer.schedule(new TimerTask() {
                   @Override
                   public void run() {
                       runUsbTask(false);
                   }
               },1000);
           }
        }
    }

    // This method will update UsbCombo with new devices attached or removed
    private void updateUsbCombo(int index) {
        usbDriveComBo.getItems().clear();
        for(int i=0;i<usbDrivelist.size();i++) {
            String driveLabel = usbDrivelist.get(i);
            String driveSize = size.get(i);
            String driveLocation = location.get(i);
            usbDriveComBo.getItems().add(driveLocation.substring(driveLocation.lastIndexOf("/")+1)+" "+
                    "("+driveLabel+") ["+driveSize+"]");
            Log("New block inserted: "+driveLabel,true);
        }
        if (usbDriveComBo.getItems().size()>index) {
            usbDriveComBo.getSelectionModel().select(index);
        }
        if (usbDriveComBo.getItems().size()>0 && fileComBo.getItems().size()>0) {
            startCancelButton.disableProperty().setValue(false);
        }
        if (index==-1) {
            usbDriveComBo.getSelectionModel().select(0);
        }
    }

    // This method is used to execute root shell commands
    private String ExecuteShell(String command) {
        String[] cmd = {"/bin/bash","-c","echo '"+ password + "' | sudo -S "+command};
        StringBuilder joined= new StringBuilder();
        Process pb = null;
        try {
            pb = Runtime.getRuntime().exec(cmd);
            String line;
            BufferedReader input = new BufferedReader(new InputStreamReader(pb.getInputStream()));
            while ((line = input.readLine()) != null) {
                joined.append(line).append("\n");
            }
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return joined.toString();
    }
    private String ExecuteShellISO(String command) {
        String[] cmd = {"/bin/bash","-c","echo '"+ password + "' | sudo -S "+command};
        StringBuilder joined= new StringBuilder();
        try {
            isoextract = Runtime.getRuntime().exec(cmd);
            String line;
            BufferedReader input = new BufferedReader(new InputStreamReader(isoextract.getInputStream()));
            while ((line = input.readLine()) != null) {
                joined.append(line).append("\n");
            }
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return joined.toString();
    }
    private Process ExecuteProcessNoReturn(String command) {
        String[] cmd = {"/bin/bash","-c","echo '"+ password + "' | sudo -S "+command};
        Process pb = null;
        try {
            pb = Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pb;
    }

    // This method will show about info
    private void showAbout() {
        Parent root;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Windows/about.fxml"));
            root = fxmlLoader.load();
            Stage primaryStage = new Stage();
            primaryStage.setTitle("About");
            Scene scene = new Scene(root, 363, 234);
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method will be used to set sudo password at beginning..
    private void sudoAskPassword() {
        String pass = CommonClass.readSetting("pass");
        if (!pass.isEmpty()) {
            password = pass;
            return;
        }
        Parent root;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Windows/askSudo.fxml"));
            root = fxmlLoader.load();
            Stage primaryStage = new Stage();
            primaryStage.setTitle("Iso2Usb - Ask Password");
            ((askSudo) fxmlLoader.getController()).setStage(primaryStage);
            Scene scene = new Scene(root, 355, 192);
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.showAndWait();
            TextField field = (TextField) scene.lookup("#pswd");
            password = field.getText();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method is used to disable controls
    private void Disable(boolean all) {
        mainpanel.disableProperty().setValue(true);
        startCancelButton.disableProperty().setValue(true);
        usbDriveComBo.disableProperty().setValue(true);
        fileComBo.disableProperty().setValue(true);
        browseButton.disableProperty().setValue(true);
        statusbarItems.disableProperty().setValue(true);
    }
    private void Disable() {
        mainpanel.disableProperty().setValue(true);
    }

    private void Enable() {
        mainpanel.disableProperty().setValue(false);
        startCancelButton.disableProperty().setValue(false);
        usbDriveComBo.disableProperty().setValue(false);
        fileComBo.disableProperty().setValue(false);
        browseButton.disableProperty().setValue(false);
        statusbarItems.disableProperty().setValue(false);
    }

    // This is used to log text to main terminal...
    private void Log(String text) {
        System.out.println(text +"\n");
    }
    private void Log(String text,boolean showatstatus) {
        logTextArea.appendText(text+"\n");
        if (showatstatus) statusLabel.setText(text);
    }

    private void LoadFile(String filename) {
       Log(filename);
       ExecuteShell("7z x -y \""+filename+"\" -o"+temppath+" isolinux");
       File file = new File(filename);
       File f = new File("/tmp/iso2usb/isolinux");
       String normalname = file.getName().replace(".iso","")
               .replace(".dd","");
       if (f.exists() && f.isDirectory()) {
           // This is a linux iso...
           Log("Detected '"+file.getName()+"' as linux iso",true);
           ExecuteShell("7z x -y \""+filename+"\" -o/tmp/iso2usb *.diskdefines");
           ExecuteShell("chmod +x "+"/tmp/iso2usb/README.diskdefines");
           if (CommonClass.filexist("/tmp/iso2usb/README.diskdefines")) {
               // This will set proper text in Volume Label TextBox
               String firstline = CommonClass.read("/tmp/iso2usb/README.diskdefines").split("\r|\n")[0];
               volumeText.setText(RegexMatch(".+?(?=\")",firstline,0)
                       .replace("#define DISKNAME", "").trim() + " " +
                       firstline.split(" ")[firstline.split(" ").length - 1]);
               new File("/tmp/iso2usb/README.diskdefines").delete();
           }else {
               volumeText.setText(normalname);
           }
           syslinux = DetectSyslinuxVersion("/tmp/iso2usb/isolinux");
           logTextArea.appendText("-- syslinux: "+syslinux+"\n");
          // ExecuteShell("rm -rf "+"/tmp/iso2usb/isolinux");
       } else {
           /* This is a unix based system could be windows, I am assuming it as windows...
            * We will use Standard installation for windows instead Windows To Go like RUFUS...
            */
           Log("Detected '"+file.getName()+"' file as unix iso");
           volumeText.setText(normalname);
       }
        if (usbDriveComBo.getItems().size() <= 0)
        {
            startCancelButton.disableProperty().setValue(true);
        }
        else startCancelButton.disableProperty().setValue(false);
        mainpanel.disableProperty().setValue(false);
        hashButton.disableProperty().setValue(false);
        ddsave.disableProperty().setValue(false);
        // Changing images on status bar buttons
        Image image1 = new Image(getClass().getResourceAsStream("images/checksum.png"),22,22,true,false);
        Image image2 = new Image(getClass().getResourceAsStream("images/dd_save.png"),22,22,true,false);
        hashButton.imageProperty().set(image1);
        ddsave.imageProperty().set(image2);
    }

    // This method will be used to detect syslinux (as it is ported from C# code)
    private String DetectSyslinuxVersion(String location)
    {
        /*A very bad regex here, but seriously I can't think of something else.
         * Seriously a freaking dirty way to detect syslinux version... Mind blowing...*/
        Pattern p = Pattern.compile("ISOLINUX \\d\\.\\d\\d");
        Matcher m = p.matcher(ExecuteShell("sudo cat "+location + "/isolinux.bin"));
        while (m.find()) {
            return m.group(0).replace("ISOLINUX","").trim();
        }
        return null;
    }

    // This method will help to detect cluster size depends volume size and file system type of USB drive
    public void DetectClusterSize() {
        // It is necessary to format USB with 4096 cluster when on linux since mkfs shows it as error

        // There will be no options for file system FAT16 since it does not support devices more than 2GB
        // In NTFS maximum cluster size is 4 KB which is already included in FAT32...
        clusterCombo.getItems().clear();
        // Calculate Cluster size for FAT32 partition...
        if (volumeSize > 2147483648.0) // For over 2GB drive
        {
            clusterCombo.getItems().add("4096 bytes (Default)");
            if (volumeSize < 8589934592.0) // Make this default...
            {
                makeDefault();
            }
        }
        if (volumeSize > 8589934592.0) // For over 8GB drive
        {
            clusterCombo.getItems().add("8192 bytes");
            if (volumeSize < 17179869184.0) // Make this default...
            {
                makeDefault();
            }
        }
        if (volumeSize > 17179869184.0) // For over 16GB drive
        {
            clusterCombo.getItems().add("16384 bytes");
            if (volumeSize < 34359738368.0) // Make this default...
            {
                makeDefault();
            }
        }
        if (volumeSize >= 34359738368.0)  // For over 32GB drive
        {
            clusterCombo.getItems().add("32768 bytes");
            makeDefault();
        }
    }
    void makeDefault()
    {
        /* A way to add (Default) text to new item */
       /* int index = clusterCombo.getItems().size()-1;
        clusterCombo.getSelectionModel().select(index);
        String size = (String) clusterCombo.getValue();
        clusterCombo.getItems().remove(index);
        clusterCombo.getItems().add(size + " (Default)");
        clusterCombo.getSelectionModel().select(index);*/
        clusterCombo.getSelectionModel().select(0);
    }
    // A standard alert that will be shown before formatting USB
    private boolean createAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(ButtonType.YES,ButtonType.NO);
        alert.setTitle("Warning");
        alert.setHeaderText("Warning");
        alert.setContentText("All data stored in USB drive will be erased during this process. Make sure to take backup if neccessary.\n\nAre you sure to continue?");
        alert.showAndWait().ifPresent(rs -> {
            if (rs == ButtonType.YES) {
                bool = true;
            }
            if (rs == ButtonType.NO) {
                bool = false;
            }
        });
        return bool;
    }
    // A custom alert that needs title and header text as parameters
    private boolean createAlert(String title, String headerText, String message, Alert.AlertType type,ButtonType... buttonTypes) {
        Alert alert = new Alert(type);
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(buttonTypes);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(message);
        alert.showAndWait().ifPresent(rs -> {
            if (rs == ButtonType.YES) {
                bool = true;
            }
            if (rs == ButtonType.NO) {
                bool = false;
            }
        });
        return bool;
    }
    // This method will create new usb task
    private void runUsbTask(boolean torunonce) {
        if (usb!=null) {
            usb.cancel();
        }
        usb = new DetectUsb(torunonce);
        usbtask = new Thread(usb);
        usbtask.setDaemon(true);
        usbtask.start();
        if (torunonce) {
            try {
                usbtask.join();
            } catch (Exception ignored) {

            }
        }
    }

    // This method will be used to check updates
    private void checkForUpdates() {
        if (CommonClass.readSetting("autoupdate").contains("yes") && checkForConnection()) {
            logTextArea.appendText("Checking for updates\n");
            logTextArea.appendText("[*] This can be disable from update button!\n");
            Disable(true);
            CheckUpdates();
        }
    }
    public void CheckUpdates() {
        progressBar.setProgress(-1);
        new Thread(new Task<Void>() {
            String version = "0";
            boolean showupdate=false;

            @Override
            protected Void call() throws Exception {
                // Reading stream from url
                URL url = new URL(updatelink);
                BufferedReader br;
                try {
                    br = new BufferedReader(new InputStreamReader(url.openStream()));
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.equalsIgnoreCase("quit")) {
                            break;
                        }
                        Pattern p = Pattern.compile("version(.*?)\\=");
                        Matcher m = p.matcher(line);
                        while (m.find()) {
                            version = line.split("=")[1].trim().replace(".","");
                        }
                    }
                }
                catch (IOException ioe) {
                    System.out.println("Exception while reading input " + ioe);
                }
                if (Integer.parseInt(CURRENT_VERSION) < Integer.parseInt(version)) {
                    showupdate=true;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                progressBar.setProgress(0);
                Enable();
                startCancelButton.disableProperty().setValue(true);
                if (showupdate) {
                    if (createAlert("Download","Update","A new update is available for Iso2Usb",
                            Alert.AlertType.INFORMATION,ButtonType.YES,ButtonType.NO)) {
                        try {
                            new ProcessBuilder("x-www-browser", "https://kaustubhpatange.github.io/Iso2Usb").start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }else logTextArea.appendText("[*] No update found!\n");

                // Running usbTask
                runUsbTask(false);

                super.succeeded();
            }
        }).start();
    }

    // This method will be used to check if internet connection is available
    private boolean checkForConnection() {
        try {
            URL url = new URL("http://www.google.com");
            URLConnection conn = url.openConnection();
            conn.connect();
            conn.getInputStream().close();
            return true;
        }catch (Exception ignore) {
            return false;
        }
    }

    // A piece of code from stackoverflow extracting file from resource to a folder
     public void ExportResource(String resourceName,String destination,boolean to_tmp) {
         try {
             InputStream stream = getClass().getResourceAsStream(resourceName);
             Files.copy(stream,new File("/tmp/iso2usb/"+resourceName).toPath(),
                     StandardCopyOption.REPLACE_EXISTING);
             if (!to_tmp) ExecuteShell("mv "+"/tmp/iso2usb/"+resourceName+" "+destination+"");
         } catch (Exception e) {
             e.printStackTrace();
         }
    }

    // I created this method coz direct stuff like String str = new Pattern.compile().matcher().group(0)
    // didn't work for me
    public String RegexMatch(String pattern,String text,int group) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher("#define DISKNAME  elementary OS 5.0  \"juno\" - Release amd64");
        while (m.find()) {
           return m.group(group);
        }
        return null;
    }

    // A function to calculate folder size...
    public long folderSize(File directory) {
        long length = 0;
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isFile())
                length += file.length();
            else
                length += folderSize(file);
        }
        return length;
    }
}
