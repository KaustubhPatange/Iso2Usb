package Code;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CommonClass {
    public static void write(String filename,String contents) {
        try{
            FileWriter fw=new FileWriter(filename);
            fw.write(contents);
            fw.close();
        }catch(Exception e){System.out.println(e);}
    }
    public static void append(String filename,String contents) {
        File f = new File(filename);
        if (!f.exists())
            write(filename,"");
        try{
            FileWriter fw=new FileWriter(filename);
            fw.append(contents);
            fw.close();
        }catch(Exception e){System.out.println(e);}
    }
    public static void copy(File file,File dest) {
        try {
            Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
    public static String read(String filename) {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(filename));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            String everything = sb.toString();
            br.close();
            return everything;
        } catch (Exception ex) {System.out.println(ex);}
        return "";
    }
    public static boolean filexist(String filename) {
        File f = new File(filename);
        return f.exists();
    }
    public static String readSetting(String key) {
        String readtoEnd = read(".iso2usb");
        if (readtoEnd.contains(key)) {
            for(String line : readtoEnd.split("\r|\n")) {
                if (line.contains(key)) {
                    return line.split("=")[1].trim();
                }
            }
        }
        return "";
    }
    public static void writeSetting(String key,String value) {
        File f = new File(".iso2usb");
        if (!f.exists())
            write(".iso2usb","");
        String readtoEnd = read(".iso2usb");
        if (readtoEnd.contains(key)) {
            String tojoin="";
            for(String line : readtoEnd.split("\r|\n")) {
                if (line.contains(key)) {
                    tojoin+="--"+key+"="+value+"\n";
                }else tojoin+=line+"\n";
            }
            write(".iso2usb",tojoin);
        } else {
            append(".iso2usb","--"+key+"="+value);
        }
    }
}
