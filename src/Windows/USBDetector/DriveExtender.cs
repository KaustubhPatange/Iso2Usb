using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Windows;

namespace kp.Iso2Linux
{
    public class DriveExtender
    {
        static string VolumeNumber=null, DRIVE_LETTER=null;
        /// <summary>
        /// Create a separate DriveExtender class from drive letter.
        /// </summary>
        /// <param name="driveletter">D,F,E,etc.</param>
        public DriveExtender(string driveletter)
        {
            DRIVE_LETTER = driveletter;
            var output = Regex.Split(DiskPart(new string[] { "list volume" }), "\r\n|\r|\n");
            foreach (string line in output)
            {
                if (line.Contains("Removable"))
                {
                    // This is a usb drive...
                    string formatted_string = Regex.Replace(line.Trim(), "[ ]{2,}", " ", RegexOptions.None);
                    var letter = formatted_string.Split(' ')[2];
                    if (letter.Contains(driveletter))
                        VolumeNumber = formatted_string.Split(' ')[1];
                }
                if (VolumeNumber != null) break;
            }
        }
       /// <summary>
       /// Format the USB drive with suitable parameter supplied
       /// </summary>
       /// <param name="Ftype">FAT32 or NTFS</param>
       /// <param name="Ptype">MBR or GPT</param>
       /// <param name="VolumeLabel">The name of the drive like 'FlashDrive'</param>
       /// <param name="clustersize">4096 (Default) or 8192</param>
       /// <param name="quickformat">If false, program will check for bad sector blocks.</param>
        public void Format(FileSystemType Ftype, PartitionType Ptype, string VolumeLabel, string clustersize = "4096", bool quickformat=true)
        {
            string ftype = "fat32", quick = " quick", ptype = "mbr";
            if (Ftype == FileSystemType.NTFS) ftype = "ntfs";
            if (Ptype == PartitionType.GPT) ptype = "gpt";
            if (!quickformat) quick = "";
            DiskPart(new string[] { $"select volume {VolumeNumber}","clean",$"convert {ptype}",
                "create partition primary",$"format fs={ftype} label=\"{VolumeLabel}\" unit=\"{clustersize}\"{quick}" });
        }
        /// <summary>
        /// Returns DriveInfo for particular drive letter...
        /// </summary>
        /// <returns></returns>
        public DriveInfo GetDriveInfo()
        {
            foreach(DriveInfo drive in DriveInfo.GetDrives())
            {
                if (drive.Name.Contains(DRIVE_LETTER))
                    return drive;
            }
            return null;
        }

        #region Other methods
        public enum PartitionType
        {
            MBR,
            GPT
        }
        public enum FileSystemType
        {
            FAT32,
            NTFS
        }
        /// <summary>
        /// Runs diskpart commands
        /// </summary>
        /// <param name="commands">new string[] { "list disk","exit",... }</param>
        /// <param name="autoexit"></param>
        /// <returns></returns>
        internal static string DiskPart(string[] commands, bool autoexit=true)
        {
            string text = null;
            Process p = new Process();
            p.StartInfo.FileName = "diskpart.exe";
            p.StartInfo.RedirectStandardError = true;
            p.StartInfo.RedirectStandardOutput = true;
            p.StartInfo.RedirectStandardInput = true;
            p.StartInfo.UseShellExecute = false;
            p.StartInfo.CreateNoWindow = true;
            p.EnableRaisingEvents = true;
            p.Start();
            foreach(string command in commands)
            {
                p.StandardInput.WriteLine(command);
            }
            if (autoexit)
            p.StandardInput.WriteLine("exit");
            do
            {
                try
                {
                    System.Windows.Forms.Application.DoEvents();
                }
                catch { }
                string output = p.StandardOutput.ReadToEnd();
                text += output;
                string err = p.StandardError.ReadToEnd();
                text += err;
            }
            while (!p.HasExited);
            return text;
        }
        #endregion
    }
}
